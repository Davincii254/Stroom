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
 *
 */

package stroom.search.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import stroom.cluster.server.ClusterCallService;
import stroom.entity.shared.Sort.Direction;
import stroom.index.server.IndexService;
import stroom.index.server.IndexShardService;
import stroom.index.shared.FindIndexShardCriteria;
import stroom.index.shared.Index;
import stroom.index.shared.IndexField;
import stroom.index.shared.IndexShard;
import stroom.index.shared.IndexShard.IndexShardStatus;
import stroom.node.shared.Node;
import stroom.query.api.v2.Query;
import stroom.query.api.v2.QueryKey;
import stroom.search.resultsender.NodeResult;
import stroom.security.SecurityContext;
import stroom.security.SecurityHelper;
import stroom.security.shared.UserIdentity;
import stroom.task.cluster.ClusterDispatchAsyncHelper;
import stroom.task.cluster.ClusterResultCollectorCache;
import stroom.task.cluster.TargetNodeSetFactory;
import stroom.task.cluster.TargetNodeSetFactory.TargetType;
import stroom.task.cluster.TerminateTaskClusterTask;
import stroom.task.server.AbstractTaskHandler;
import stroom.task.server.GenericServerTask;
import stroom.task.server.TaskHandlerBean;
import stroom.task.server.TaskManager;
import stroom.task.shared.FindTaskCriteria;
import stroom.util.shared.TaskId;
import stroom.util.shared.VoidResult;
import stroom.util.spring.StroomScope;
import stroom.util.task.TaskMonitor;
import stroom.util.thread.ThreadUtil;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@TaskHandlerBean(task = AsyncSearchTask.class)
@Scope(value = StroomScope.TASK)
class AsyncSearchTaskHandler extends AbstractTaskHandler<AsyncSearchTask, VoidResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncSearchTaskHandler.class);

    private final TaskMonitor taskMonitor;
    private final TargetNodeSetFactory targetNodeSetFactory;
    private final ClusterDispatchAsyncHelper dispatchHelper;
    private final ClusterCallService clusterCallService;
    private final ClusterResultCollectorCache clusterResultCollectorCache;
    private final IndexService indexService;
    private final IndexShardService indexShardService;
    private final TaskManager taskManager;
    private final SecurityContext securityContext;

    @Inject
    AsyncSearchTaskHandler(final TaskMonitor taskMonitor, final TargetNodeSetFactory targetNodeSetFactory,
                           final ClusterDispatchAsyncHelper dispatchHelper,
                           @Named("clusterCallServiceRemote") final ClusterCallService clusterCallService,
                           final ClusterResultCollectorCache clusterResultCollectorCache, final IndexService indexService,
                           final IndexShardService indexShardService, final TaskManager taskManager, final SecurityContext securityContext) {
        this.taskMonitor = taskMonitor;
        this.targetNodeSetFactory = targetNodeSetFactory;
        this.dispatchHelper = dispatchHelper;
        this.clusterCallService = clusterCallService;
        this.clusterResultCollectorCache = clusterResultCollectorCache;
        this.indexService = indexService;
        this.indexShardService = indexShardService;
        this.taskManager = taskManager;
        this.securityContext = securityContext;
    }

    @Override
    public VoidResult exec(final AsyncSearchTask task) {
        try (final SecurityHelper securityHelper = SecurityHelper.elevate(securityContext)) {
            final ClusterSearchResultCollector resultCollector = task.getResultCollector();
            if (!task.isTerminated()) {
                final Node sourceNode = targetNodeSetFactory.getSourceNode();
                final Map<Node, List<Long>> shardMap = new HashMap<>();

                try {
                    final Set<Node> remainingNodes = Collections.newSetFromMap(new ConcurrentHashMap<>());

                    // Get the nodes that we are going to send the search request
                    // to.
                    final Set<Node> targetNodes = targetNodeSetFactory.getEnabledActiveTargetNodeSet();
                    taskMonitor.info(task.getSearchName() + " - initialising");
                    final Query query = task.getQuery();

                    // Reload the index.
                    final Index index = indexService.loadByUuid(query.getDataSource().getUuid());

                    // Get an array of stored index fields that will be used for
                    // getting stored data.
                    // TODO : Specify stored fields based on the fields that all
                    // coprocessors will require. Also
                    // batch search only needs stream and event id stored fields.
                    final String[] storedFields = getStoredFields(index);

                    // Get a list of search index shards to look through.
                    final FindIndexShardCriteria findIndexShardCriteria = new FindIndexShardCriteria();
                    findIndexShardCriteria.getIndexSet().add(query.getDataSource());
                    // Only non deleted indexes.
                    findIndexShardCriteria.getIndexShardStatusSet().addAll(IndexShard.NON_DELETED_INDEX_SHARD_STATUS);
                    // Order by partition name and key.
                    findIndexShardCriteria.addSort(FindIndexShardCriteria.FIELD_PARTITION, Direction.DESCENDING, false);
                    findIndexShardCriteria.addSort(FindIndexShardCriteria.FIELD_ID, Direction.DESCENDING, false);
                    findIndexShardCriteria.getFetchSet().add(Node.ENTITY_TYPE);
                    final List<IndexShard> indexShards = indexShardService.find(findIndexShardCriteria);

                    // Build a map of nodes that will deal with each set of shards.
                    for (final IndexShard indexShard : indexShards) {
                        if (IndexShardStatus.CORRUPT.equals(indexShard.getStatus())) {
                            resultCollector.getErrorSet(indexShard.getNode()).add(
                                    "Attempt to search an index shard marked as corrupt: id=" + indexShard.getId() + ".");
                        } else {
                            final Node node = indexShard.getNode();
                            shardMap.computeIfAbsent(node, k -> new ArrayList<>()).add(indexShard.getId());
                        }
                    }

                    // Tell the result collector which nodes we expect to get results from.
                    resultCollector.setExpectedNodes(shardMap.keySet());

                    // Start remote cluster search execution.
                    for (final Entry<Node, List<Long>> entry : shardMap.entrySet()) {
                        final Node node = entry.getKey();
                        final List<Long> shards = entry.getValue();

                        if (targetNodes.contains(node)) {
                            final ClusterSearchTask clusterSearchTask = new ClusterSearchTask(
                                    securityContext.getUserIdentity(),
                                    "Cluster Search",
                                    task.getKey(),
                                    query,
                                    shards,
                                    storedFields,
                                    task.getCoprocessorMap(),
                                    task.getDateTimeLocale(),
                                    task.getNow());
                            LOGGER.debug("Dispatching clusterSearchTask to node {}", node);
//                            dispatcher.execAsync(clusterSearchTask, resultCollector, sourceNode,
//                                    Collections.singleton(node));


                            try {
                                final boolean success = (Boolean) clusterCallService.call(sourceNode, node, RemoteSearchManager.BEAN_NAME,
                                        RemoteSearchManager.START_SEARCH,
                                        new Class[]{UserIdentity.class, TaskId.class, ClusterSearchTask.class},
                                        new Object[]{securityContext.getUserIdentity(), task.getId(), clusterSearchTask});
                                if (!success) {
                                    resultCollector.onFailure(node, new SearchException("Failed to start remote search"));
                                } else {
                                    remainingNodes.add(node);
                                }

                            } catch (final Throwable e) {
//                                throw EntityServiceExceptionUtil.create(e);

                                resultCollector.onFailure(node, new SearchException(e.getMessage(), e));
                            }


                        } else {
                            resultCollector.onFailure(node,
                                    new SearchException("Node is not enabled or active. Some search results may be missing."));
                        }
                    }
                    taskMonitor.info(task.getSearchName() + " - searching...");

                    // Await completion.
                    while (!task.isTerminated() &&
                            remainingNodes.size() > 0) {

                        final long start = System.currentTimeMillis();

                        for (Node node : remainingNodes) {
                            final NodeResult nodeResult = (NodeResult) clusterCallService.call(sourceNode, node, RemoteSearchManager.BEAN_NAME,
                                    RemoteSearchManager.POLL, new Class[]{UserIdentity.class, QueryKey.class}, new Object[]{securityContext.getUserIdentity(), task.getKey()});
                            if (nodeResult != null) {
                                resultCollector.onSuccess(node, nodeResult);
                                if (nodeResult.isComplete()) {
                                    remainingNodes.remove(node);
                                }
                            }
                        }

                        final long elapsed = System.currentTimeMillis() - start;
                        if (elapsed < 1000) {
                            ThreadUtil.sleep(1000 - elapsed);
                        }

//                        boolean awaitResult = LAMBDA_LOGGER.logDurationIfTraceEnabled(
//                                () -> {
//                                    try {
//                                        // block and wait for up to 10s for our search to be completed/terminated
//                                        return resultCollector.awaitCompletion(10, TimeUnit.SECONDS);
//                                    } catch (InterruptedException e) {
//                                        //Don't reset the interrupt status as we are at the top level of
//                                        //the task execution
//                                        throw new RuntimeException("Thread interrupted");
//                                    }
//                                },
//                                "waiting for completion condition");
//
//                        LOGGER.trace("await finished with result {}", awaitResult);
//
                        // If the collector is no longer in the cache then terminate
                        // this search task.
                        if (clusterResultCollectorCache.get(resultCollector.getId()) == null) {
                            terminateTasks(task);
                        }
                    }

                    // Perform a final wait for the result collector to do final merges.
                    resultCollector.waitForPendingWork();
                    taskMonitor.info(task.getSearchName() + " - complete");

                } catch (final Exception e) {
                    resultCollector.getErrorSet(sourceNode).add(e.getMessage());
                } finally {
                    // Destroy remote search results.
                    for (final Node node : shardMap.keySet()) {
                        try {
                            final boolean success = (Boolean) clusterCallService.call(sourceNode, node, RemoteSearchManager.BEAN_NAME,
                                    RemoteSearchManager.DESTROY, new Class[]{UserIdentity.class, QueryKey.class}, new Object[]{securityContext.getUserIdentity(), task.getKey()});

                        } catch (final Throwable e) {
                            resultCollector.onFailure(node, new SearchException(e.getMessage(), e));
                        }
                    }

                    // Make sure we try and terminate any child tasks on worker
                    // nodes if we need to.
                    terminateTasks(task);

                    // Ensure search is complete even if we had errors.
                    resultCollector.complete();

                    // We need to wait here for the client to keep getting results if
                    // this is an interactive search.
                    taskMonitor.info(task.getSearchName() + " - staying alive for UI requests");
                }
            }

            return VoidResult.INSTANCE;
        }
    }

    private void terminateTasks(final AsyncSearchTask task) {
        // Terminate this task.
        task.terminate();

        // We have to wrap the cluster termination task in another task or
        // ClusterDispatchAsyncImpl
        // will not execute it if the parent task is terminated.
        final GenericServerTask outerTask = GenericServerTask.create(null, task.getUserIdentity(), "Terminate: " + task.getTaskName(), "Terminating cluster tasks");
        outerTask.setRunnable(() -> {
            taskMonitor.info(task.getSearchName() + " - terminating child tasks");
            final FindTaskCriteria findTaskCriteria = new FindTaskCriteria();
            findTaskCriteria.addAncestorId(task.getId());
            final TerminateTaskClusterTask terminateTask = new TerminateTaskClusterTask(task.getUserIdentity(), "Terminate: " + task.getTaskName(), findTaskCriteria, false);

            // Terminate matching tasks.
            dispatchHelper.execAsync(terminateTask, TargetType.ACTIVE);
        });
        taskManager.execAsync(outerTask);
    }

    private String[] getStoredFields(final Index index) {
        final List<IndexField> indexFields = index.getIndexFieldsObject().getIndexFields();
        return indexFields
                .stream()
                .filter(IndexField::isStored)
                .map(IndexField::getFieldName)
                .toArray(String[]::new);
    }
}
