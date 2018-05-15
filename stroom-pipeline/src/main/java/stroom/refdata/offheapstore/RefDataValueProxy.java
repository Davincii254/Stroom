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

import stroom.refdata.saxevents.Key;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class RefDataValueProxy {

    // held for the purpose of testing equality of a ValueProxy and
    // for calling methods on the instance
    private final RefDataStore refDataStore;
    private final ValueStoreKey valueStoreKey;
    // held so we know what type of value we are proxying for
    private final Class<?> valueClazz;

    RefDataValueProxy(final RefDataStore refDataStore, final ValueStoreKey valueStoreKey, final Class<?> valueClazz) {
        Objects.requireNonNull(refDataStore);
        this.refDataStore = Objects.requireNonNull(refDataStore);
        this.valueStoreKey = Objects.requireNonNull(valueStoreKey);
        this.valueClazz = valueClazz;
    }

    /**
     * Materialise the value that this is proxying. The useValue() method should be preferred
     * as this method will involve the added cost of copying the contents of the value.
     * @return An optional value, as the value may have been evicted from the pool. Callers
     * should expect to handle this possibility.
     */
    public Optional<RefDataValue> supplyValue() {
        return refDataStore.getValue(valueStoreKey);
    }

    <T> Optional<T> mapValue(final Function<RefDataValue, T> valueMapper) {
        return refDataStore.map(valueStoreKey, valueMapper);
    }

    <T> Optional<T> mapBytes(final Function<ByteBuffer, T> valueMapper) {
        return refDataStore.mapBytes(valueStoreKey, valueMapper);
    }

    void consumeValue(final Consumer<RefDataValue> valueConsumer) {
        refDataStore.consumeValue(valueStoreKey, valueConsumer);
    }

    void consumeBytes(final Consumer<ByteBuffer> bytesConsumer) {
        refDataStore.consumeBytes(valueStoreKey, bytesConsumer);
    }


    Class<?> getValueClazz() {
        return valueClazz;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RefDataValueProxy<?> that = (RefDataValueProxy<?>) o;
        return Objects.equals(refDataStore, that.refDataStore) &&
                Objects.equals(valueStoreKey, that.valueStoreKey) &&
                Objects.equals(valueClazz, that.valueClazz);
    }

    @Override
    public int hashCode() {

        return Objects.hash(refDataStore, valueStoreKey, valueClazz);
    }

    @Override
    public String toString() {
        return "RefDataValueProxy{" +
                "refDataStore=" + refDataStore +
                ", valueStoreKey=" + valueStoreKey +
                ", valueClazz=" + valueClazz +
                '}';
    }
}
