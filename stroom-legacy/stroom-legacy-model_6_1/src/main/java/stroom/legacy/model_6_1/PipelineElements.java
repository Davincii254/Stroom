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

package stroom.legacy.model_6_1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Elements", propOrder = {"add", "remove"})
@Deprecated
public class PipelineElements implements SharedObject {
    private static final long serialVersionUID = -2207621085023679751L;

    @XmlElementWrapper(name = "add", required = false)
    @XmlElement(name = "element", required = false)
    private List<PipelineElement> add = new ArrayList<>();

    @XmlElementWrapper(name = "remove", required = false)
    @XmlElement(name = "element", required = false)
    private List<PipelineElement> remove = new ArrayList<>();

    public List<PipelineElement> getAdd() {
        return add;
    }

    public List<PipelineElement> getRemove() {
        return remove;
    }
}
