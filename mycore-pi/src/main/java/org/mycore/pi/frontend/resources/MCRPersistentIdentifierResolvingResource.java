/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mycore.pi.MCRPersistentIdentifierManager;

import com.google.gson.Gson;

@Path("pi/resolve")
public class MCRPersistentIdentifierResolvingResource {

    @GET
    @Path("{identifier:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resolve(@PathParam("identifier") String identifier) {
        HashMap<String, List<String>> resolveMap = new HashMap<>();
        MCRPersistentIdentifierManager.getInstance().getResolvers().forEach(resolver -> MCRPersistentIdentifierManager
            .getInstance().get(identifier)
            .forEach(mcrPersistentIdentifier -> resolveMap.put(resolver.getName(),
                resolver.resolveSuppress(mcrPersistentIdentifier).collect(Collectors.toList()))));
        return Response.ok().entity(new Gson().toJson(resolveMap)).build();
    }
}
