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

package org.mycore.mets.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.mets.model.MCRMETSGeneratorFactory;
import org.mycore.mets.model.converter.MCRJSONSimpleModelConverter;
import org.mycore.mets.model.converter.MCRSimpleModelJSONConverter;
import org.mycore.mets.model.converter.MCRSimpleModelXMLConverter;
import org.mycore.mets.model.converter.MCRXMLSimpleModelConverter;
import org.mycore.mets.model.simple.MCRMetsSimpleModel;
import org.mycore.mets.tools.MCRMetsLock;
import org.mycore.resource.MCRResourceHelper;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/mets")
public class MetsResource {

    public static final String METS_XML_PATH = "/mets.xml";

    private static final String DERIVATE_ID = "derivateId";

    @GET
    @Path("/editor/start/{derivateId}")
    @Produces(MediaType.TEXT_HTML)
    public String startEditor(@PathParam(DERIVATE_ID) String derivateId) {
        MCRObjectID derivateIdObject = MCRObjectID.getInstance(derivateId);

        checkDerivateExists(derivateIdObject);
        checkDerivateAccess(derivateIdObject, MCRAccessManager.PERMISSION_WRITE);

        try (InputStream resourceAsStream = MCRResourceHelper.getResourceAsStream("mets-editor.html")) {
            String htmlTemplate = new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8);
            // add additional javascript code
            String js = MCRConfiguration2.getString("MCR.Mets.Editor.additional.javascript").orElse(null);
            if (js != null && !js.isEmpty()) {
                htmlTemplate = htmlTemplate.replace("<link rel=\"additionalJS\" />", js);
            }
            // replace variables
            htmlTemplate = htmlTemplate.replaceAll("\\{baseURL\\}", MCRFrontendUtil.getBaseURL())
                .replaceAll("\\{derivateID\\}", derivateId);
            return htmlTemplate;
        } catch (IOException e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/editor/islocked/{derivateId}")
    public String showIsLocked(@PathParam(DERIVATE_ID) String derivateId) {
        checkDerivateAccess(MCRObjectID.getInstance(derivateId), MCRAccessManager.PERMISSION_READ);
        boolean isLocked = MCRMetsLock.isLocked(derivateId);
        return "{\"lock\": " + isLocked + " }";
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/editor/lock/{derivateId}")
    public String lock(@PathParam(DERIVATE_ID) String derivateId) {
        checkDerivateAccess(MCRObjectID.getInstance(derivateId), MCRAccessManager.PERMISSION_WRITE);
        boolean isLockSuccessfully = MCRMetsLock.doLock(derivateId);
        return "{\"success\": " + isLockSuccessfully + " }";
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/editor/unlock/{derivateId}")
    public String unlock(@PathParam(DERIVATE_ID) String derivateId) {
        checkDerivateAccess(MCRObjectID.getInstance(derivateId), MCRAccessManager.PERMISSION_WRITE);
        try {
            MCRMetsLock.doUnlock(derivateId);
            return "{\"success\": true }";
        } catch (MCRException e) {
            return "{\"success\": false }";
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/crud/{derivateId}")
    public String get(@PathParam(DERIVATE_ID) String derivateId) {
        MCRObjectID derivateIdObject = MCRObjectID.getInstance(derivateId);

        checkDerivateExists(derivateIdObject);
        checkDerivateAccess(derivateIdObject, MCRAccessManager.PERMISSION_READ);

        MCRPath rootPath = MCRPath.getPath(derivateId, "/");
        if (!Files.isDirectory(rootPath)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        MCRPath metsPath = MCRPath.getPath(derivateId, METS_XML_PATH);
        try {
            return MCRSimpleModelJSONConverter.toJSON(MCRXMLSimpleModelConverter.fromXML(getMetsDocument(metsPath)));
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/crud/{derivateId}")
    public String save(@PathParam(DERIVATE_ID) String derivateId, String data) {
        MCRObjectID derivateIdObject = MCRObjectID.getInstance(derivateId);

        checkDerivateExists(derivateIdObject);
        checkDerivateAccess(derivateIdObject, MCRAccessManager.PERMISSION_WRITE);

        MCRMetsSimpleModel model = MCRJSONSimpleModelConverter.toSimpleModel(data);
        Document document = MCRSimpleModelXMLConverter.toXML(model);
        XMLOutputter o = new XMLOutputter();
        try (OutputStream out = Files.newOutputStream(MCRPath.getPath(derivateId, METS_XML_PATH),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
            o.output(document, out);
        } catch (IOException e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return "{ \"success\": true }";
    }

    @DELETE
    @Path("/crud/{derivateId}")
    public String delete(@PathParam(DERIVATE_ID) String derivateId) {
        MCRObjectID derivateIdObject = MCRObjectID.getInstance(derivateId);

        checkDerivateExists(derivateIdObject);
        checkDerivateAccess(derivateIdObject, MCRAccessManager.PERMISSION_DELETE);

        try {
            Files.delete(MCRPath.getPath(derivateId, METS_XML_PATH));
        } catch (IOException e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return "{ \"success\": true }";
    }

    /**
     * A simple helper to receive existing or create new default mets document.
     * @param metsPath the path to the mets document
     * @return a document with the content of the mets.xml
     * @throws WebApplicationException if something went wrong while generating or parsing the mets
     */
    private Document getMetsDocument(MCRPath metsPath) {
        if (!Files.exists(metsPath)) {
            try {
                return MCRMETSGeneratorFactory.create(metsPath.getParent()).generate().asDocument();
            } catch (Exception e) {
                throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
            }
        } else {
            try (InputStream inputStream = Files.newInputStream(metsPath)) {
                SAXBuilder builder = new SAXBuilder();
                return builder.build(inputStream);
            } catch (JDOMException | IOException e) {
                throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * A simple helper to detect if a user has a specific permission for a derivate.
     *
     * @param derivateIdObject the id Object of the derivate
     * @param permission       the permission to check {@link MCRAccessManager#PERMISSION_DELETE}
     *                         | {@link MCRAccessManager#PERMISSION_READ} | {@link MCRAccessManager#PERMISSION_WRITE}
     * @throws WebApplicationException if the user doesnt have the permission
     */
    private void checkDerivateAccess(MCRObjectID derivateIdObject, String permission) {
        if (!MCRAccessManager.checkPermission(derivateIdObject, permission)) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
    }

    /**
     * A simple helper to detect if a derivate exists.
     *
     * @param derivateIdObject the id object of the derivate
     * @throws WebApplicationException if the derivate does not exists
     */
    private void checkDerivateExists(MCRObjectID derivateIdObject) {
        if (!MCRMetadataManager.exists(derivateIdObject)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }
}
