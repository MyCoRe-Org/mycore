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

package org.mycore.dedup.resources;

import java.util.List;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.dedup.MCRDeDupKeyManager;
import org.mycore.dedup.MCRPossibleDuplicate;

import com.google.gson.Gson;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource exposing the deduplication data, served as part of the standalone {@link MCRDeDupApp}
 * under {@code /api/dedup}. All endpoints require the {@value #PERMISSION} permission.
 * <ul>
 *   <li>{@code GET    /api/dedup/duplicates} &ndash; all possible duplicate pairs</li>
 *   <li>{@code GET    /api/dedup/duplicates/{id}} &ndash; possible duplicates of one object</li>
 *   <li>{@code GET    /api/dedup/no-duplicates} &ndash; all no-duplicate markings</li>
 *   <li>{@code POST   /api/dedup/no-duplicates?id1=..&amp;id2=..} &ndash; mark a pair as no duplicates</li>
 *   <li>{@code DELETE /api/dedup/no-duplicates/{markingId}} &ndash; remove a no-duplicate marking</li>
 * </ul>
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class MCRDeDupResource {

    /** Permission required to use the deduplication API. */
    public static final String PERMISSION = "manage-deduplication";

    private static final Gson GSON = new Gson();

    @GET
    @Path("duplicates")
    public Response listAllDuplicates() {
        checkAccess();
        List<MCRPossibleDuplicate> duplicates = MCRDeDupKeyManager.obtainInstance().findAllDuplicates();
        return Response.ok(GSON.toJson(duplicates)).build();
    }

    @GET
    @Path("duplicates/{id}")
    public Response listDuplicates(@PathParam("id") String id) {
        checkAccess();
        List<MCRPossibleDuplicate> duplicates = MCRDeDupKeyManager.obtainInstance().findDuplicates(parseId(id));
        return Response.ok(GSON.toJson(duplicates)).build();
    }

    @GET
    @Path("no-duplicates")
    public Response listNoDuplicates() {
        checkAccess();
        List<MCRNoDuplicateDto> markings = MCRDeDupKeyManager.obtainInstance().listNoDuplicates().stream()
            .map(MCRNoDuplicateDto::of)
            .toList();
        return Response.ok(GSON.toJson(markings)).build();
    }

    @POST
    @Path("no-duplicates")
    public Response addNoDuplicate(@QueryParam("id1") String id1, @QueryParam("id2") String id2) {
        checkAccess();
        MCRObjectID objectId1 = parseId(id1);
        MCRObjectID objectId2 = parseId(id2);
        if (objectId1.equals(objectId2)) {
            throw new BadRequestException("id1 and id2 must denote different objects");
        }
        String creator = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        MCRDeDupKeyManager.obtainInstance().addNoDuplicate(objectId1, objectId2, creator);
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("no-duplicates/{markingId}")
    public Response removeNoDuplicate(@PathParam("markingId") long markingId) {
        checkAccess();
        MCRDeDupKeyManager.obtainInstance().removeNoDuplicate(markingId);
        return Response.noContent().build();
    }

    private static void checkAccess() {
        if (!MCRAccessManager.checkPermission(PERMISSION)) {
            throw new ForbiddenException("Missing permission '" + PERMISSION + "' to use the deduplication API");
        }
    }

    private static MCRObjectID parseId(String id) {
        if (id == null || !MCRObjectID.isValid(id)) {
            throw new BadRequestException("Invalid object id: " + id);
        }
        return MCRObjectID.getInstance(id);
    }
}
