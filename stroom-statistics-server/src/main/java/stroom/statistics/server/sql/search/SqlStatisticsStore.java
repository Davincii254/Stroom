package stroom.statistics.server.sql.search;

import stroom.mapreduce.v2.UnsafePairQueue;
import stroom.query.api.v2.TableSettings;
import stroom.query.v2.CompiledSorter;
import stroom.query.v2.Coprocessor;
import stroom.query.v2.CoprocessorSettingsMap;
import stroom.query.v2.Data;
import stroom.query.v2.Item;
import stroom.query.v2.Key;
import stroom.query.v2.Payload;
import stroom.query.v2.ResultStoreCreator;
import stroom.query.v2.Store;
import stroom.query.v2.TablePayload;
import stroom.query.v2.TrimSettings;

import java.util.List;
import java.util.Map;

public class SqlStatisticsStore implements Store {

    private CoprocessorSettingsMap coprocessorSettingsMap;
    private Map<CoprocessorSettingsMap.CoprocessorKey, Coprocessor> coprocessorMap;
    private Map<CoprocessorSettingsMap.CoprocessorKey, Payload> payloadMap;

    private final TableSettings tableSettings;

    public SqlStatisticsStore(final TableSettings tableSettings) {
        this.tableSettings = tableSettings;
    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public Data getData(String componentId) {
        final CoprocessorSettingsMap.CoprocessorKey coprocessorKey = coprocessorSettingsMap.getCoprocessorKey(componentId);
        if (coprocessorKey == null) {
            return null;
        }

        Payload payload = payloadMap.get(coprocessorKey);
        TablePayload tablePayload = (TablePayload) payload;
        UnsafePairQueue<Key, Item> queue = tablePayload.getQueue();

        //TODO create a resultStoreCreator (expose it as public) then call

        CompiledSorter compiledSorter = new CompiledSorter(tableSettings.getFields());
        final ResultStoreCreator resultStoreCreator = new ResultStoreCreator(compiledSorter);
        resultStoreCreator.read(queue);
//
//        // Trim the number of results in the store.
        //TODO where does defaultStoreTrimSizes come from?
        final TrimSettings trimSettings = new TrimSettings(tableSettings.getMaxResults(), null);
        resultStoreCreator.trim(trimSettings);


//        Map<Key, Items<Item>> childMap = new HashMap<>();
//        // We should now have a reduction in the reducedQueue.
//        queue.forEach(pair -> {
//            final Item item = pair.getValue();
//
//            if (item.getKey() != null) {
//                childMap.computeIfAbsent(item.getKey().getParent(), k -> new ItemsArrayList<>()).add(item);
//            } else {
//                childMap.computeIfAbsent(null, k -> new ItemsArrayList<>()).add(item);
//            }
//        });

//        return new Data(childMap, queue.size(), queue.size());
        //TODO need to check args for this method
        return resultStoreCreator.create(queue.size(), queue.size());
    }

    @Override
    public List<String> getErrors() {
        return null;
    }

    @Override
    public List<String> getHighlights() {
        return null;
    }

    public void process(CoprocessorSettingsMap coprocessorSettingsMap) {
        this.coprocessorSettingsMap = coprocessorSettingsMap;
    }

    public void coprocessorMap(Map<CoprocessorSettingsMap.CoprocessorKey, Coprocessor> coprocessorMap) {
        this.coprocessorMap = coprocessorMap;
    }

    public void payloadMap(Map<CoprocessorSettingsMap.CoprocessorKey, Payload> payloadMap) {
        this.payloadMap = payloadMap;
    }
}
