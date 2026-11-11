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

package org.mycore.restapi;

import static org.junit.Assert.assertEquals;
import static org.mycore.restapi.MCRTransactionFilterTest.TestTransaction.BEGIN;
import static org.mycore.restapi.MCRTransactionFilterTest.TestTransaction.COMMIT;
import static org.mycore.restapi.MCRTransactionFilterTest.TestTransaction.EVENTS;
import static org.mycore.restapi.MCRTransactionFilterTest.TestTransaction.ROLLBACK;

import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTransactionManager;
import org.mycore.frontend.jersey.resources.MCRJerseyTestFeature;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * Runs {@link MCRTransactionFilter} together with {@link MCRSessionFilter} in Jersey. Unlike
 * {@link MCRTransactionFilterTest}, this covers that Jersey marks responses created from exceptions and that the
 * response part of {@link MCRTransactionFilter} runs before {@link MCRSessionFilter} commits or rolls back.
 * <p>
 * Each request should begin and commit the authentication transaction, then begin the user transaction, which is
 * committed or rolled back.
 */
public class MCRTransactionFilterJerseyTest extends MCRTestCase {

    private MCRJerseyTestFeature jersey;

    @Before
    public void setUpJersey() throws Exception {
        MCRTransactionManager.rollbackTransactions();
        jersey = new MCRJerseyTestFeature();
        jersey.setUp(Set.of(
            MCRSessionFilter.class,
            MCREnableTransactionFilter.class,
            MCRTransactionFilter.class,
            TestResource.class,
            TestExceptionMapper.class));
        EVENTS.clear();
    }

    @After
    public void tearDownJersey() throws Exception {
        jersey.tearDown();
        EVENTS.clear();
    }

    @Test
    public void successfulRequestShouldCommit() {
        assertTransactions(Response.Status.NO_CONTENT, "success", COMMIT);
    }

    @Test
    public void errorResponseReturnedWithoutExceptionShouldCommit() {
        assertTransactions(Response.Status.BAD_REQUEST, "error-response", COMMIT);
    }

    @Test
    public void webApplicationExceptionShouldRollBack() {
        assertTransactions(Response.Status.BAD_REQUEST, "web-application-exception", ROLLBACK);
    }

    @Test
    public void mappedExceptionShouldRollBack() {
        assertTransactions(Response.Status.CONFLICT, "mapped-exception", ROLLBACK);
    }

    private void assertTransactions(Response.Status expectedStatus, String path, String expectedOutcome) {
        try (Response response = jersey.target("transaction/" + path).request().post(null)) {
            assertEquals(expectedStatus.getStatusCode(), response.getStatus());
        }
        assertEquals(List.of(BEGIN, COMMIT, BEGIN, expectedOutcome), EVENTS);
    }

    @Path("transaction")
    public static class TestResource {

        @POST
        @Path("success")
        public void success() {
        }

        @POST
        @Path("error-response")
        public Response errorResponse() {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        @POST
        @Path("web-application-exception")
        public void webApplicationException() {
            throw new BadRequestException();
        }

        @POST
        @Path("mapped-exception")
        public void mappedException() {
            throw new TestException();
        }

    }

    public static class TestExceptionMapper implements ExceptionMapper<TestException> {

        @Override
        public Response toResponse(TestException exception) {
            return Response.status(Response.Status.CONFLICT).build();
        }

    }

    private static final class TestException extends RuntimeException {

        private static final long serialVersionUID = 1L;

    }

}
