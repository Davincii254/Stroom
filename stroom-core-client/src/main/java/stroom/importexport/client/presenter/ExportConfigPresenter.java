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

package stroom.importexport.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.core.client.LocationManager;
import stroom.dispatch.client.ExportFileCompleteUtil;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.explorer.client.presenter.DocumentTypeCache;
import stroom.explorer.client.presenter.EntityCheckTreePresenter;
import stroom.explorer.client.presenter.TypeFilterPresenter;
import stroom.explorer.shared.ExplorerNode;
import stroom.importexport.client.event.ExportConfigEvent;
import stroom.importexport.shared.ContentResource;
import stroom.security.shared.DocumentPermissionNames;
import stroom.util.shared.DocRefs;
import stroom.util.shared.ResourceGeneration;
import stroom.widget.popup.client.event.DisablePopupEvent;
import stroom.widget.popup.client.event.EnablePopupEvent;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.DefaultPopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupPosition;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupView.PopupType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.MyPresenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.ProxyEvent;
import com.gwtplatform.mvp.client.proxy.Proxy;

import java.util.HashSet;
import java.util.Set;

public class ExportConfigPresenter
        extends MyPresenter<ExportConfigPresenter.ExportConfigView, ExportConfigPresenter.ExportProxy>
        implements ExportConfigUiHandlers, ExportConfigEvent.Handler {

    private static final ContentResource CONTENT_RESOURCE = GWT.create(ContentResource.class);

    private final LocationManager locationManager;
    private final EntityCheckTreePresenter treePresenter;
    private final TypeFilterPresenter typeFilterPresenter;
    private final RestFactory restFactory;
    private final DocumentTypeCache documentTypeCache;

    @Inject
    public ExportConfigPresenter(final EventBus eventBus,
                                 final ExportConfigView view,
                                 final ExportProxy proxy,
                                 final LocationManager locationManager,
                                 final EntityCheckTreePresenter treePresenter,
                                 final TypeFilterPresenter typeFilterPresenter,
                                 final RestFactory restFactory,
                                 final DocumentTypeCache documentTypeCache) {
        super(eventBus, view, proxy);
        this.locationManager = locationManager;
        this.treePresenter = treePresenter;
        this.typeFilterPresenter = typeFilterPresenter;
        this.restFactory = restFactory;
        this.documentTypeCache = documentTypeCache;
        view.setTreeView(treePresenter.getView());
        view.setUiHandlers(this);
    }

    @Override
    protected void onBind() {
        registerHandler(typeFilterPresenter.addDataSelectionHandler(event -> treePresenter.setIncludedTypeSet(
                typeFilterPresenter.getIncludedTypes())));
    }

    @ProxyEvent
    @Override
    public void onExport(final ExportConfigEvent event) {
        if (event.getSelection() != null) {
            for (final ExplorerNode node : event.getSelection()) {
                treePresenter.getTreeModel().setEnsureVisible(new HashSet<>(event.getSelection()));
                treePresenter.getSelectionModel().setSelected(node, true);
            }
        }

        forceReveal();
    }

    @Override
    protected void revealInParent() {
        documentTypeCache.clear();
        // Set the data for the type filter.
        documentTypeCache.fetch(typeFilterPresenter::setDocumentTypes);

        treePresenter.setRequiredPermissions(DocumentPermissionNames.READ);
        treePresenter.refresh();

        final PopupUiHandlers popupUiHandlers = new DefaultPopupUiHandlers() {
            @Override
            public void onHideRequest(final boolean autoClose, final boolean ok) {
                if (ok) {
                    export();
                } else {
                    HidePopupEvent.fire(ExportConfigPresenter.this, ExportConfigPresenter.this, false, false);
                }
            }
        };

        final PopupSize popupSize = PopupSize.resizable(500, 600);
        ShowPopupEvent.fire(this, this, PopupType.OK_CANCEL_DIALOG, popupSize, "Export", popupUiHandlers);
    }

    private void export() {
        // Disable the popup ok/cancel buttons before we attempt export.
        DisablePopupEvent.fire(this, this);

        final Set<ExplorerNode> dataItems = treePresenter.getSelectionModel().getSelectedSet();
        if (dataItems == null || dataItems.size() == 0) {
            // Let the user know that they didn't select anything to export.
            AlertEvent.fireWarn(this, "No folders have been selected for export", null);
            // Re-enable buttons on this popup.
            EnablePopupEvent.fire(this, this);

        } else {
            final Set<DocRef> docRefs = new HashSet<>();
            for (final ExplorerNode explorerNode : dataItems) {
                docRefs.add(explorerNode.getDocRef());
            }

            final Rest<ResourceGeneration> rest = restFactory.create();
            rest
                    .onSuccess(result -> ExportFileCompleteUtil.onSuccess(locationManager, this, result))
                    .onFailure(throwable -> ExportFileCompleteUtil.onFailure(this, throwable))
                    .call(CONTENT_RESOURCE)
                    .exportContent(new DocRefs(docRefs));
        }
    }

    @Override
    public void changeQuickFilter(final String name) {
        treePresenter.changeNameFilter(name);
    }

    @Override
    public void showTypeFilter(final MouseDownEvent event) {
        final Element target = event.getNativeEvent().getEventTarget().cast();

        final PopupPosition popupPosition = new PopupPosition(target.getAbsoluteLeft() - 1,
                target.getAbsoluteTop() + target.getClientHeight() + 2);
        ShowPopupEvent.fire(
                this,
                typeFilterPresenter,
                PopupType.POPUP,
                popupPosition,
                null,
                target);
    }

    public interface ExportConfigView extends View, HasUiHandlers<ExportConfigUiHandlers> {

        void setTreeView(View view);
    }

    @ProxyCodeSplit
    public interface ExportProxy extends Proxy<ExportConfigPresenter> {

    }
}
