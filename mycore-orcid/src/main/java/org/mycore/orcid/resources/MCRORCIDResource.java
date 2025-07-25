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

package org.mycore.orcid.resources;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.MCRJSONManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.orcid.MCRORCIDProfile;
import org.mycore.orcid.user.MCRORCIDSession;
import org.mycore.orcid.user.MCRORCIDUser;
import org.mycore.orcid.user.MCRPublicationStatus;
import org.mycore.orcid.user.MCRUserStatus;
import org.mycore.orcid.works.MCRWorksSection;
import org.xml.sax.SAXException;

import com.google.gson.Gson;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

@Path("orcid")
public class MCRORCIDResource {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Returns the ORCID status of the current user as JSON, e.g.
     *
     * {
     *   "orcid": "0000-0001-5484-889X",
     *   "isORCIDUser": true,
     *   "weAreTrustedParty": true
     * }
     *
     * @see org.mycore.orcid.user.MCRUserStatus
     */
    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    public String getUserStatus() {
        MCRORCIDUser user = MCRORCIDSession.getCurrentUser();
        Gson gson = MCRJSONManager.obtainInstance().createGson();
        return gson.toJson(user.getStatus());
    }

    /**
     * Returns the publication status for a given MCRObjectID in the current user's ORCID profile, e.g.
     *
     * {
     *   "user": {
     *     "orcid": "0000-0001-5484-889X",
     *     "isORCIDUser": true,
     *     "weAreTrustedParty": true
     *   },
     *   "objectID": "mir_mods_00088905",
     *   "isUsersPublication": true,
     *   "isInORCIDProfile": true
     * }
     *
     * @see org.mycore.orcid.user.MCRPublicationStatus
     */
    @GET
    @Path("status/{objectID}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPublicationStatus(@PathParam("objectID") String objectID)
        throws JDOMException, IOException, SAXException {
        MCRObjectID oid = checkID(objectID);
        MCRORCIDUser user = MCRORCIDSession.getCurrentUser();
        return publicationStatus(oid, user);
    }

    /**
     * Publishes a work in the current user's ORCID profile, or
     * updates an existing work there, given the object ID of the local MODS object.
     * <p>
     * The request path must contain the MCRObjectID to publish.
     * The current user must have an ORCID profile and must have authorized this application
     * to add or updated works.
     * <p>
     * Returns the new publication status as by {@link #getPublicationStatus(String)}
     *
     * @author Frank Lützenkirchen
     */
    @GET
    @Path("publish/{objectID}")
    @Produces(MediaType.APPLICATION_JSON)
    public String publish(@PathParam("objectID") String objectID) {
        MCRObjectID oid = checkID(objectID);
        MCRORCIDUser user = MCRORCIDSession.getCurrentUser();

        MCRUserStatus userStatus = user.getStatus();
        if (!(userStatus.isORCIDUser() && userStatus.weAreTrustedParty())) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        try {
            MCRORCIDProfile profile = user.getProfile();
            MCRWorksSection works = profile.getWorksSection();
            MCRPublicationStatus status = user.getPublicationStatus(oid);

            if (!status.isUsersPublication()) {
                throw new WebApplicationException(Status.FORBIDDEN);
            } else if (status.isInORCIDProfile()) {
                works.findWork(oid).get().update(oid);
            } else {
                works.addWorkFrom(oid);
            }

            return publicationStatus(oid, user);
        } catch (Exception ex) {
            LOGGER.warn(() -> "Exception publishing " + oid + " to ORCID", ex);
            String msg = ex.getClass().getName() + " " + ex.getMessage();
            throw new InternalServerErrorException(msg, ex);
        }
    }

    private MCRObjectID checkID(String objectID) {
        if (objectID == null || objectID.isEmpty() || !MCRObjectID.isValid(objectID)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        MCRObjectID oid = MCRObjectID.getInstance(objectID);
        if (!MCRMetadataManager.exists(oid)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        return oid;
    }

    private String publicationStatus(MCRObjectID oid, MCRORCIDUser user)
        throws JDOMException, IOException, SAXException {
        MCRPublicationStatus status = user.getPublicationStatus(oid);
        Gson gson = MCRJSONManager.obtainInstance().createGson();
        return gson.toJson(status);
    }
}
