/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.query.common.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.mapreduce.v2.PairQueue;
import stroom.mapreduce.v2.UnsafePairQueue;
import stroom.query.api.v2.Field;
import stroom.query.util.LambdaLogger;
import stroom.query.util.LambdaLoggerFactory;
import stroom.util.shared.HasTerminate;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class TablePayloadHandler implements PayloadHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TablePayloadHandler.class);
    private static final LambdaLogger LAMBDA_LOGGER = LambdaLoggerFactory.getLogger(TablePayloadHandler.class);

    private final CompiledSorter compiledSorter;
    private final CompiledDepths compiledDepths;
    private final Sizes maxResults;
    private final Sizes storeSize;
    private final AtomicLong totalResults = new AtomicLong();

    private volatile PairQueue<GroupKey, Item> currentQueue;
    private volatile Data data;
    private volatile boolean hasEnoughData;

    public TablePayloadHandler(final List<Field> fields,
                               final boolean showDetails,
                               final Sizes maxResults,
                               final Sizes storeSize) {
        this.compiledSorter = new CompiledSorter(fields);
        this.maxResults = maxResults;
        this.storeSize = storeSize;
        this.compiledDepths = new CompiledDepths(fields, showDetails);
        this.data = new ResultStoreCreator(compiledSorter).create(0, 0);
    }

    void clear() {
        totalResults.set(0);
        currentQueue = null;
        data = new ResultStoreCreator(compiledSorter).create(0, 0);
    }

    void addQueue(final UnsafePairQueue<GroupKey, Item> newQueue, final HasTerminate hasTerminate) {
        LAMBDA_LOGGER.trace(() -> LambdaLogger.buildMessage("addQueue called for {} items", newQueue.size()));
        if (newQueue != null) {
            if (!Thread.currentThread().isInterrupted() && !hasTerminate.isTerminated() && !hasEnoughData) {
                // Add the new queue to the pending merge queue ready for
                // merging.
                try {
                    mergeQueue(newQueue);
                } catch (final RuntimeException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }

        LAMBDA_LOGGER.trace(() -> "Finished adding items to the queue");
    }

    private void mergeQueue(final UnsafePairQueue<GroupKey, Item> newQueue) {
        /*
         * Update the total number of results that we have received.
         */
        totalResults.getAndAdd(newQueue.size());

        final PairQueue<GroupKey, Item> outputQueue = new UnsafePairQueue<>();

        /*
         * Create a partitioner to perform result reduction if needed.
         */
        final ItemPartitioner partitioner = new ItemPartitioner(compiledDepths.getDepths(),
                compiledDepths.getMaxDepth());
        partitioner.setOutputCollector(outputQueue);

        if (currentQueue != null) {
            /* First deal with the current queue. */
            partitioner.read(currentQueue);
        }

        /* New deal with the new queue. */
        partitioner.read(newQueue);

        /* Perform partitioning and reduction. */
        partitioner.partition();

        currentQueue = updateResultStore(outputQueue);
    }

    private PairQueue<GroupKey, Item> updateResultStore(final PairQueue<GroupKey, Item> queue) {
        // Stick the new reduced results into a new result store.
        final ResultStoreCreator resultStoreCreator = new ResultStoreCreator(compiledSorter);
        resultStoreCreator.read(queue);

        // Trim the number of results in the store.
        resultStoreCreator.sortAndTrim(storeSize);

        // Put the remaining items into the current queue ready for the next
        // result.
        final PairQueue<GroupKey, Item> remaining = new UnsafePairQueue<>();
        long size = 0;
        for (final Items<Item> items : resultStoreCreator.getChildMap().values()) {
            for (final Item item : items) {
                remaining.collect(item.key, item);
                size++;
            }
        }

        // Update the result store reference to point at this new store.
        this.data = resultStoreCreator.create(size, totalResults.get());

        // Some searches can be terminated early if the user is not sorting or grouping.
        if (!hasEnoughData && !compiledSorter.hasSort() && !compiledDepths.hasGroupBy()) {
            // No sorting or grouping so we can stop the search as soon as we have the number of results requested by
            // the client
            if (maxResults != null && data.getTotalSize() >= maxResults.size(0)) {
                hasEnoughData = true;
            }
        }

        // Give back the remaining queue items ready for the next result.
        return remaining;
    }

    public Data getData() {
        return data;
    }
}
