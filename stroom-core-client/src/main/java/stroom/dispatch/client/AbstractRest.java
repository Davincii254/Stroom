package stroom.dispatch.client;

import stroom.alert.client.event.AlertEvent;
import stroom.util.client.JSONUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import org.fusesource.restygwt.client.DirectRestService;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.REST;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;

public abstract class AbstractRest<R> implements Rest<R> {

    private final REST<R> rest;
    private Consumer<R> resultConsumer;
    private Consumer<Throwable> errorConsumer;

    AbstractRest(final HasHandlers hasHandlers) {
        final MethodCallback<R> methodCallback = new MethodCallback<R>() {
            @Override
            public void onFailure(final Method method, final Throwable exception) {
                try {
                    // The exception is restyGWT's FailedResponseException
                    // so extract the response payload and treat it as WebApplicationException
                    // json
//                        String msg = exception.getMessage();
                    Throwable throwable = null;

                    if (method.getResponse() != null &&
                            method.getResponse().getText() != null &&
                            !method.getResponse().getText().trim().isEmpty()) {
                        final String responseText = method.getResponse().getText().trim();
                        final String contentType = method.getResponse().getHeader("Content-Type");

                        if (MediaType.TEXT_PLAIN.equals(contentType)) {
                            throwable = getThrowableFromStringResponse(method, exception, responseText);
                        } else {
                            try {
                                throwable = getThrowableFromJsonResponse(method, exception, responseText);
                            } catch (Exception e) {
                                GWT.log("Error parsing response as json: " + e.getMessage());
                                try {
                                    // Try parsing it as text
                                    throwable = getThrowableFromStringResponse(method, exception, responseText);
                                } catch (Exception e2) {
                                    GWT.log("Error parsing response as text: " + e.getMessage());
                                }
                            }
                        }
                    }

                    // Fallback
                    if (throwable == null) {
                        throwable = exception;
                    }

                    if (errorConsumer != null) {
                        errorConsumer.accept(throwable);
                    } else {
                        GWT.log(throwable.getMessage(), throwable);

                        if (throwable instanceof ResponseException) {
                            final ResponseException responseException = (ResponseException) throwable;
                            String details = responseException.getDetails();
                            if (details != null && details.trim().length() > 0) {
                                AlertEvent.fireError(hasHandlers,
                                        throwable.getMessage(),
                                        details.trim(),
                                        null);
                            } else {
                                AlertEvent.fireError(hasHandlers, throwable.getMessage(), null);
                            }
                        } else {
                            AlertEvent.fireError(hasHandlers, throwable.getMessage(), null);
                        }
                    }
                } catch (final Throwable t) {
                    GWT.log(method.getRequest().toString());
                    GWT.log(t.getMessage(), t);
                    AlertEvent.fireErrorFromException(hasHandlers, t, null);
                } finally {
                    decrementTaskCount();
                }
            }

            @Override
            public void onSuccess(final Method method, final R response) {
                try {
                    if (resultConsumer != null) {
                        resultConsumer.accept(response);
                    }
                } catch (final Throwable t) {
                    GWT.log(method.getRequest().toString());
                    GWT.log(t.getMessage(), t);
                    AlertEvent.fireErrorFromException(hasHandlers, t, null);
                } finally {
                    decrementTaskCount();
                }
            }
        };
        rest = REST.withCallback(methodCallback);
    }

    private Throwable getThrowableFromStringResponse(final Method method,
                                                     final Throwable throwable,
                                                     final String response) {
        return new ResponseException(
                method.builder.getHTTPMethod(),
                method.builder.getUrl(),
                null,
                method.getResponse().getStatusCode(),
                response,
                null,
                null,
                throwable);
    }

    private Throwable getThrowableFromJsonResponse(final Method method,
                                                   final Throwable throwable,
                                                   final String json) {

        final Throwable newThrowable;
        // Assuming we get a response like { "code": "", "message": "" } or
        // { "code": "", "details": "" }
        final JSONObject responseJson = (JSONObject) JSONParser.parseStrict(json);
        final Integer code = JSONUtil.getInteger(responseJson.get("code"));
        final String message = JSONUtil.getString(responseJson.get("message"));
        final String details = JSONUtil.getString(responseJson.get("details"));
        final String responseKeyValues = responseJson.keySet()
                .stream()
                .map(key -> {
                    final String val = getJsonKey(responseJson, key);
                    return val != null
                            ? key + ": " + val
                            : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

        if (message != null && message.length() > 0) {
            newThrowable = new ResponseException(
                    method.builder.getHTTPMethod(),
                    method.builder.getUrl(),
                    json,
                    code,
                    message,
                    details,
                    responseKeyValues,
                    throwable);

        } else {
            final String msg = "Error calling " +
                    method.builder.getHTTPMethod() +
                    " " +
                    method.builder.getUrl() +
                    " - " +
                    responseKeyValues;
            newThrowable = new RuntimeException(msg, throwable);
        }
        return newThrowable;
    }

    @Override
    public Rest<R> onSuccess(Consumer<R> consumer) {
        resultConsumer = consumer;
        return this;
    }

    @Override
    public Rest<R> onFailure(Consumer<Throwable> consumer) {
        errorConsumer = consumer;
        return this;
    }

    @Override
    public <T extends DirectRestService> T call(T service) {
        incrementTaskCount();
        return rest.call(service);
    }

    protected abstract void incrementTaskCount();

    protected abstract void decrementTaskCount();

    private String getJsonKey(final JSONObject jsonObject, final String key) {

        final String value;
        if (jsonObject.containsKey(key)) {
            final JSONValue jsonValue = jsonObject.get(key);
            if (jsonValue.isString() != null) {
                value = jsonValue.isString().stringValue();
            } else if (jsonValue.isNumber() != null) {
                value = Double.toString(jsonValue.isNumber().doubleValue());
            } else if (jsonValue.isBoolean() != null) {
                value = Boolean.toString(jsonValue.isBoolean().booleanValue());
            } else {
                // Just give back the json
                value = jsonValue.toString();
            }
        } else {
            value = null;
        }
        return value;
    }
}