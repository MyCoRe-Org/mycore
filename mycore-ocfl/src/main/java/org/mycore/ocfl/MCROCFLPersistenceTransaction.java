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

package org.mycore.ocfl;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceTransaction;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLPersistenceTransaction implements MCRPersistenceTransaction {

    private static final Logger LOGGER = LogManager.getLogger(MCROCFLPersistenceTransaction.class);

    // TODO replace this with a setting
    private static final String Q_DATA = "classQueue";

    protected MCRSession currentSession;

    protected Optional<MCROCFLXMLClassificationManager> managerOpt;

    // private ThreadLocal<Set<MCRCategoryID>> threadLocal;
    private ThreadLocal<Map<MCRCategoryID, Boolean>> markForNewOCFLVersion;

    private boolean rollbackOnly = false;

    private ArrayList<MCREvent> rollbackList;

    // list of all things to commit, save them here instead of session
    // how do we write here?

    public MCROCFLPersistenceTransaction() {
        try {
            managerOpt = MCRConfiguration2
                .<MCROCFLXMLClassificationManager>getSingleInstanceOf("MCR.Classification.Manager");
        } catch (Exception e) {
            LOGGER.debug("ClassificationManager could not be found, setting to empty.");
            managerOpt = Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReady() {
        LOGGER.debug("TRANSACTION READY CHECK - {}", managerOpt.isPresent());
        if (!managerOpt.isPresent()) {
            return false;
        } else {
            return !isActive() && managerOpt.get().isMutable();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void begin() {
        LOGGER.debug("TRANSACTION BEGIN");
        // if (isActive()) {
        //     throw new IllegalStateException("TRANSACTION ALREADY ACTIVE");
        // }
        if (isActive()) {
            LOGGER.warn("EXISTING TRANSACTION, ROLLING BACK FOR CLEAN STATE");
            rollback();
        }
        currentSession = MCRSessionMgr.getCurrentSession();
        currentSession.put(Q_DATA, new ArrayList<MCREvent>());
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void commit() {
        LOGGER.debug("TRANSACTION COMMIT");
        if (!isActive() || getRollbackOnly()) {
            throw new IllegalStateException("TRANSACTION NOT ACTIVE OR ONLY ROLLBACK");
        }
        try {
            managerOpt.get().commitSession(currentSession);
        } catch (Exception e) {
            rollbackOnly = true;
            rollbackList = (ArrayList<MCREvent>) currentSession.get(Q_DATA);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void rollback() {
        LOGGER.debug("TRANSACTION ROLLBACK");
        if (!isActive()) {
            throw new IllegalStateException("TRANSACTION NOT ACTIVE");
        }
        try {
            if (rollbackOnly) {
                ((ArrayList<MCREvent>) currentSession.get(Q_DATA)).addAll(rollbackList);
            }
        } catch (Exception e) {
            currentSession.put(Q_DATA, rollbackList);
        }
        managerOpt.get().rollbackSession(currentSession);
        rollbackOnly = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getRollbackOnly() {
        LOGGER.debug("TRANSACTION ROLLBACK CHECK - {}", rollbackOnly);
        if (!isActive()) {
            throw new IllegalStateException("TRANSACTION NOT ACTIVE");
        }
        return rollbackOnly;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        boolean active = MCRSessionMgr.getCurrentSession().get(Q_DATA) != null;
        LOGGER.debug("TRANSACTION ACTIVE CHECK - {}", active);
        return active;
    }

}
