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

package stroom.widget.tab.client.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;

public class BasicTabSelector extends AbstractTabSelector {

    private final Element text;
    private final Element element;

    public BasicTabSelector() {
        final Element arrows = DOM.createDiv();
        arrows.setClassName("basicTabSelector-arrows");

        text = DOM.createDiv();
        text.setClassName("basicTabSelector-text");

        element = DOM.createDiv();
        element.setClassName("basicTabSelector");

        element.appendChild(arrows);
        element.appendChild(text);

        setElement(element);
    }

    @Override
    public void setText(final String text) {
        this.text.setInnerText(text);
    }
}
