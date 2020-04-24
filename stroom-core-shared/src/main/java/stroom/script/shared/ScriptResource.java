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

package stroom.script.shared;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.fusesource.restygwt.client.DirectRestService;
import stroom.docref.DocRef;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Api(value = "script - /v1")
@Path("/script" + ResourcePaths.V1)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ScriptResource extends RestResource, DirectRestService {
    @POST
    @Path("/read")
    @ApiOperation(
            value = "Get a script doc",
            response = ScriptDoc.class)
    ScriptDoc read(DocRef docRef);

    @PUT
    @Path("/update")
    @ApiOperation(
            value = "Update a script doc",
            response = ScriptDoc.class)
    ScriptDoc update(ScriptDoc xslt);

    @POST
    @Path("/fetchLinkedScripts")
    @ApiOperation(
            value = "Fetch related scripts",
            response = ScriptDoc.class)
    List<ScriptDoc> fetchLinkedScripts(FetchLinkedScriptRequest request);
}