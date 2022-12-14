/*
 * Copyright 2016 Crown Copyright
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
 */

package stroom.preferences.client;

import stroom.cell.tickbox.shared.TickBoxState;
import stroom.item.client.ItemListBox;
import stroom.item.client.StringListBox;
import stroom.preferences.client.UserPreferencesPresenter.UserPreferencesView;
import stroom.query.api.v2.TimeZone;
import stroom.query.api.v2.TimeZone.Use;
import stroom.widget.tickbox.client.view.TickBox;
import stroom.widget.valuespinner.client.ValueSpinner;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.RowFormatter;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

import java.util.Arrays;
import java.util.List;

public final class UserPreferencesViewImpl
        extends ViewWithUiHandlers<UserPreferencesUiHandlers>
        implements UserPreferencesView {

    public static final List<String> STANDARD_FORMATS = Arrays
            .asList("yyyy-MM-dd'T'HH:mm:ss.SSSXX",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS xx",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS xxx",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS VV",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "dd/MM/yyyy HH:mm:ss",
                    "dd/MM/yy HH:mm:ss",
                    "MM/dd/yyyy HH:mm:ss",
                    "d MMM yyyy HH:mm:ss",
                    "yyyy-MM-dd",
                    "dd/MM/yyyy",
                    "dd/MM/yy",
                    "MM/dd/yyyy",
                    "d MMM yyyy");

    private final Widget widget;

    @UiField
    Grid grid;
    @UiField
    StringListBox theme;
    @UiField
    StringListBox editorTheme;
    @UiField
    StringListBox density;
    @UiField
    StringListBox font;
    @UiField
    StringListBox fontSize;
    @UiField
    StringListBox format;
    @UiField
    TickBox custom;
    @UiField
    TextBox text;
    @UiField
    ItemListBox<Use> timeZoneUse;
    @UiField
    StringListBox timeZoneId;
    @UiField
    ValueSpinner timeZoneOffsetHours;
    @UiField
    ValueSpinner timeZoneOffsetMinutes;
    @UiField
    Button setAsDefault;
    @UiField
    Button revertToDefault;

    @Inject
    public UserPreferencesViewImpl(final Binder binder) {
        widget = binder.createAndBindUi(this);

        theme.addChangeHandler(event -> {
            if (getUiHandlers() != null) {
                getUiHandlers().onChange();
            }
        });
        editorTheme.addChangeHandler(event -> {
            if (getUiHandlers() != null) {
                getUiHandlers().onChange();
            }
        });
        density.addChangeHandler(event -> {
            if (getUiHandlers() != null) {
                getUiHandlers().onChange();
            }
        });
        font.addChangeHandler(event -> {
            if (getUiHandlers() != null) {
                getUiHandlers().onChange();
            }
        });
        fontSize.addChangeHandler(event -> {
            if (getUiHandlers() != null) {
                getUiHandlers().onChange();
            }
        });

        density.addItem("Default");
        density.addItem("Comfortable");
        density.addItem("Compact");

        fontSize.addItem("Small");
        fontSize.addItem("Medium");
        fontSize.addItem("Large");

        format.addItems(STANDARD_FORMATS);

        timeZoneUse.addItem(TimeZone.Use.LOCAL);
        timeZoneUse.addItem(TimeZone.Use.UTC);
        timeZoneUse.addItem(TimeZone.Use.ID);
        timeZoneUse.addItem(TimeZone.Use.OFFSET);

        for (final String tz : getTimeZoneIds()) {
            timeZoneId.addItem(tz);
        }

        timeZoneOffsetHours.setMin(-12);
        timeZoneOffsetHours.setMax(12);
        timeZoneOffsetHours.setValue(0);
        timeZoneOffsetHours.setMinStep(1);
        timeZoneOffsetHours.setMaxStep(1);

        timeZoneOffsetMinutes.setMin(0);
        timeZoneOffsetMinutes.setMax(45);
        timeZoneOffsetMinutes.setValue(0);
        timeZoneOffsetMinutes.setMinStep(15);
        timeZoneOffsetMinutes.setMaxStep(15);
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public String getTheme() {
        return theme.getSelected();
    }

    @Override
    public void setTheme(final String theme) {
        this.theme.setSelected(theme);
    }

    @Override
    public void setThemes(final List<String> themes) {
        this.theme.clear();
        this.theme.addItems(themes);
    }

    @Override
    public String getEditorTheme() {
        return editorTheme.getSelected();
    }

    @Override
    public void setEditorTheme(final String editorTheme) {
        this.editorTheme.setSelected(editorTheme);
    }

    @Override
    public void setEditorThemes(final List<String> editorThemes) {
        this.editorTheme.clear();
        this.editorTheme.addItem("");
        this.editorTheme.addItems(editorThemes);
    }

    @Override
    public String getDensity() {
        return density.getSelected();
    }

    @Override
    public void setDensity(final String density) {
        if (density == null) {
            this.density.setSelected("Default");
        } else {
            this.density.setSelected(density);
        }
    }

    @Override
    public String getFont() {
        return font.getSelected();
    }

    @Override
    public void setFont(final String font) {
        this.font.setSelected(font);
    }

    @Override
    public void setFonts(final List<String> fonts) {
        this.font.clear();
        this.font.addItems(fonts);
    }

    @Override
    public String getFontSize() {
        return this.fontSize.getSelected();
    }

    @Override
    public void setFontSize(final String fontSize) {
        this.fontSize.setSelected(fontSize);
    }

    @Override
    public String getPattern() {
        if (custom.getBooleanValue()) {
            return text.getText();
        }

        return format.getSelected();
    }

    @Override
    public void setPattern(final String pattern) {
        String text = pattern;
        if (text == null || text.trim().length() == 0) {
            text = STANDARD_FORMATS.get(0);
        }

        if (!text.equals(this.format.getSelected())) {
            this.format.setSelected(text);
        }

        final boolean custom = this.format.getSelectedIndex() == -1;
        this.custom.setBooleanValue(custom);
        this.text.setEnabled(custom);
        this.text.setText(text);
    }

    @Override
    public Use getTimeZoneUse() {
        return this.timeZoneUse.getSelectedItem();
    }

    @Override
    public void setTimeZoneUse(final Use use) {
        this.timeZoneUse.setSelectedItem(use);
        changeVisible();
    }

    @Override
    public String getTimeZoneId() {
        return this.timeZoneId.getSelected();
    }

    @Override
    public void setTimeZoneId(final String timeZoneId) {
        this.timeZoneId.setSelected(timeZoneId);
    }

    @Override
    public Integer getTimeZoneOffsetHours() {
        final int val = this.timeZoneOffsetHours.getValue();
        if (val == 0) {
            return null;
        }
        return val;
    }

    @Override
    public void setTimeZoneOffsetHours(final Integer timeZoneOffsetHours) {
        if (timeZoneOffsetHours == null) {
            this.timeZoneOffsetHours.setValue(0);
        } else {
            this.timeZoneOffsetHours.setValue(timeZoneOffsetHours);
        }
    }

    @Override
    public Integer getTimeZoneOffsetMinutes() {
        final int val = this.timeZoneOffsetMinutes.getValue();
        if (val == 0) {
            return null;
        }
        return val;
    }

    @Override
    public void setTimeZoneOffsetMinutes(final Integer timeZoneOffsetMinutes) {
        if (timeZoneOffsetMinutes == null) {
            this.timeZoneOffsetMinutes.setValue(0);
        } else {
            this.timeZoneOffsetMinutes.setValue(timeZoneOffsetMinutes);
        }
    }

    @Override
    public void setAsDefaultVisible(final boolean visible) {
        setAsDefault.setVisible(visible);
    }


    public void changeVisible() {
        final RowFormatter formatter = grid.getRowFormatter();
        formatter.setVisible(9, TimeZone.Use.ID.equals(this.timeZoneUse.getSelectedItem()));
        formatter.setVisible(10, TimeZone.Use.OFFSET.equals(this.timeZoneUse.getSelectedItem()));
    }

    @UiHandler("custom")
    public void onTickBoxClick(final ValueChangeEvent<TickBoxState> event) {
        text.setEnabled(custom.getBooleanValue());
    }

    @UiHandler("format")
    public void onFormatChange(final ChangeEvent event) {
        setPattern(this.format.getSelected());
    }

    @UiHandler("timeZoneUse")
    public void onTimeZoneUseChange(final SelectionEvent<TimeZone.Use> event) {
        changeVisible();
    }

    @UiHandler("setAsDefault")
    void onClickSetAsDefault(final ClickEvent event) {
        if (getUiHandlers() != null) {
            getUiHandlers().onSetAsDefault();
        }
    }

    @UiHandler("revertToDefault")
    void onClickRevertToDefault(final ClickEvent event) {
        if (getUiHandlers() != null) {
            getUiHandlers().onRevertToDefault();
        }
    }

    public interface Binder extends UiBinder<Widget, UserPreferencesViewImpl> {

    }

    private static native String[] getTimeZoneIds()/*-{
        return $wnd.moment.tz.names();
    }-*/;
}
