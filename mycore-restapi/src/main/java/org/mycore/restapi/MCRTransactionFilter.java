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

package org.mycore.restapi;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCRJPAUtil;
import org.mycore.common.MCRSessionMgr;

@Priority(Priorities.USER)
public class MCRTransactionFilter implements ContainerRequestFilter {

    public static final String PROP_REQUIRE_TRANSACTION = "mcr:jpaTrans";

    public static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (MCRSessionMgr.isLocked()) {
            return;
        }
        if (MCRJPAUtil.isTransactionActive()) {
            LOGGER.debug("Filter scoped JPA transaction is active.");
            if (MCRJPAUtil.transactionRequiresRollback()) {
                try {
                    MCRJPAUtil.rollbackTransaction();
                } finally {
                    throw new InternalServerErrorException("Transaction rollback was required.");
                }
            }
            MCRJPAUtil.commitTransaction();
        }
        if (Boolean.TRUE.equals(requestContext.getProperty(PROP_REQUIRE_TRANSACTION))) {
            LOGGER.debug("Starting user JPA transaction.");
            MCRJPAUtil.beginTransaction();
        }
    }
}
