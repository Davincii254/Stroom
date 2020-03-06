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

package stroom.pipeline.shared.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Pipeline", propOrder = {"elements", "properties", "pipelineReferences", "links"})
@XmlRootElement(name = "pipeline")
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder({"elements", "properties", "pipelineReferences", "links"})
public class PipelineData {
    @XmlElement(name = "elements")
    @JsonProperty
    private PipelineElements elements = new PipelineElements();
    @XmlElement(name = "properties")
    @JsonProperty
    private PipelineProperties properties = new PipelineProperties();
    @XmlElement(name = "pipelineReferences")
    @JsonProperty
    private PipelineReferences pipelineReferences = new PipelineReferences();
    @XmlElement(name = "links")
    @JsonProperty
    private PipelineLinks links = new PipelineLinks();

    public PipelineData() {
    }

    @JsonCreator
    public PipelineData(@JsonProperty("elements") final PipelineElements elements,
                        @JsonProperty("properties") final PipelineProperties properties,
                        @JsonProperty("pipelineReferences") final PipelineReferences pipelineReferences,
                        @JsonProperty("links") final PipelineLinks links) {
        this.elements = elements;
        this.properties = properties;
        this.pipelineReferences = pipelineReferences;
        this.links = links;
    }

    public PipelineElements getElements() {
        return elements;
    }

    public void setElements(final PipelineElements elements) {
        this.elements = elements;
    }

    public PipelineProperties getProperties() {
        return properties;
    }

    public void setProperties(final PipelineProperties properties) {
        this.properties = properties;
    }

    public PipelineReferences getPipelineReferences() {
        return pipelineReferences;
    }

    public void setPipelineReferences(final PipelineReferences pipelineReferences) {
        this.pipelineReferences = pipelineReferences;
    }

    public PipelineLinks getLinks() {
        return links;
    }

    public void setLinks(final PipelineLinks links) {
        this.links = links;
    }

    // Used for testing....
    public void addElement(final PipelineElementType elementType, final String id) {
        final PipelineElement element = new PipelineElement();
        element.setId(id);
        element.setType(elementType.getType());
        element.setElementType(elementType);
        addElement(element);
    }

    public void addElement(final PipelineElement element) {
        getAddedElements().add(element);
    }

    public void removeElement(final PipelineElementType elementType, final String id) {
        final PipelineElement element = new PipelineElement();
        element.setId(id);
        element.setType(elementType.getType());
        element.setElementType(elementType);
        removeElement(element);
    }

    public void removeElement(final PipelineElement element) {
        getRemovedElements().add(element);
    }

    public void addProperty(final String element, final PipelinePropertyType propertyType, final boolean value) {
        final PipelineProperty property = new PipelineProperty(element, propertyType.getName());
        property.setPropertyType(propertyType);
        final PipelinePropertyValue val = new PipelinePropertyValue(value);
        property.setValue(val);
        addProperty(property);
    }

    public void addProperty(final PipelineProperty property) {
        getAddedProperties().add(property);
    }

    public void removeProperty(final String element, final PipelinePropertyType propertyType) {
        final PipelineProperty property = new PipelineProperty(element, propertyType.getName());
        property.setPropertyType(propertyType);
        removeProperty(property);
    }

    public void removeProperty(final PipelineProperty property) {
        getRemovedProperties().add(property);
    }

    public void addPipelineReference(final PipelineReference pipelineReference) {
        getAddedPipelineReferences().add(pipelineReference);
    }

    public void removePipelineReference(final PipelineReference pipelineReference) {
        getRemovedPipelineReferences().add(pipelineReference);
    }

    public void addLink(final String from, final String to) {
        final PipelineLink link = new PipelineLink(from, to);
        addLink(link);
    }

    public void addLink(final PipelineElement from, final PipelineElement to) {
        final PipelineLink link = new PipelineLink(from.getId(), to.getId());
        addLink(link);
    }

    public void addLink(final PipelineLink link) {
        getAddedLinks().add(link);
    }

    public void removeLink(final String from, final String to) {
        final PipelineLink link = new PipelineLink(from, to);
        removeLink(link);
    }

    public void removeLink(final PipelineLink link) {
        getRemovedLinks().add(link);
    }

    /**
     * Convenience methods
     */
    @JsonIgnore
    public List<PipelineElement> getAddedElements() {
        return elements.getAdd();
    }

    @JsonIgnore
    public List<PipelineElement> getRemovedElements() {
        return elements.getRemove();
    }

    @JsonIgnore
    public List<PipelineProperty> getAddedProperties() {
        return properties.getAdd();
    }

    @JsonIgnore
    public List<PipelineProperty> getRemovedProperties() {
        return properties.getRemove();
    }

    @JsonIgnore
    public List<PipelineReference> getAddedPipelineReferences() {
        return pipelineReferences.getAdd();
    }

    @JsonIgnore
    public List<PipelineReference> getRemovedPipelineReferences() {
        return pipelineReferences.getRemove();
    }

    @JsonIgnore
    public List<PipelineLink> getAddedLinks() {
        return links.getAdd();
    }

    @JsonIgnore
    public List<PipelineLink> getRemovedLinks() {
        return links.getRemove();
    }
}
