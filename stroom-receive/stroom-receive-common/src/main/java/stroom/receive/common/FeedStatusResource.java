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
 */

package stroom.receive.common;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import stroom.proxy.feed.remote.GetFeedStatusRequest;
import stroom.proxy.feed.remote.GetFeedStatusResponse;
import stroom.util.RestResource;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(value = "feedStatus - /v1")
@Path("/feedStatus/v1")
@Produces(MediaType.APPLICATION_JSON)
public class FeedStatusResource implements RestResource {
    private final FeedStatusService feedStatusService;

    @Inject
    public FeedStatusResource(final FeedStatusService feedStatusService) {
        this.feedStatusService = feedStatusService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getFeedStatus")
    @Timed
    @ApiOperation(
            value = "Submit a request to get the status of a feed",
            response = GetFeedStatusResponse.class)
    public GetFeedStatusResponse getFeedStatus(@ApiParam("GetFeedStatusRequest") final GetFeedStatusRequest request) {
        return feedStatusService.getFeedStatus(request);
    }
}