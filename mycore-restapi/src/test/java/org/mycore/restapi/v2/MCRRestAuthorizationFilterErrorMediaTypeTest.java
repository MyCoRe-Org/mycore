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
import static org.junit.Assert.assertTrue;
import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.frontend.jersey.access.MCRRequestScopeACL;
import org.mycore.frontend.jersey.access.MCRRequestScopeACLFilter;

import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

import jakarta.annotation.Priority;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Checks that errors raised by {@link MCRRestAuthorizationFilter} after resource matching keep their status, even if
 * the matched resource method produces neither JSON nor XML.
 */
public class MCRRestAuthorizationFilterErrorMediaTypeTest extends MCRTestCase {

    private static final String OBJECT_ID = "test_document_00000001";

    private static final String BROWSER_IMAGE_ACCEPT = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8";

    private ObjectsJerseyTest jersey;

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
    public void forbiddenOnJsonEndpoint() {
        try (Response response = jersey.target("objects/" + OBJECT_ID).request("*/*").get()) {
            assertForbiddenAsJson(response);
        }
    }

    @Test
    public void forbiddenOnTextEndpoint() {
        try (Response response = jersey.target("objects/" + OBJECT_ID + "/text").request("*/*").get()) {
            assertForbiddenAsJson(response);
        }
    }

    @Test
    public void forbiddenOnThumbnailEndpoint() {
        try (Response response = jersey.target("objects/" + OBJECT_ID + "/thumb.png").request(BROWSER_IMAGE_ACCEPT)
            .get()) {
            assertForbiddenAsJson(response);
        }
    }

    @Test
    public void forbiddenOnThumbnailEndpointAsXmlIfXmlIsAccepted() {
        try (Response response = jersey.target("objects/" + OBJECT_ID + "/thumb.png")
            .request(MediaType.APPLICATION_XML_TYPE, MediaType.valueOf("image/png")).get()) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
            assertTrue(MediaType.APPLICATION_XML_TYPE.isCompatible(response.getMediaType()));
            assertTrue(response.readEntity(String.class).contains("errorCode=\"API_NO_PERMISSION\""));
        }
    }

    private static void assertForbiddenAsJson(Response response) {
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()));
        assertEquals(MCRErrorCodeConstants.API_NO_PERMISSION, response.getHeaderString("X-Error-Code"));
    }

    @Path("objects/{" + PARAM_MCRID + "}")
    public static class ObjectResource {

        @GET
        @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
        public String getObject(@PathParam(PARAM_MCRID) String id) {
            return id;
        }

        @GET
        @Path("text")
        @Produces(MediaType.TEXT_PLAIN)
        public String getText(@PathParam(PARAM_MCRID) String id) {
            return id;
        }

        @GET
        @Path("thumb.png")
        @Produces({ "image/png", "image/jpeg" })
        public byte[] getThumbnail(@PathParam(PARAM_MCRID) String id) {
            return new byte[0];
        }
    }

    @Priority(Priorities.AUTHENTICATION)
    public static class DenyAllACLFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) {
            requestContext.setProperty(MCRRequestScopeACLFilter.ACL_INSTANT_KEY, new DenyAllACL());
        }
    }

    private static class DenyAllACL implements MCRRequestScopeACL {

        @Override
        public boolean isPrivate() {
            return false;
        }

        @Override
        public boolean checkPermission(String permission) {
            return false;
        }

        @Override
        public boolean checkPermission(String id, String permission) {
            return false;
        }
    }

    private static class ObjectsJerseyTest extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig()
                .register(ObjectResource.class)
                .register(DenyAllACLFilter.class)
                .register(MCRRestAuthorizationFilter.class)
                .register(MCRExceptionMapper.class)
                .register(MCRErrorResponseMediaTypeFilter.class)
                .register(JacksonXmlBindJsonProvider.class);
        }
    }
}
