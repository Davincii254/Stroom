/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package stroom.query.common.v2;

import stroom.dashboard.expression.v1.DateUtil;
import stroom.query.api.v2.ExpressionItem;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionTerm;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.query.api.v2.Field;
import stroom.query.api.v2.Format;
import stroom.query.api.v2.Format.Type;
import stroom.util.shared.CompareUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class ConditionalFormattingExpressionMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionalFormattingExpressionMatcher.class);
    private static final String DELIMITER = ",";

    private final Map<String, Pattern> patternCache = new HashMap<>();

    private final Map<String, Field> fieldNameToFieldMap;

    public ConditionalFormattingExpressionMatcher(final List<Field> fields) {
        this.fieldNameToFieldMap = new HashMap<>();
        for (final Field field : fields) {
            fieldNameToFieldMap.putIfAbsent(field.getName(), field);
        }
    }

    public boolean match(final Map<String, Object> attributeMap, final ExpressionItem item) {
        // If the initial item is null or not enabled then don't match.
        if (item == null || !item.enabled()) {
            return false;
        }
        return matchItem(attributeMap, item);
    }

    private boolean matchItem(final Map<String, Object> attributeMap, final ExpressionItem item) {
        if (!item.enabled()) {
            // If the child item is not enabled then return and keep trying to match with other parts
            // of the expression.
            return true;
        }

        if (item instanceof ExpressionOperator) {
            return matchOperator(attributeMap, (ExpressionOperator) item);
        } else if (item instanceof ExpressionTerm) {
            return matchTerm(attributeMap, (ExpressionTerm) item);
        } else {
            throw new MatchException("Unexpected item type");
        }
    }

    private boolean matchOperator(final Map<String, Object> attributeMap,
                                  final ExpressionOperator operator) {
        if (operator.getChildren() == null || operator.getChildren().size() == 0) {
            return true;
        }

        switch (operator.op()) {
            case AND:
                for (final ExpressionItem child : operator.getChildren()) {
                    if (!matchItem(attributeMap, child)) {
                        return false;
                    }
                }
                return true;
            case OR:
                for (final ExpressionItem child : operator.getChildren()) {
                    if (matchItem(attributeMap, child)) {
                        return true;
                    }
                }
                return false;
            case NOT:
                return operator.getChildren().size() == 1
                        && !matchItem(attributeMap, operator.getChildren().get(0));
            default:
                throw new MatchException("Unexpected operator type");
        }
    }

    private boolean matchTerm(final Map<String, Object> attributeMap, final ExpressionTerm term) {
        // The term field is the column name, NOT the index field name
        String termField = term.getField();
        final Condition condition = term.getCondition();
        String termValue = term.getValue();

        // Clean strings to remove unwanted whitespace that the user may have
        // added accidentally.
        if (termField != null) {
            termField = termField.trim();
        }
        if (termValue != null) {
            termValue = termValue.trim();
        }

        // Try and find the referenced field.
        if (termField == null || termField.length() == 0) {
            throw new MatchException("Field not set");
        }
        final Field field = fieldNameToFieldMap.get(termField);
        if (field == null) {
            throw new MatchException("Field not found in index: " + termField);
        }
        final String fieldName = field.getName();
        if (termValue == null || termValue.length() == 0) {
            throw new MatchException("Value not set");
        }

        final Object attribute = attributeMap.get(term.getField());
        if (attribute == null) {
            throw new MatchException("Attribute '" + term.getField() + "' not found");
        }

        // Create a query based on the field type and condition.
        if (matchesFormatType(field, Format.Type.NUMBER)) {
            return matchNumericField(condition, termValue, field, fieldName, attribute);
        } else if (matchesFormatType(field, Format.Type.DATE_TIME)) {
            return matchDateField(condition, termValue, field, fieldName, attribute);
        } else {
            switch (condition) {
                case EQUALS:
                    return isStringMatch(termValue, attribute);
                // CONTAINS only supported for legacy content, not for use in UI
                case CONTAINS:
                    return isStringContainsMatch(termValue, attribute);
                case IN:
                    return isIn(termValue, attribute);
                default:
                    // Try to treat as a numeric field.
                    return matchNumericField(condition, termValue, field, fieldName, attribute);
            }
        }
    }

    private boolean matchesFormatType(final Field field, final Type type) {
        return Optional.ofNullable(field)
                .map(Field::getFormat)
                .map(Format::getType)
                .filter(fieldType -> fieldType.equals(type))
                .isPresent();
    }

    private boolean matchDateField(final Condition condition,
                                   final String termValue,
                                   final Field field,
                                   final String fieldName,
                                   final Object attribute) {
        switch (condition) {
            case EQUALS: {
                final Long num1 = getDate(fieldName, attribute);
                final Long num2 = getDate(termValue);
                return Objects.equals(num1, num2);
            }
            case GREATER_THAN: {
                final Long num1 = getDate(fieldName, attribute);
                final Long num2 = getDate(termValue);
                return CompareUtil.compareLong(num1, num2) > 0;
            }
            case GREATER_THAN_OR_EQUAL_TO: {
                final Long num1 = getDate(fieldName, attribute);
                final Long num2 = getDate(termValue);
                return CompareUtil.compareLong(num1, num2) >= 0;
            }
            case LESS_THAN: {
                final Long num1 = getDate(fieldName, attribute);
                final Long num2 = getDate(termValue);
                return CompareUtil.compareLong(num1, num2) < 0;
            }
            case LESS_THAN_OR_EQUAL_TO: {
                final Long num1 = getDate(fieldName, attribute);
                final Long num2 = getDate(termValue);
                return CompareUtil.compareLong(num1, num2) <= 0;
            }
            case BETWEEN: {
                final Long[] between = getDates(termValue);
                if (between.length != 2) {
                    throw new MatchException("2 numbers needed for between query");
                }
                if (CompareUtil.compareLong(between[0], between[1]) >= 0) {
                    throw new MatchException("From number must be lower than to number");
                }
                final Long num = getDate(fieldName, attribute);
                return CompareUtil.compareLong(num, between[0]) >= 0
                        && CompareUtil.compareLong(num, between[1]) <= 0;
            }
            default:
                throw new MatchException("Unexpected condition '" + condition.getDisplayValue() + "' for "
                        + field.getFormat().getType().getDisplayValue() + " field type");
        }
    }

    private boolean matchNumericField(final Condition condition,
                                      final String termValue,
                                      final Field field,
                                      final String fieldName,
                                      final Object attribute) {
        switch (condition) {
            case EQUALS: {
                final BigDecimal num1 = getNumber(fieldName, attribute);
                final BigDecimal num2 = getNumber(fieldName, termValue);
                return Objects.equals(num1, num2);
            }
            case GREATER_THAN: {
                final BigDecimal num1 = getNumber(fieldName, attribute);
                final BigDecimal num2 = getNumber(fieldName, termValue);
                int compVal = CompareUtil.compareBigDecimal(num1, num2);

                LOGGER.debug(num1 + " " + num2 + " " + compVal);

                return compVal > 0;
            }
            case GREATER_THAN_OR_EQUAL_TO: {
                final BigDecimal num1 = getNumber(fieldName, attribute);
                final BigDecimal num2 = getNumber(fieldName, termValue);
                return CompareUtil.compareBigDecimal(num1, num2) >= 0;
            }
            case LESS_THAN: {
                final BigDecimal num1 = getNumber(fieldName, attribute);
                final BigDecimal num2 = getNumber(fieldName, termValue);
                return CompareUtil.compareBigDecimal(num1, num2) < 0;
            }
            case LESS_THAN_OR_EQUAL_TO: {
                final BigDecimal num1 = getNumber(fieldName, attribute);
                final BigDecimal num2 = getNumber(fieldName, termValue);
                return CompareUtil.compareBigDecimal(num1, num2) <= 0;
            }
            case BETWEEN: {
                final BigDecimal[] between = getNumbers(fieldName, termValue);
                if (between.length != 2) {
                    throw new MatchException("2 numbers needed for between query");
                }
                if (CompareUtil.compareBigDecimal(between[0], between[1]) >= 0) {
                    throw new MatchException("From number must be lower than to number");
                }
                final BigDecimal num = getNumber(fieldName, attribute);
                return CompareUtil.compareBigDecimal(num, between[0]) >= 0
                        && CompareUtil.compareBigDecimal(num, between[1]) <= 0;
            }
            case IN:
                return isNumericIn(fieldName, termValue, attribute);
            default:
                throw new MatchException("Unexpected condition '" + condition.getDisplayValue() + "' for "
                        + field.getFormat().getType().getDisplayValue() + " field type");
        }
    }

    private boolean isNumericIn(final String fieldName, final Object termValue, final Object attribute) {
        final BigDecimal num = getNumber(fieldName, attribute);
        final BigDecimal[] in = getNumbers(fieldName, termValue);
        for (final BigDecimal n : in) {
            if (Objects.equals(n, num)) {
                return true;
            }
        }

        return false;
    }

    private boolean isIn(final Object termValue, final Object attribute) {
        final String[] termValues = termValue.toString().split(" ");
        for (final String tv : termValues) {
            if (isStringMatch(tv, attribute)) {
                return true;
            }
        }
        return false;
    }

    private boolean isStringMatch(final String termValue, final Object attribute) {
        if (termValue.contains("*")) {
            final Pattern pattern = patternCache.computeIfAbsent(termValue, k -> {
                final String regex = k.replaceAll("\\*", ".*");
                return Pattern.compile(regex);
            });
            return pattern.matcher(attribute.toString()).matches();
        }
        return termValue.equals(attribute.toString());
    }

    private boolean isStringContainsMatch(final String termValue, final Object attribute) {
        if (attribute == null && termValue == null) {
            return true;
        } else if (attribute == null) {
            return false;
        } else if (termValue == null || termValue.isEmpty()) {
            return true;
        } else {
            return isStringMatch(termValue, attribute) || attribute.toString().contains(termValue);
        }
    }

    private BigDecimal getNumber(final String fieldName, final Object value) {
        if (value == null) {
            return null;
        } else {
            try {
                if (value instanceof Long) {
                    return BigDecimal.valueOf((long) value);
                } else if (value instanceof Double) {
                    return BigDecimal.valueOf((Double) value);
                }
                return new BigDecimal(value.toString());
            } catch (final NumberFormatException e) {
                throw new MatchException(
                        "Expected a numeric value for field \"" + fieldName +
                                "\" but was given string \"" + value + "\"");
            }
        }
    }

    private Long getDate(final String fieldName, final Object value) {
        if (value == null) {
            return null;
        } else {
            if (value instanceof String) {
                String valueStr = (String) value;
                try {
                    return DateUtil.parseNormalDateTimeString(valueStr);
                } catch (final NumberFormatException e) {
                    throw new MatchException(
                            "Unable to parse a date/time from value \"" + valueStr + "\"");
                }
            } else {
                throw new MatchException(
                        "Expected a string value for field \"" + fieldName + "\" but was given \"" + value
                                + "\" of type " + value.getClass().getName());
            }
        }
    }

    private Long getDate(final Object value) {
        if (value == null) {
            return null;
        } else {
            if (value instanceof String) {
                String valueStr = (String) value;
                try {
                    // This is a term value so will be IDO format
                    return DateUtil.parseNormalDateTimeString(valueStr);
                } catch (final NumberFormatException e) {
                    throw new MatchException(
                            "Unable to parse a date/time from value \"" + valueStr + "\"");
                }
            } else {
                throw new MatchException(
                        "Expected a string value but was given \"" + value
                                + "\" of type " + value.getClass().getName());
            }
        }
    }

    private BigDecimal[] getNumbers(final String fieldName, final Object value) {
        if (value == null) {
            return new BigDecimal[0];
        } else {
            final String[] values = value.toString().split(DELIMITER);
            final BigDecimal[] numbers = new BigDecimal[values.length];
            for (int i = 0; i < values.length; i++) {
                numbers[i] = getNumber(fieldName, values[i].trim());
            }

            return numbers;
        }
    }

    private Long[] getDates(final Object value) {
        if (value == null) {
            return new Long[0];
        } else {
            final String[] values = value.toString().split(DELIMITER);
            final Long[] dates = new Long[values.length];
            for (int i = 0; i < values.length; i++) {
                dates[i] = getDate(values[i].trim());
            }

            return dates;
        }
    }

    private static class MatchException extends RuntimeException {

        MatchException(final String message) {
            super(message);
        }
    }
}
