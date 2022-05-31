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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.MCRPersistenceTransaction;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;

/**
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLPersistenceTransaction implements MCRPersistenceTransaction {

    private static final Logger LOGGER = LogManager.getLogger(MCROCFLPersistenceTransaction.class);

    protected MCRSession currentSession;

    protected Optional<MCROCFLXMLClassificationManager> managerOpt;

    // private ThreadLocal<Set<MCRCategoryID>> threadLocal;
    private static final ThreadLocal<Map<MCRCategoryID, MCRCategory>> CATEGORY_WORKSPACE = new ThreadLocal<>();

    private boolean rollbackOnly;

    private boolean active;

    // list of all things to commit, save them here instead of session
    // how do we write here?

    public MCROCFLPersistenceTransaction() {
        try {
            managerOpt = MCRConfiguration2.getSingleInstanceOf("MCR.Classification.Manager");
        } catch (Exception e) {
            LOGGER.debug("ClassificationManager could not be found, setting to empty.");
            managerOpt = Optional.empty();
        }
        rollbackOnly = false;
        active = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReady() {
        LOGGER.debug("TRANSACTION READY CHECK - {}", managerOpt.isPresent());
        return managerOpt.isPresent() && !isActive() && managerOpt.get().isMutable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void begin() {
        LOGGER.debug("TRANSACTION BEGIN");
        if (isActive()) {
            throw new IllegalStateException("TRANSACTION ALREADY ACTIVE");
        }
        active = true;
        CATEGORY_WORKSPACE.set(new HashMap<>());
        currentSession = MCRSessionMgr.getCurrentSession();
    }

    @Override
    public void commit() {
        LOGGER.debug("TRANSACTION COMMIT");
        if (!isActive() || getRollbackOnly()) {
            throw new IllegalStateException("TRANSACTION NOT ACTIVE OR ONLY ROLLBACK");
        }
        active = false;
        final Map<MCRCategoryID, MCRCategory> mapOfChanges = CATEGORY_WORKSPACE.get();
        final MCROCFLXMLClassificationManager ocflClassficationManager = managerOpt.get();
        //save new OCFL version of classifications
        mapOfChanges.entrySet()
            .stream()
            .filter(e -> Objects.nonNull(e.getValue())) // value is category if classification should not be deleted
            .map(Map.Entry::getValue)
            .forEach(category -> MCRSessionMgr.getCurrentSession()
                .onCommit(() -> {
                    //TODO: read classification from just here
                    final Document categoryXML = MCRCategoryTransformer.getMetaDataDocument(category, false);
                    ocflClassficationManager.fileUpdate(category.getId(), category, new MCRJDOMContent(categoryXML),
                        null);
                }));
        //delete classifications
        mapOfChanges.entrySet()
            .stream()
            .filter(e -> Objects.isNull(e.getValue())) // value is category if classification should not be deleted
            .forEach(entry -> MCRSessionMgr.getCurrentSession()
                .onCommit(() -> ocflClassficationManager.fileDelete(entry.getKey(), null, null, null)));
        //throw away:
        try {
            managerOpt.get().commitSession(currentSession);
        } catch (Exception e) {
            rollbackOnly = true;
            throw e;
        }
    }

    @Override
    public void rollback() {
        LOGGER.debug("TRANSACTION ROLLBACK");
        if (!isActive()) {
            throw new IllegalStateException("TRANSACTION NOT ACTIVE");
        }
        active = false;
        CATEGORY_WORKSPACE.remove();
        rollbackOnly = false;
    }

    @Override
    public boolean getRollbackOnly() {
        LOGGER.debug("TRANSACTION ROLLBACK CHECK - {}", rollbackOnly);
        if (!isActive()) {
            throw new IllegalStateException("TRANSACTION NOT ACTIVE");
        }
        return rollbackOnly;
    }

    @Override
    public boolean isActive() {
        LOGGER.debug("TRANSACTION ACTIVE CHECK - {}", active);
        return active;
    }

    public static void addClassfication(MCRCategoryID id, @Nullable MCRCategory category) {
        if (!Objects.requireNonNull(id).isRootID()) {
            throw new IllegalArgumentException("Only root category ids are allowed: " + id);
        }
        CATEGORY_WORKSPACE.get().put(id, category);
    }

}
