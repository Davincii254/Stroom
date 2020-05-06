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

package stroom.statistics.impl.hbase.entity;

import stroom.docstore.api.DocumentActionHandlerBinder;
import stroom.explorer.api.ExplorerActionHandlerBinder;
import stroom.importexport.api.ImportExportActionHandler;
import stroom.statistics.impl.hbase.shared.StroomStatsStoreDoc;
import stroom.util.guice.RestResourcesBinder;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class StroomStatsStoreModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StroomStatsStoreStore.class).to(StroomStatsStoreStoreImpl.class);

        ExplorerActionHandlerBinder.create(binder())
                .bind(StroomStatsStoreStoreImpl.class);

        final Multibinder<ImportExportActionHandler> importExportActionHandlerBinder = Multibinder.newSetBinder(binder(), ImportExportActionHandler.class);
        importExportActionHandlerBinder.addBinding().to(StroomStatsStoreStoreImpl.class);

        DocumentActionHandlerBinder.create(binder())
                .bind(StroomStatsStoreDoc.DOCUMENT_TYPE, StroomStatsStoreStoreImpl.class);

        RestResourcesBinder.create(binder())
                .bind(StatsStoreResourceImpl.class);
    }
}