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

import stroom.dispatch.client.RestFactory;
import stroom.security.shared.FindUserCriteria;
import stroom.security.shared.User;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.DefaultPopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupView;
import stroom.widget.popup.client.presenter.Size;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import java.util.function.Consumer;

public class SelectGroupPresenter extends AbstractDataUserListPresenter {

    private Consumer<User> userConsumer;

    @Inject
    public SelectGroupPresenter(final EventBus eventBus,
                                final UserListView userListView,
                                final RestFactory restFactory) {
        super(eventBus, userListView, restFactory);
    }

    @Override
    protected void onBind() {
        super.onBind();
        registerHandler(getSelectionModel().addSelectionHandler(event -> {
            if (event.getSelectionType().isDoubleSelect() && getSelectionModel().getSelected() != null) {
                if (findUserCriteria != null && findUserCriteria.getRelatedUser() == null) {
                    hide(false, true);
                }
            }
        }));
    }

    public void show(final Consumer<User> groupConsumer) {
        this.userConsumer = groupConsumer;
        final FindUserCriteria findUserCriteria = new FindUserCriteria();

        // If we are a group then get users and vice versa.
        findUserCriteria.setGroup(true);
        setup(findUserCriteria);

        final PopupSize popupSize = PopupSize.builder()
                .width(Size
                        .builder()
                        .initial(400)
                        .min(400)
                        .resizable(true)
                        .build())
                .height(Size
                        .builder()
                        .initial(400)
                        .min(400)
                        .resizable(true)
                        .build())
                .build();
        final PopupUiHandlers popupUiHandlers = new DefaultPopupUiHandlers() {
            @Override
            public void onHideRequest(boolean autoClose, boolean ok) {
                hide(autoClose, ok);
            }
        };
        ShowPopupEvent.fire(this,
                this,
                PopupView.PopupType.OK_CANCEL_DIALOG,
                popupSize,
                "Choose Group To Add",
                popupUiHandlers);
    }

    private void hide(boolean autoClose, boolean ok) {
        if (ok && userConsumer != null) {
            final User selected = getSelectionModel().getSelected();
            if (selected != null) {
                userConsumer.accept(selected);
            }
        }

        HidePopupEvent.fire(
                this,
                this,
                autoClose,
                ok);
    }
}
