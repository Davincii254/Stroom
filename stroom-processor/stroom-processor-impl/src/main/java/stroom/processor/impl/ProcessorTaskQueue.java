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

package stroom.processor.impl;

import stroom.processor.shared.ProcessorTask;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProcessorTaskQueue {

    private final LinkedBlockingQueue<ProcessorTask> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean filling = new AtomicBoolean();

    public ProcessorTask poll() {
        return queue.poll();
    }

    public void add(final ProcessorTask streamTask) {
        queue.add(streamTask);
    }

    public boolean addAll(final Collection<? extends ProcessorTask> streamTasks) {
        return queue.addAll(streamTasks);
    }

    public int size() {
        return queue.size();
    }

    public boolean compareAndSetFilling(final boolean expect, final boolean update) {
        return this.filling.compareAndSet(expect, update);
    }

    public boolean isFilling() {
        return filling.get();
    }

    public void setFilling(final boolean update) {
        this.filling.set(update);
    }
}
