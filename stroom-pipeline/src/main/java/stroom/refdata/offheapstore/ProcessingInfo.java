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
 *
 */

package stroom.refdata.offheapstore;

import stroom.util.logging.LambdaLogger;

import java.util.Objects;

public class ProcessingInfo {

    private final long createTimeEpochMs;
    private final long lastAccessedTimeEpochMs;
    private final long effectiveTimeEpochMs;
    private final ProcessingState processingState;

    ProcessingInfo(final long createTimeEpochMs,
                   final long lastAccessedTimeEpochMs,
                   final long effectiveTimeEpochMs,
                   final ProcessingState processingState) {
        this.createTimeEpochMs = createTimeEpochMs;
        this.lastAccessedTimeEpochMs = lastAccessedTimeEpochMs;
        this.effectiveTimeEpochMs = effectiveTimeEpochMs;
        this.processingState = processingState;
    }

    ProcessingInfo updateState(final ProcessingState newProcessingState) {
        return new ProcessingInfo(createTimeEpochMs, lastAccessedTimeEpochMs, effectiveTimeEpochMs, newProcessingState);
    }

    ProcessingInfo updateLastAccessedTime() {
        return new ProcessingInfo(createTimeEpochMs, System.currentTimeMillis(), effectiveTimeEpochMs, processingState);
    }

    long getCreateTimeEpochMs() {
        return createTimeEpochMs;
    }

    long getEffectiveTimeEpochMs() {
        return effectiveTimeEpochMs;
    }

    ProcessingState getProcessingState() {
        return processingState;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ProcessingInfo that = (ProcessingInfo) o;
        return createTimeEpochMs == that.createTimeEpochMs &&
                effectiveTimeEpochMs == that.effectiveTimeEpochMs &&
                processingState == that.processingState;
    }

    @Override
    public int hashCode() {

        return Objects.hash(createTimeEpochMs, effectiveTimeEpochMs, processingState);
    }

    @Override
    public String toString() {
        return "ProcessingInfo{" +
                "createTimeEpochMs=" + createTimeEpochMs +
                ", effectiveTimeEpochMs=" + effectiveTimeEpochMs +
                ", processingState=" + processingState +
                '}';
    }

    public enum ProcessingState {

        IN_PROGRESS((byte)0),
        COMPLETE((byte)1);

        private final byte id;

        ProcessingState(final byte id) {
            this.id = id;
        }

        byte getId() {
            return id;
        }

        static ProcessingState valueOf(final byte id) {
            for (ProcessingState processingState : ProcessingState.values()) {
                if (id == processingState.id) {
                    return processingState;
                }
            }
            throw new RuntimeException(
                    LambdaLogger.buildMessage("Unable to find ProcessingState for id {}", Byte.toString(id)));
        }
    }

}
