/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.pi.frontend.resources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mycore.pi.MCRPIManager;

import com.google.gson.Gson;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("pi/resolve")
public class MCRPersistentIdentifierResolvingResource {

    @GET
    @Path("{identifier:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resolve(@PathParam("identifier") String identifier) {
        Map<String, List<String>> resolveMap = new HashMap<>();
        MCRPIManager.getInstance().getResolvers().forEach(resolver -> MCRPIManager
            .getInstance().get(identifier)
            .forEach(mcrPersistentIdentifier -> resolveMap.put(resolver.getName(),
                resolver.resolveSuppress(mcrPersistentIdentifier).collect(Collectors.toList()))));
        return Response.ok().entity(new Gson().toJson(resolveMap)).build();
    }
}
