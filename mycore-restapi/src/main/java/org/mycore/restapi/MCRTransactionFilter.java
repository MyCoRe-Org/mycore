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

import java.util.EnumSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ContainerResponse;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionManager;

import jakarta.annotation.Priority;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;

/**
 * Manages the JPA transaction of a REST request.
 * <p>
 * On request, the transaction opened by {@link MCRSessionFilter} for authentication is committed, and a new
 * transaction is started if the resource requires one (see {@link MCREnableTransactionFilter}).
 * <p>
 * On response, all active transactions are marked rollback-only if the response was created from a thrown
 * exception (by an {@link jakarta.ws.rs.ext.ExceptionMapper} or by Jersey itself for a
 * {@link jakarta.ws.rs.WebApplicationException}) and has an error status (4xx or 5xx). Otherwise
 * {@link MCRSessionFilter} would commit writes done before the exception. Because of its priority, this filter
 * runs before the response filter of {@link MCRSessionFilter} that commits or rolls back.
 * <p>
 * Not affected, so committed as usual:
 * <ul>
 *   <li>responses returned with an error status without throwing an exception,</li>
 *   <li>exceptions carrying a non-error status, e.g. a thrown redirect,</li>
 *   <li>exceptions from an earlier request filter (e.g. authorization): the request part of this filter did not
 *   run, and this filter leaves the authentication transaction untouched.</li>
 * </ul>
 */
@Priority(Priorities.USER)
public class MCRTransactionFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String PROP_REQUIRE_TRANSACTION = "mcr:jpaTrans";

    /**
     * Set once the request part of this filter ran, i.e. the authentication transaction (if any) is no longer
     * pending. Only then the response part may mark transactions rollback-only.
     */
    static final String PROP_REQUEST_FILTERED = "mcr:jpaTransRequestFiltered";

    private static final Set<Response.Status.Family> ERROR_FAMILIES =
        EnumSet.of(Response.Status.Family.CLIENT_ERROR, Response.Status.Family.SERVER_ERROR);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (MCRSessionMgr.isLocked()) {
            return;
        }
        MCRSessionMgr.getCurrentSession();
        if (MCRTransactionManager.hasActiveTransactions()) {
            LOGGER.debug("Filter scoped JPA transaction is active.");
            if (MCRTransactionManager.hasRollbackOnlyTransactions()) {
                InternalServerErrorException internalServerErrorException =
                    new InternalServerErrorException("Transaction rollback was required.");
                try {
                    MCRTransactionManager.rollbackTransactions();
                } catch(Exception exc) {
                    internalServerErrorException.addSuppressed(exc);
                }
                throw internalServerErrorException;
            }
            MCRTransactionManager.commitTransactions();
        }
        requestContext.setProperty(PROP_REQUEST_FILTERED, true);
        if (Boolean.TRUE.equals(requestContext.getProperty(PROP_REQUIRE_TRANSACTION))) {
            LOGGER.debug("Starting user JPA transaction.");
            MCRTransactionManager.beginTransactions();
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // without the request part, the authentication transaction may still be pending;
        // an exception from an earlier request filter must not roll back the login
        if (!Boolean.TRUE.equals(requestContext.getProperty(PROP_REQUEST_FILTERED))) {
            return;
        }
        // Jersey marks every response created from a thrown exception; plain JAX-RS has no access to it here
        if (responseContext instanceof ContainerResponse response && response.isMappedFromException()
            && ERROR_FAMILIES.contains(response.getStatusInfo().getFamily())) {
            LOGGER.debug("Error response was created from an exception, marking transactions rollback-only.");
            MCRTransactionManager.setRollbackOnly();
        }
    }
}
