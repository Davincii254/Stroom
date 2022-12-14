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

package stroom.security.client.presenter;

import stroom.cell.info.client.SvgCell;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.security.shared.FindUserNameCriteria;
import stroom.security.shared.User;
import stroom.security.shared.UserResource;
import stroom.svg.client.Preset;
import stroom.svg.client.SvgPresets;
import stroom.widget.button.client.ButtonView;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.DefaultPopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupView;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;

import java.util.function.Consumer;

public class SelectUserPresenter extends MyPresenterWidget<UserListView> implements UserListUiHandlers {

    private static final UserResource USER_RESOURCE = GWT.create(UserResource.class);

    private final DataGridView<String> dataGridView;
    private final RestFactory restFactory;
    private final ButtonView newButton;
    private final ManageNewEntityPresenter newPresenter;
    private UserNameDataProvider dataProvider;
    private FindUserNameCriteria findUserCriteria;
    private Consumer<User> userConsumer;

    @Inject
    public SelectUserPresenter(final EventBus eventBus,
                               final UserListView userListView,
                               final RestFactory restFactory,
                               final ManageNewEntityPresenter newPresenter) {
        super(eventBus, userListView);
        this.restFactory = restFactory;
        this.newPresenter = newPresenter;

        dataGridView = new DataGridViewImpl<>(true);
        userListView.setDatGridView(dataGridView);
        userListView.setUiHandlers(this);

        // Icon
        dataGridView.addColumn(new Column<String, Preset>(new SvgCell()) {
            @Override
            public Preset getValue(final String userRef) {
                return SvgPresets.USER;
            }
        }, "</br>", 20);

        // Name.
        dataGridView.addResizableColumn(new Column<String, String>(new TextCell()) {
            @Override
            public String getValue(final String userRef) {
                return userRef;
            }
        }, "Name", 350);

        dataGridView.addEndColumn(new EndColumn<>());
        newButton = dataGridView.addButton(SvgPresets.NEW_ITEM);
    }

    @Override
    protected void onBind() {
        super.onBind();
        registerHandler(newButton.addClickHandler(event -> {
            if (event.getNativeButton() == NativeEvent.BUTTON_LEFT) {
                onNew();
            }
        }));
        registerHandler(dataGridView.getSelectionModel().addSelectionHandler(event -> {
            if (event.getSelectionType().isDoubleSelect()) {
                final String selected = dataGridView.getSelectionModel().getSelected();
                hide(false, true, selected);
            }
        }));
    }

    private void onNew() {
        final PopupUiHandlers hidePopupUiHandlers = new DefaultPopupUiHandlers() {
            @Override
            public void onHideRequest(final boolean autoClose, final boolean ok) {
                if (ok) {
                    final String selected = newPresenter.getName();
                    hide(false, true, selected);
                }
                newPresenter.hide();
            }
        };

        newPresenter.show(hidePopupUiHandlers);
    }

    @Override
    public void changeNameFilter(String name) {
        if (findUserCriteria != null) {
            String filter = name;

            if (filter != null) {
                filter = filter.trim();
                if (filter.length() == 0) {
                    filter = null;
                }
            }

            if ((filter == null && findUserCriteria.getQuickFilterInput() == null) ||
                    (filter != null && filter.equals(findUserCriteria.getQuickFilterInput()))) {
                return;
            }

            findUserCriteria.setQuickFilterInput(filter);
            dataProvider.refresh();
        }
    }

    public void show(final Consumer<User> userConsumer) {
        this.userConsumer = userConsumer;
        final FindUserNameCriteria findUserCriteria = new FindUserNameCriteria();
        setup(findUserCriteria);

        final PopupSize popupSize = PopupSize.resizable(400, 400);
        final PopupUiHandlers popupUiHandlers = new DefaultPopupUiHandlers() {
            @Override
            public void onHideRequest(boolean autoClose, boolean ok) {
                final String selected = dataGridView.getSelectionModel().getSelected();
                hide(autoClose, ok, selected);
            }
        };
        ShowPopupEvent.fire(
                SelectUserPresenter.this,
                SelectUserPresenter.this,
                PopupView.PopupType.OK_CANCEL_DIALOG,
                popupSize,
                "Choose User To Add",
                popupUiHandlers);
    }

    private void hide(final boolean autoClose, final boolean ok, final String selected) {
        if (ok && userConsumer != null && selected != null && selected.length() > 0) {
            final Rest<User> rest = restFactory.create();
            rest
                    .onSuccess(user -> {
                        userConsumer.accept(user);
                        HidePopupEvent.fire(
                                this,
                                this,
                                autoClose,
                                ok);
                    })
                    .call(USER_RESOURCE)
                    .create(selected, false);
        } else {
            HidePopupEvent.fire(
                    this,
                    this,
                    autoClose,
                    ok);
        }
    }

    public void setup(final FindUserNameCriteria findUserCriteria) {
        this.findUserCriteria = findUserCriteria;
        dataProvider = new UserNameDataProvider(getEventBus(), restFactory, getDataGridView());
        dataProvider.setCriteria(findUserCriteria);
        refresh();
    }

    public void refresh() {
        dataProvider.refresh();
    }

    public DataGridView<String> getDataGridView() {
        return dataGridView;
    }
}
