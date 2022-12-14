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

package stroom.pipeline.structure.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.data.shared.StreamTypeNames;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.docstore.shared.DocRefUtil;
import stroom.document.client.event.DirtyEvent;
import stroom.document.client.event.DirtyEvent.DirtyHandler;
import stroom.document.client.event.HasDirtyHandlers;
import stroom.entity.client.presenter.ReadOnlyChangeHandler;
import stroom.explorer.shared.ExplorerResource;
import stroom.pipeline.shared.PipelineDoc;
import stroom.pipeline.shared.data.PipelineElement;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.pipeline.shared.data.PipelinePropertyType;
import stroom.pipeline.shared.data.PipelineReference;
import stroom.svg.client.SvgPresets;
import stroom.widget.button.client.ButtonView;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupView.PopupType;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.MyPresenterWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PipelineReferenceListPresenter extends MyPresenterWidget<DataGridView<PipelineReference>>
        implements HasDirtyHandlers, ReadOnlyChangeHandler {

    private static final ExplorerResource EXPLORER_RESOURCE = GWT.create(ExplorerResource.class);
    private static final String ADDED = "pipelineStructureViewImpl-property-added";
    private static final String REMOVED = "pipelineStructureViewImpl-property-removed";
    private static final String INHERITED = "pipelineStructureViewImpl-property-inherited";
    private final ButtonView addButton;
    private final ButtonView editButton;
    private final ButtonView removeButton;
    private final Map<PipelineReference, State> referenceStateMap = new HashMap<>();
    private final List<PipelineReference> references = new ArrayList<>();
    private final Provider<NewPipelineReferencePresenter> newPipelineReferencePresenter;
    private final RestFactory restFactory;

    private Map<PipelineElementType, Map<String, PipelinePropertyType>> allPropertyTypes;
    private PipelineDoc pipeline;
    private PipelineModel pipelineModel;
    private PipelineElement currentElement;
    private PipelinePropertyType propertyType;

    private boolean readOnly = true;

    @Inject
    public PipelineReferenceListPresenter(final EventBus eventBus,
                                          final Provider<NewPipelineReferencePresenter> newPipelineReferencePresenter,
                                          final RestFactory restFactory) {
        super(eventBus, new DataGridViewImpl<>(true));
        this.newPipelineReferencePresenter = newPipelineReferencePresenter;
        this.restFactory = restFactory;

        addButton = getView().addButton(SvgPresets.NEW_ITEM);
        editButton = getView().addButton(SvgPresets.EDIT);
        removeButton = getView().addButton(SvgPresets.REMOVE);

        addColumns();

        enableButtons();
    }

    @Override
    protected void onBind() {
        registerHandler(getView().getSelectionModel().addSelectionHandler(event -> {
            enableButtons();
            if (event.getSelectionType().isDoubleSelect()) {
                onEdit(getView().getSelectionModel().getSelected());
            }
        }));
        registerHandler(addButton.addClickHandler(event -> {
            if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                onAdd(event);
            }
        }));
        registerHandler(editButton.addClickHandler(event -> {
            if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                onEdit(getView().getSelectionModel().getSelected());
            }
        }));
        registerHandler(removeButton.addClickHandler(event -> {
            if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                onRemove();
            }
        }));
    }

    private void addColumns() {
        addPipelineColumn();
        addFeedColumn();
        addStreamTypeColumn();
        addInheritedFromColumn();

        addEndColumn();
    }

    private void addPipelineColumn() {
        // Pipeline.
        getView().addResizableColumn(new Column<PipelineReference, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(final PipelineReference pipelineReference) {
                if (pipelineReference.getPipeline() == null) {
                    return null;
                }
                return getSafeHtmlWithState(pipelineReference, pipelineReference.getPipeline().getName());
            }
        }, "Pipeline", 200);
    }

    private void addFeedColumn() {
        // Feed.
        getView().addResizableColumn(new Column<PipelineReference, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(final PipelineReference pipelineReference) {
                if (pipelineReference.getFeed() == null) {
                    return null;
                }
                return getSafeHtmlWithState(pipelineReference, pipelineReference.getFeed().getName());
            }
        }, "Feed", 200);
    }

    private void addStreamTypeColumn() {
        // Stream type.
        getView().addResizableColumn(new Column<PipelineReference, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(final PipelineReference pipelineReference) {
                if (pipelineReference.getStreamType() == null) {
                    return null;
                }
                return getSafeHtmlWithState(pipelineReference, pipelineReference.getStreamType());
            }
        }, "Type", 200);
    }

    private void addInheritedFromColumn() {
        // Default Value.
        getView().addResizableColumn(new Column<PipelineReference, String>(new TextCell()) {
            @Override
            public String getValue(final PipelineReference pipelineReference) {
                if (pipelineReference.getSourcePipeline() == null
                        || pipeline.getUuid().equals(pipelineReference.getSourcePipeline().getUuid())) {
                    return null;
                }
                return pipelineReference.getSourcePipeline().getName();
            }
        }, "Inherited From", 100);
    }

    private void addEndColumn() {
        getView().addEndColumn(new EndColumn<>());
    }

    private SafeHtml getSafeHtmlWithState(final PipelineReference pipelineReference, final String string) {
        if (string == null) {
            return SafeHtmlUtils.EMPTY_SAFE_HTML;
        }

        String className = null;
        final State state = referenceStateMap.get(pipelineReference);
        switch (state) {
            case ADDED:
                className = ADDED;
                break;
            case REMOVED:
                className = REMOVED;
                break;
            case INHERITED:
                className = INHERITED;
                break;
        }

        final SafeHtmlBuilder builder = new SafeHtmlBuilder();
        builder.append(SafeHtmlUtils.fromTrustedString("<div class=\"" + className + "\">"));
        builder.appendEscaped(string);
        builder.append(SafeHtmlUtils.fromTrustedString("</div>"));
        return builder.toSafeHtml();
    }

    public void setPipeline(final PipelineDoc pipeline) {
        this.pipeline = pipeline;
    }

    public void setPipelineModel(final PipelineModel pipelineModel) {
        this.pipelineModel = pipelineModel;
    }

    public void setCurrentElement(final PipelineElement currentElement) {
        this.currentElement = currentElement;

        // Discover the reference property type.
        this.propertyType = null;
        if (currentElement != null && allPropertyTypes != null) {
            final Map<String, PipelinePropertyType> propertyTypes = allPropertyTypes
                    .get(currentElement.getElementType());
            if (propertyTypes != null) {
                for (final PipelinePropertyType propertyType : propertyTypes.values()) {
                    if (propertyType.isPipelineReference()) {
                        this.propertyType = propertyType;
                    }
                }
            }
        }

        refresh();
    }

    public void setPropertyTypes(final Map<PipelineElementType, Map<String, PipelinePropertyType>> propertyTypes) {
        this.allPropertyTypes = propertyTypes;
    }

    private void onAdd(final ClickEvent event) {
        if (currentElement != null) {
            final PipelineReference pipelineReference = new PipelineReference(currentElement.getId(),
                    propertyType.getName(), null, null, StreamTypeNames.REFERENCE, null);
            pipelineReference.setSourcePipeline(DocRefUtil.create(pipeline));
            showEditor(pipelineReference, true);
        }
    }

    private void onEdit(final PipelineReference pipelineReference) {
        if (pipelineReference != null) {
            // Only allow edit of added references.
            final State state = referenceStateMap.get(pipelineReference);
            if (State.ADDED.equals(state)) {
                showEditor(pipelineReference, false);
            }
        }
    }

    private void showEditor(final PipelineReference pipelineReference, final boolean isNew) {
        if (pipelineReference != null) {
            final List<PipelineReference> added = pipelineModel.getPipelineData().getAddedPipelineReferences();
            added.remove(pipelineReference);

            final NewPipelineReferencePresenter editor = newPipelineReferencePresenter.get();
            editor.read(pipelineReference);

            final PopupUiHandlers popupUiHandlers = new PopupUiHandlers() {
                @Override
                public void onHideRequest(final boolean autoClose, final boolean ok) {
                    if (ok) {
                        editor.write(pipelineReference);

                        if (pipelineReference.getPipeline() == null) {
                            AlertEvent.fireError(PipelineReferenceListPresenter.this,
                                    "You must specify a pipeline to use.", null);
                        } else if (pipelineReference.getFeed() == null) {
                            AlertEvent.fireError(PipelineReferenceListPresenter.this, "You must specify a feed to use.",
                                    null);
                        } else if (pipelineReference.getStreamType() == null) {
                            AlertEvent.fireError(PipelineReferenceListPresenter.this,
                                    "You must specify a stream type to use.", null);
                        } else {
                            if (!added.contains(pipelineReference)) {
                                added.add(pipelineReference);
                            }

                            setDirty(isNew || editor.isDirty());
                            refresh();
                            HidePopupEvent.fire(PipelineReferenceListPresenter.this, editor);
                        }
                    } else {
                        // User has cancelled edit so add the reference back to
                        // the list if this was an existing reference
                        if (!isNew) {
                            if (!added.contains(pipelineReference)) {
                                added.add(pipelineReference);
                            }
                        }

                        HidePopupEvent.fire(PipelineReferenceListPresenter.this, editor);
                    }
                }

                @Override
                public void onHide(final boolean autoClose, final boolean ok) {
                    // Do nothing.
                }
            };

            final PopupSize popupSize = PopupSize.resizableX();
            if (isNew) {
                ShowPopupEvent.fire(this, editor, PopupType.OK_CANCEL_DIALOG, popupSize, "New Pipeline Reference",
                        popupUiHandlers);
            } else {
                ShowPopupEvent.fire(this, editor, PopupType.OK_CANCEL_DIALOG, popupSize, "Edit Pipeline Reference",
                        popupUiHandlers);
            }
        }
    }

    private void onRemove() {
        final PipelineReference selected = getView().getSelectionModel().getSelected();
        if (selected != null) {
            if (pipelineModel.getPipelineData().getAddedPipelineReferences().contains(selected)) {
                pipelineModel.getPipelineData().getAddedPipelineReferences().remove(selected);
                pipelineModel.getPipelineData().getRemovedPipelineReferences().remove(selected);

            } else {
                if (pipelineModel.getPipelineData().getRemovedPipelineReferences().contains(selected)) {
                    pipelineModel.getPipelineData().getRemovedPipelineReferences().remove(selected);
                } else {
                    pipelineModel.getPipelineData().getRemovedPipelineReferences().add(selected);
                }
            }

            setDirty(true);
            refresh();
        }
    }

    private void refresh() {
        referenceStateMap.clear();
        references.clear();
        getView().getSelectionModel().clear();

        if (currentElement != null) {
            final String id = currentElement.getId();
            if (id != null) {
                final Map<String, List<PipelineReference>> baseReferences = pipelineModel.getBaseData()
                        .getPipelineReferences().get(id);
                if (baseReferences != null) {
                    for (final List<PipelineReference> list : baseReferences.values()) {
                        for (final PipelineReference reference : list) {
                            referenceStateMap.put(reference, State.INHERITED);
                        }
                    }
                }
                for (final PipelineReference reference : pipelineModel.getPipelineData().getAddedPipelineReferences()) {
                    if (id.equals(reference.getElement())) {
                        referenceStateMap.put(reference, State.ADDED);
                    }
                }
                for (final PipelineReference reference : pipelineModel.getPipelineData()
                        .getRemovedPipelineReferences()) {
                    if (id.equals(reference.getElement())) {
                        referenceStateMap.put(reference, State.REMOVED);
                    }
                }

                references.addAll(referenceStateMap.keySet());
                Collections.sort(this.references);
            }
        }

        // See if we need to load accurate doc refs (we do this to get correct entity names for display)
        final Set<DocRef> docRefs = new HashSet<>();
        references.forEach(ref -> addPipelineReference(docRefs, ref));
        if (docRefs.size() > 0) {
            // Load entities.
            final Rest<Set<DocRef>> rest = restFactory.create();
            rest
                    .onSuccess(result -> {
                        final Map<DocRef, DocRef> fetchedDocRefs = result
                                .stream()
                                .collect(Collectors.toMap(Function.identity(), Function.identity()));

                        for (final PipelineReference reference : references) {
                            reference.setFeed(resolve(fetchedDocRefs, reference.getFeed()));
                            reference.setPipeline(resolve(fetchedDocRefs, reference.getPipeline()));
                        }

                        setData(references);
                    })
                    .call(EXPLORER_RESOURCE)
                    .fetchDocRefs(docRefs);
        } else {
            setData(references);
        }
    }

    private DocRef resolve(final Map<DocRef, DocRef> map, final DocRef docRef) {
        if (docRef == null) {
            return null;
        }

        final DocRef fetchedDocRef = map.get(docRef);
        if (fetchedDocRef != null) {
            return fetchedDocRef;
        }

        return docRef;
    }

    private void addPipelineReference(final Set<DocRef> docRefs, PipelineReference reference) {
        if (reference.getFeed() != null) {
            docRefs.add(reference.getFeed());
        }
        if (reference.getPipeline() != null) {
            docRefs.add(reference.getPipeline());
        }
    }

    private void setData(final List<PipelineReference> references) {
        getView().setRowData(0, references);
        getView().setRowCount(references.size());
        enableButtons();
    }

    protected void enableButtons() {
        addButton.setEnabled(!readOnly && propertyType != null);

        final PipelineReference selected = getView().getSelectionModel().getSelected();
        final State state = referenceStateMap.get(selected);

        editButton.setEnabled(!readOnly && State.ADDED.equals(state));
        removeButton.setEnabled(!readOnly && selected != null);

        if (readOnly) {
            addButton.setTitle("New reference disabled as pipeline is read only");
            editButton.setTitle("Edit reference disabled as pipeline is read only");
            removeButton.setTitle("Remove reference disabled as pipeline is read only");
        } else {
            addButton.setTitle("New Reference");
            editButton.setTitle("Edit Reference");
            removeButton.setTitle("Remove Reference");
        }
    }

    protected void setDirty(final boolean dirty) {
        if (dirty) {
            DirtyEvent.fire(this, dirty);
        }
    }

    @Override
    public void onReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
        enableButtons();
    }

    @Override
    public HandlerRegistration addDirtyHandler(final DirtyHandler handler) {
        return addHandlerToSource(DirtyEvent.getType(), handler);
    }

    private enum State {
        INHERITED,
        ADDED,
        REMOVED
    }
}
