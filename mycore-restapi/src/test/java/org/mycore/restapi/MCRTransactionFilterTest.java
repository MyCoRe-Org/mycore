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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.glassfish.jersey.server.ContainerResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRPersistenceTransaction;
import org.mycore.common.MCRTransactionManager;
import org.mycore.test.MyCoReTest;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Response;

/**
 * Runs against the real {@link MCRTransactionManager}. The only persistence transaction available in this module's
 * tests is {@link TestTransaction}, registered via {@code META-INF/services} in the test resources.
 */
@MyCoReTest
public class MCRTransactionFilterTest {

    private final MCRTransactionFilter filter = new MCRTransactionFilter();

    private final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

    // TestTransaction is registered for all tests of this module, so start and end without open transactions
    @BeforeEach
    public void rollBackTransactionsOfPreviousTests() {
        MCRTransactionManager.rollbackTransactions();
    }

    @AfterEach
    public void rollBackLeftovers() {
        MCRTransactionManager.rollbackTransactions();
    }

    @Test
    public void requestPartShouldCommitAuthenticationTransactionAndMarkRequestAsFiltered() {
        MCRTransactionManager.beginTransactions();

        filter.filter(requestContext);

        assertFalse(MCRTransactionManager.hasActiveTransactions());
        verify(requestContext).setProperty(MCRTransactionFilter.PROP_REQUEST_FILTERED, true);
    }

    @Test
    public void requestPartShouldNotMarkRequestAsFilteredWhenRollbackIsRequired() {
        MCRTransactionManager.beginTransactions();
        MCRTransactionManager.setRollbackOnly();

        assertThrows(InternalServerErrorException.class, () -> filter.filter(requestContext));

        assertFalse(MCRTransactionManager.hasActiveTransactions());
        verify(requestContext, never()).setProperty(eq(MCRTransactionFilter.PROP_REQUEST_FILTERED), any());
    }

    @Test
    public void clientErrorResponseFromExceptionShouldMarkTransactionsRollbackOnly() {
        assertRollbackOnly(true, response(true, Response.Status.FORBIDDEN));
    }

    @Test
    public void serverErrorResponseFromExceptionShouldMarkTransactionsRollbackOnly() {
        assertRollbackOnly(true, response(true, Response.Status.INTERNAL_SERVER_ERROR));
    }

    @Test
    public void nonErrorResponseFromExceptionShouldNotTouchTransactions() {
        assertRollbackOnly(false, response(true, Response.Status.SEE_OTHER));
    }

    @Test
    public void regularErrorResponseShouldNotTouchTransactions() {
        assertRollbackOnly(false, response(false, Response.Status.BAD_REQUEST));
    }

    @Test
    public void nonJerseyResponseShouldNotTouchTransactions() {
        assertRollbackOnly(false, mock(ContainerResponseContext.class));
    }

    @Test
    public void exceptionBeforeRequestPartShouldNotRollBackAuthenticationTransaction() {
        // e.g. the authorization filter threw before the authentication transaction was committed
        MCRTransactionManager.beginTransactions();

        filter.filter(requestContext, response(true, Response.Status.FORBIDDEN));

        assertFalse(MCRTransactionManager.hasRollbackOnlyTransactions());
    }

    private void assertRollbackOnly(boolean expected, ContainerResponseContext responseContext) {
        when(requestContext.getProperty(MCRTransactionFilter.PROP_REQUEST_FILTERED)).thenReturn(true);
        MCRTransactionManager.beginTransactions();
        assertTrue(MCRTransactionManager.hasActiveTransactions());

        filter.filter(requestContext, responseContext);

        if (expected) {
            assertTrue(MCRTransactionManager.hasRollbackOnlyTransactions());
        } else {
            assertFalse(MCRTransactionManager.hasRollbackOnlyTransactions());
        }
    }

    private static ContainerResponse response(boolean mappedFromException, Response.Status status) {
        ContainerResponse response = mock(ContainerResponse.class);
        when(response.isMappedFromException()).thenReturn(mappedFromException);
        when(response.getStatusInfo()).thenReturn(status);
        return response;
    }

    /**
     * Minimal persistence transaction for tests without a database. Records begin, commit and rollback of all
     * instances in {@link #EVENTS}, as requests may run on other threads with their own instances.
     */
    public static class TestTransaction implements MCRPersistenceTransaction {

        static final String BEGIN = "begin";

        static final String COMMIT = "commit";

        static final String ROLLBACK = "rollback";

        static final List<String> EVENTS = new CopyOnWriteArrayList<>();

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void begin() {
            EVENTS.add(BEGIN);
        }

        @Override
        public void commit() {
            EVENTS.add(COMMIT);
        }

        @Override
        public void rollback() {
            EVENTS.add(ROLLBACK);
        }

        @Override
        public int getCommitPriority() {
            return 0;
        }
    }
}
