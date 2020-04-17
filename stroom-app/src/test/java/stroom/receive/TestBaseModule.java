package stroom.receive;

import com.google.inject.AbstractModule;
import com.google.inject.util.Providers;
import stroom.cache.impl.CacheModule;
import stroom.collection.mock.MockCollectionModule;
import stroom.core.receive.ReceiveDataModule;
import stroom.data.store.mock.MockStreamStoreModule;
import stroom.dictionary.impl.DictionaryModule;
import stroom.docstore.impl.DocStoreModule;
import stroom.docstore.impl.memory.MemoryPersistenceModule;
import stroom.event.logging.api.DocumentEventLog;
import stroom.feed.impl.FeedModule;
import stroom.meta.mock.MockMetaModule;
import stroom.meta.statistics.impl.MockMetaStatisticsModule;
import stroom.receive.rules.impl.ReceiveDataRuleSetModule;
import stroom.security.mock.MockSecurityContextModule;
import stroom.task.impl.TaskContextModule;
import stroom.util.pipeline.scope.PipelineScopeModule;

public class TestBaseModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new CacheModule());
        install(new DictionaryModule());
        install(new DocStoreModule());
        install(new FeedModule());
        install(new MemoryPersistenceModule());
        install(new MockMetaModule());
        install(new MockMetaStatisticsModule());
        install(new MockSecurityContextModule());
        install(new MockStreamStoreModule());
        install(new PipelineScopeModule());
        install(new ReceiveDataModule());
        install(new ReceiveDataRuleSetModule());
        install(new MockCollectionModule());
        install(new TaskContextModule());

        bind(DocumentEventLog.class).toProvider(Providers.of(null));
    }
}
