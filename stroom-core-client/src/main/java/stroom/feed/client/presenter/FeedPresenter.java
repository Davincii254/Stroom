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

package stroom.feed.client.presenter;

import stroom.data.client.presenter.ClassificationWrappedMetaPresenter;
import stroom.data.client.presenter.ProcessorTaskPresenter;
import stroom.docref.DocRef;
import stroom.entity.client.presenter.ContentCallback;
import stroom.entity.client.presenter.DocumentEditTabPresenter;
import stroom.entity.client.presenter.LinkTabPanelView;
import stroom.entity.client.presenter.TabContentProvider;
import stroom.feed.shared.FeedDoc;
import stroom.security.client.api.ClientSecurityContext;
import stroom.security.shared.PermissionNames;
import stroom.widget.tab.client.presenter.TabData;
import stroom.widget.tab.client.presenter.TabDataImpl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.PresenterWidget;

public class FeedPresenter extends DocumentEditTabPresenter<LinkTabPanelView, FeedDoc> {

    private static final TabData SETTINGS = new TabDataImpl("Settings");
    private static final TabData DATA = new TabDataImpl("Data");
    // private static final Tab MONITORING = new Tab("Monitoring");
    private static final TabData TASKS = new TabDataImpl("Active Tasks");

    private final TabContentProvider<FeedDoc> tabContentProvider = new TabContentProvider<>();

    @Inject
    public FeedPresenter(final EventBus eventBus,
                         final LinkTabPanelView view,
                         final ClientSecurityContext securityContext,
                         final Provider<FeedSettingsPresenter> settingsPresenterProvider,
                         final Provider<ClassificationWrappedMetaPresenter> metaPresenterProvider,
                         final Provider<ProcessorTaskPresenter> taskPresenterProvider) {
        super(eventBus, view, securityContext);

        tabContentProvider.setDirtyHandler(event -> {
            if (event.isDirty()) {
                setDirty(true);
            }
        });

        TabData selectedTab = SETTINGS;

        if (securityContext.hasAppPermission(PermissionNames.VIEW_DATA_PERMISSION)) {
            addTab(DATA);
            tabContentProvider.add(DATA, metaPresenterProvider);

            selectedTab = DATA;
        }

        if (securityContext.hasAppPermission(PermissionNames.MANAGE_PROCESSORS_PERMISSION)) {
            addTab(TASKS);
            tabContentProvider.add(TASKS, taskPresenterProvider);
        }

        addTab(SETTINGS);
        tabContentProvider.add(SETTINGS, settingsPresenterProvider);

        selectTab(selectedTab);
    }

    @Override
    protected void getContent(final TabData tab, final ContentCallback callback) {
        callback.onReady(tabContentProvider.getPresenter(tab));
    }

    @Override
    public void onRead(final DocRef docRef, final FeedDoc feed) {
        super.onRead(docRef, feed);
        tabContentProvider.read(docRef, feed);
    }

    @Override
    protected void onWrite(final FeedDoc feed) {
        tabContentProvider.write(feed);
        // Something has changed, e.g. the encoding so refresh the meta presenter to reflect it
        final PresenterWidget<?> presenter = tabContentProvider.getPresenter(DATA);
        if (presenter instanceof ClassificationWrappedMetaPresenter) {
            final ClassificationWrappedMetaPresenter metaPresenter =
                    (ClassificationWrappedMetaPresenter) presenter;
            metaPresenter.refreshData();
        }
    }

    @Override
    public void onReadOnly(final boolean readOnly) {
        super.onReadOnly(readOnly);
        tabContentProvider.onReadOnly(readOnly);
    }

    @Override
    public String getType() {
        return FeedDoc.DOCUMENT_TYPE;
    }
}
