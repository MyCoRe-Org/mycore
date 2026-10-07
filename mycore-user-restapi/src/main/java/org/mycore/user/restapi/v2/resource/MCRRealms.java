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

package org.mycore.user.restapi.v2.resource;

import java.util.List;

import org.mycore.restapi.annotations.MCRApiDraft;
import org.mycore.user.restapi.v2.dto.MCRRealmInfo;
import org.mycore.user2.MCRRealm;
import org.mycore.user2.MCRRealmFactory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource exposing the realms a user can belong to.
 */
@MCRApiDraft("MCRUsers")
@Path("/realms")
@OpenAPIDefinition(
    tags = @Tag(name = MCRRealms.TAG_MCR_REALM, description = "Realms a user can belong to"))
public class MCRRealms {

    static final String TAG_MCR_REALM = "mcr_realm";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Lists all configured realms",
        responses = @ApiResponse(
            description = "The configured realms",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MCRRealmInfo.class)))),
        tags = TAG_MCR_REALM)
    public Response listRealms() {
        List<MCRRealmInfo> realms = MCRRealmFactory.listRealms().stream()
            .map(MCRRealms::toRealmInfo)
            .toList();
        return Response.ok(realms).build();
    }

    private static MCRRealmInfo toRealmInfo(MCRRealm realm) {
        return new MCRRealmInfo(
            realm.getID(),
            realm.getLabel(),
            realm.getLoginURL(),
            realm.getPasswordChangeURL(),
            realm.getCreateURL()
        );
    }

}
