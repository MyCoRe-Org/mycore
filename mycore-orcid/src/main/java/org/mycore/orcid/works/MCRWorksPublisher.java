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

package org.mycore.orcid.works;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.orcid.MCRORCIDConstants;
import org.mycore.orcid.MCRORCIDException;
import org.mycore.orcid.MCRORCIDProfile;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Provides functionality to create, update and delete works in the remote ORCID profile
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRWorksPublisher {

    private static final Logger LOGGER = LogManager.getLogger(MCRWorksPublisher.class);

    /** Transformer used to transform a MyCoRe object with MODS to ORCID's work XML schema */
    private static final MCRContentTransformer T_MCR2WORK = MCRContentTransformerFactory.getTransformer("MyCoRe2Work");

    private MCRORCIDProfile orcid;

    public MCRWorksPublisher(MCRORCIDProfile orcid) {
        this.orcid = orcid;
    }

    /** Publishes the object (its MODS) as a new "work" in the ORCID profile */
    MCRWork createWorkFrom(MCRObjectID objectID)
        throws IOException, JDOMException, SAXException {
        WebTarget target = orcid.getWebTarget().path("work");
        Builder builder = buildInvocation(target);

        Document workXML = buildWorkXMLFrom(objectID);
        Entity<InputStream> input = buildRequestEntity(workXML);

        LOGGER.info("post (create){} at {}", objectID, target.getUri());
        Response response = builder.post(input);
        expect(response, Response.Status.CREATED);

        String putCode = getPutCode(response);
        MCRWork work = new MCRWork(orcid, putCode);
        work.fetchDetails();
        return work;
    }

    void update(MCRWork work) throws IOException, SAXException {
        WebTarget target = orcid.getWebTarget().path("work").path(work.getPutCode());
        Builder builder = buildInvocation(target);

        Document workXML = buildWorkXMLFrom(work.getObjectID());
        workXML.getRootElement().setAttribute("put-code", work.getPutCode());
        Entity<InputStream> input = buildRequestEntity(workXML);

        LOGGER.info("put (update) {} to {}", work.getObjectID(), target.getUri());
        Response response = builder.put(input);
        expect(response, Response.Status.OK);
    }

    void delete(MCRWork work) throws IOException, JDOMException, SAXException {
        WebTarget target = orcid.getWebTarget().path("work").path(work.getPutCode());
        Builder builder = buildInvocation(target);
        LOGGER.info("delete {} from {}", work.getObjectID(), target.getUri());
        Response response = builder.delete();
        expect(response, Response.Status.NO_CONTENT);
        orcid.getWorksSection().removeWork(work);
    }

    private Builder buildInvocation(WebTarget target) {
        return target.request().accept(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + orcid.getAccessToken());
    }

    /** Retrieves the MyCoRe object, transforms it to ORCID work xml and validates */
    private Document buildWorkXMLFrom(MCRObjectID objectID) throws IOException, SAXParseException {
        MCRContent mcrObject = MCRXMLMetadataManager.instance().retrieveContent(objectID);
        MCRContent workXML = T_MCR2WORK.transform(mcrObject);
        return MCRXMLParserFactory.getValidatingParser().parseXML(workXML);
    }

    private Entity<InputStream> buildRequestEntity(Document workXML) throws IOException {
        InputStream in = new MCRJDOMContent(workXML).getInputStream();
        return Entity.entity(in, MCRORCIDConstants.ORCID_XML_MEDIA_TYPE);
    }

    /** Returns the put code given in the response header after a successful POST of new work */
    private String getPutCode(Response response) {
        String location = response.getHeaders().getFirst("Location").toString();
        return location.substring(location.lastIndexOf('/') + 1);
    }

    /**
     * If the response is not as expected and the request was not successful,
     * throws an exception with detailed error message from the ORCID REST API.
     *
     * @param response the response to the REST request
     * @param expectedStatus the status expected when request is successful
     * @throws MCRORCIDException if the ORCID API returned error information
     */
    private void expect(Response response, Response.Status expectedStatus)
        throws IOException {
        StatusType status = response.getStatusInfo();
        if (status.getStatusCode() != expectedStatus.getStatusCode()) {
            throw new MCRORCIDException(response);
        }
    }
}
