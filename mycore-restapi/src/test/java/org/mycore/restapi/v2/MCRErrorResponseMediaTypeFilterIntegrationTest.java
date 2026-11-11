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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Checks that {@link MCRErrorResponseMediaTypeFilter} runs in the Jersey pipeline after the media type has been
 * negotiated, so a client accepting neither JSON nor XML gets the original status with a JSON error body.
 */
public class MCRErrorResponseMediaTypeFilterIntegrationTest {

    private static final String APPLICATION_PDF = "application/pdf";

    private ErrorJerseyTest jersey;

    @BeforeEach
    public void setUp() throws Exception {
        jersey = new ErrorJerseyTest();
        jersey.setUp();
    }

    @AfterEach
    public void tearDown() throws Exception {
        jersey.tearDown();
    }

    @Test
    public void errorResponseShouldBeJsonIfNeitherJsonNorXmlIsAccepted() throws Exception {
        try (Response response = jersey.target("error").request(APPLICATION_PDF).get()) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()));
            assertEquals("TEST_ERROR", response.getHeaderString("X-Error-Code"));
            JsonNode body = new ObjectMapper().readTree(response.readEntity(String.class));
            assertEquals("TEST_ERROR", body.path("errorCode").asText());
            assertNotNull(body.get("uuid"));
        }
    }

    @Test
    public void errorResponseShouldKeepXmlIfXmlIsAccepted() {
        try (Response response = jersey.target("error").request(MediaType.APPLICATION_XML_TYPE).get()) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertTrue(MediaType.APPLICATION_XML_TYPE.isCompatible(response.getMediaType()));
            assertTrue(response.readEntity(String.class).contains("errorCode=\"TEST_ERROR\""));
        }
    }

    @Test
    public void unexpectedExceptionShouldBeJsonServerErrorIfNeitherJsonNorXmlIsAccepted() throws Exception {
        try (Response response = jersey.target("error/unexpected").request(APPLICATION_PDF).get()) {
            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
            assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()));
            JsonNode body = new ObjectMapper().readTree(response.readEntity(String.class));
            assertEquals("boom", body.path("message").asText());
            assertEquals(IllegalStateException.class.getName(), body.path("detail").asText());
            assertNotNull(body.get("uuid"));
        }
    }

    @Test
    public void webApplicationExceptionShouldBeJsonIfNeitherJsonNorXmlIsAccepted() throws Exception {
        try (Response response = jersey.target("error/rollback").request(APPLICATION_PDF).get()) {
            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
            assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()));
            JsonNode body = new ObjectMapper().readTree(response.readEntity(String.class));
            assertEquals("Transaction rollback was required.", body.path("message").asText());
            assertNotNull(body.get("uuid"));
        }
    }

    @Path("error")
    public static class ErrorResource {

        @GET
        public Response get() {
            throw MCRErrorResponse.ofStatusCode(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode("TEST_ERROR")
                .withMessage("not found")
                .toException();
        }

        @GET
        @Path("unexpected")
        public Response getUnexpected() {
            throw new IllegalStateException("boom");
        }

        @GET
        @Path("rollback")
        public Response getRollback() {
            throw new InternalServerErrorException("Transaction rollback was required.");
        }
    }

    private static class ErrorJerseyTest extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig()
                .register(ErrorResource.class)
                .register(MCRExceptionMapper.class)
                .register(MCRErrorResponseMediaTypeFilter.class)
                .register(JacksonXmlBindJsonProvider.class);
        }
    }
}
