/*
 * Copyright 2018 Crown Copyright
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

package stroom.node.impl;

import stroom.node.shared.FindNodeCriteria;
import stroom.node.shared.Node;
import stroom.util.shared.ResultList;

public interface NodeDao {
    Node create(Node node);

    Node update(Node node);

    ResultList<Node> find(FindNodeCriteria criteria);

    Node getNode(String nodeName);
}
