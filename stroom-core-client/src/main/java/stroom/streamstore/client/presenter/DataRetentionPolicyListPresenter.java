/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.streamstore.client.presenter;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.streamstore.shared.DataRetentionRule;
import stroom.util.client.BorderUtil;
import stroom.widget.button.client.GlyphButtonView;
import stroom.widget.button.client.GlyphIcon;
import stroom.widget.util.client.MultiSelectionModel;

import java.util.List;

public class DataRetentionPolicyListPresenter extends MyPresenterWidget<DataGridView<DataRetentionRule>> {
    @Inject
    public DataRetentionPolicyListPresenter(final EventBus eventBus) {
        super(eventBus, new DataGridViewImpl<>(true, false));

        // Add a border to the list.
        BorderUtil.addBorder(getWidget().getElement());

        initTableColumns();
    }

    /**
     * Add the columns to the table.
     */
    private void initTableColumns() {
        // Expression.
        final Column<DataRetentionRule, String> expressionColumn = new Column<DataRetentionRule, String>(new TextCell()) {
            @Override
            public String getValue(final DataRetentionRule row) {
                return row.getExpression().toString();
            }
        };
        getView().addResizableColumn(expressionColumn, "Expression", 500);

        // Age.
        final Column<DataRetentionRule, String> ageColumn = new Column<DataRetentionRule, String>(new TextCell()) {
            @Override
            public String getValue(final DataRetentionRule row) {
                if (row.isForever()) {
                    return "Keep Forever";
                }

                final StringBuilder sb = new StringBuilder()
                        .append(row.getAge())
                        .append(" ")
                        .append(row.getTimeUnit().getDisplayValue());
                if (row.getAge() == 1) {
                    sb.setLength(sb.length() - 1);
                }
                return sb.toString();
            }
        };
        getView().addResizableColumn(ageColumn, "Retention", 300);


        getView().addEndColumn(new EndColumn<>());
    }

    public void setData(final List<DataRetentionRule> data) {
        getView().setRowData(0, data);
        getView().setRowCount(data.size());
    }

    public MultiSelectionModel<DataRetentionRule> getSelectionModel() {
        return getView().getSelectionModel();
    }

    public GlyphButtonView add(final GlyphIcon preset) {
        return getView().addButton(preset);
    }
}
