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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.glassfish.jersey.server.ContainerResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRPersistenceTransaction;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTransactionHelper;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Response;

/**
 * Runs against the real {@link MCRTransactionHelper}. The only persistence transaction available in this module's
 * tests is {@link TestTransaction}, registered via {@code META-INF/services} in the test resources.
 */
public class MCRTransactionFilterTest extends MCRTestCase {

    private final MCRTransactionFilter filter = new MCRTransactionFilter();

    private final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

    // TestTransaction is registered for all tests of this module, so start and end without open transactions
    @Before
    public void rollBackTransactionsOfPreviousTests() {
        MCRTransactionHelper.rollbackTransaction();
    }

    @After
    public void rollBackLeftovers() {
        MCRTransactionHelper.rollbackTransaction();
    }

    @Test
    public void requestPartShouldCommitAuthenticationTransactionAndMarkRequestAsFiltered() {
        MCRTransactionHelper.beginTransaction();

        filter.filter(requestContext);

        assertFalse(MCRTransactionHelper.isTransactionActive());
        verify(requestContext).setProperty(MCRTransactionFilter.PROP_REQUEST_FILTERED, true);
    }

    @Test
    public void requestPartShouldNotMarkRequestAsFilteredWhenRollbackIsRequired() {
        MCRTransactionHelper.beginTransaction();
        MCRTransactionHelper.setRollbackOnly();

        assertThrows(InternalServerErrorException.class, () -> filter.filter(requestContext));

        assertFalse(MCRTransactionHelper.isTransactionActive());
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
        MCRTransactionHelper.beginTransaction();

        filter.filter(requestContext, response(true, Response.Status.FORBIDDEN));

        assertFalse(MCRTransactionHelper.transactionRequiresRollback());
    }

    private void assertRollbackOnly(boolean expected, ContainerResponseContext responseContext) {
        when(requestContext.getProperty(MCRTransactionFilter.PROP_REQUEST_FILTERED)).thenReturn(true);
        MCRTransactionHelper.beginTransaction();
        assertTrue(MCRTransactionHelper.isTransactionActive());

        filter.filter(requestContext, responseContext);

        if (expected) {
            assertTrue(MCRTransactionHelper.transactionRequiresRollback());
        } else {
            assertFalse(MCRTransactionHelper.transactionRequiresRollback());
        }
    }

    private static ContainerResponse response(boolean mappedFromException, Response.Status status) {
        ContainerResponse response = mock(ContainerResponse.class);
        when(response.isMappedFromException()).thenReturn(mappedFromException);
        when(response.getStatusInfo()).thenReturn(status);
        return response;
    }

    /**
     * Minimal persistence transaction for tests without a database.
     */
    public static class TestTransaction implements MCRPersistenceTransaction {

        private boolean active;

        private boolean rollbackOnly;

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void begin() {
            active = true;
            rollbackOnly = false;
        }

        @Override
        public void commit() {
            active = false;
        }

        @Override
        public void rollback() {
            active = false;
            rollbackOnly = false;
        }

        @Override
        public boolean getRollbackOnly() {
            return rollbackOnly;
        }

        @Override
        public void setRollbackOnly() {
            rollbackOnly = true;
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
