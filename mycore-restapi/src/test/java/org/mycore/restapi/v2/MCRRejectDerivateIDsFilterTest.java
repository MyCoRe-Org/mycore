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

package org.mycore.restapi.v2;

import static org.junit.Assert.assertEquals;
import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;

import java.util.Map;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class MCRRejectDerivateIDsFilterTest extends MCRTestCase {

    private static final String OBJECT_ID = "test_document_00000001";

    private static final String DERIVATE_ID = "test_derivate_00000001";

    private ObjectsJerseyTest jersey;

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> properties = super.getTestProperties();
        properties.put("MCR.Metadata.Type.document", "true");
        properties.put("MCR.Metadata.Type.derivate", "true");
        return properties;
    }

    @Before
    public void setUpJersey() throws Exception {
        jersey = new ObjectsJerseyTest();
        jersey.setUp();
    }

    @After
    public void tearDownJersey() throws Exception {
        jersey.tearDown();
    }

    @Test
    public void objectIdIsAccepted() {
        try (Response response = jersey.target("objects/" + OBJECT_ID).request().get()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals(OBJECT_ID, response.readEntity(String.class));
        }
    }

    @Test
    public void derivateIdIsRejected() {
        try (Response response = jersey.target("objects/" + DERIVATE_ID).request().get()) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(MCRErrorCodeConstants.MCROBJECT_NOT_FOUND, response.getHeaderString("X-Error-Code"));
        }
    }

    @Test
    public void derivateIdIsRejectedInSubPath() {
        try (Response response = jersey.target("objects/" + DERIVATE_ID + "/derivates").request().get()) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(MCRErrorCodeConstants.MCROBJECT_NOT_FOUND, response.getHeaderString("X-Error-Code"));
        }
    }

    @Path("objects/{" + PARAM_MCRID + "}")
    public static class ObjectResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String getObject(@PathParam(PARAM_MCRID) String id) {
            return id;
        }

        @GET
        @Path("derivates")
        @Produces(MediaType.TEXT_PLAIN)
        public String getDerivates(@PathParam(PARAM_MCRID) String id) {
            return id;
        }
    }

    private static class ObjectsJerseyTest extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig()
                .register(ObjectResource.class)
                .register(MCRRejectDerivateIDsFilter.class)
                .register(MCRExceptionMapper.class)
                .register(MCRErrorResponseMediaTypeFilter.class)
                .register(JacksonXmlBindJsonProvider.class);
        }
    }
}
