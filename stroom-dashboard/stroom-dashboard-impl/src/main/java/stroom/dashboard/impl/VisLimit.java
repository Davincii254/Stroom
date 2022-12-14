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
 */

package stroom.dashboard.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@JsonPropertyOrder({"size"})
@JsonInclude(Include.NON_NULL)
@XmlType(name = "VisLimit", propOrder = {"size"})
public class VisLimit implements Serializable {

    private static final long serialVersionUID = 1272545271946712570L;

    private Integer size;

    public VisLimit() {
    }

    public VisLimit(final Integer size) {
        this.size = size;
    }

    @XmlElement
    public Integer getSize() {
        return size;
    }

    public void setSize(final Integer size) {
        this.size = size;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final VisLimit visLimit = (VisLimit) o;

        return size != null
                ? size.equals(visLimit.size)
                : visLimit.size == null;
    }

    @Override
    public int hashCode() {
        return size != null
                ? size.hashCode()
                : 0;
    }

    @Override
    public String toString() {
        return "VisLimit{" +
                "size=" + size +
                '}';
    }
}
