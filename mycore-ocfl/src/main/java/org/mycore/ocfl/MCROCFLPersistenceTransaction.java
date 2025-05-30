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

package org.mycore.ocfl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.MCRPersistenceTransaction;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.common.MCRAbstractMetadataVersion;
import org.mycore.ocfl.classification.MCROCFLXMLClassificationManager;

/**
 * @author Tobias Lenhardt [Hammer1279]
 * @author Thomas Scheffler (yagee)
 */
public class MCROCFLPersistenceTransaction implements MCRPersistenceTransaction {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCROCFLXMLClassificationManager MANAGER = MCRConfiguration2
        .getSingleInstanceOf(MCROCFLXMLClassificationManager.class, "MCR.Classification.Manager")
        .orElse(null);

    private static final ThreadLocal<Map<MCRCategoryID, Character>> CATEGORY_WORKSPACE = new ThreadLocal<>();

    private final String threadId = Thread.currentThread().toString();

    @Override
    public boolean isReady() {
        LOGGER.debug("TRANSACTION {} READY CHECK - {}", () -> threadId, () -> MANAGER != null);
        return MANAGER != null;
    }

    @Override
    public void begin() {
        LOGGER.debug("TRANSACTION {} BEGIN", threadId);
        CATEGORY_WORKSPACE.set(new HashMap<>());
    }

    @Override
    public void commit() {
        LOGGER.debug("TRANSACTION {} COMMIT", threadId);
        final Map<MCRCategoryID, Character> mapOfChanges = CATEGORY_WORKSPACE.get();
        // save new OCFL version of classifications
        // value is category if classification should not be deleted
        mapOfChanges.forEach((categoryID, eventType) -> MCRSessionMgr.getCurrentSession()
            .onCommit(() -> {
                LOGGER.debug("[{}] UPDATING CLASS <{}>", threadId, categoryID);
                try {
                    switch (eventType) {
                        case MCRAbstractMetadataVersion.CREATED,
                            MCRAbstractMetadataVersion.UPDATED -> createOrUpdateOCFLClassification(
                                categoryID, eventType);
                        case MCRAbstractMetadataVersion.DELETED -> MANAGER.delete(categoryID);
                        default -> throw new IllegalStateException(
                            "Unsupported type in classification found: " + eventType + ", " + categoryID);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }));
        CATEGORY_WORKSPACE.remove();
    }

    private static void createOrUpdateOCFLClassification(MCRCategoryID categoryID, Character eventType)
        throws IOException {
        // read classification from just here
        final MCRCategoryDAO categoryDAO = MCRCategoryDAOFactory.obtainInstance();
        final MCRCategory categoryRoot = categoryDAO
            .getCategory(categoryID, -1);
        if (categoryID == null) {
            throw new IOException(
                "Could not get classification " + categoryID + " from " + categoryDAO.getClass().getName());
        }
        final Document categoryXML = MCRCategoryTransformer.getMetaDataDocument(categoryRoot, false);
        final MCRJDOMContent classContent = new MCRJDOMContent(categoryXML);
        try {
            if (eventType == MCRAbstractMetadataVersion.CREATED) {
                MANAGER.create(categoryID, classContent);
            } else {
                MANAGER.update(categoryID, classContent);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void rollback() {
        LOGGER.debug("TRANSACTION {} ROLLBACK", threadId);
        CATEGORY_WORKSPACE.remove();
    }

    /**
     * Add Classifications to get Updated/Deleted once the Transaction gets Committed
     * @param id The ID of the Classification
     * @param type 'A' for created, 'M' for modified, 'D' deleted
     */
    public static void addClassficationEvent(MCRCategoryID id, char type) {
        if (!Objects.requireNonNull(id).isRootID()) {
            throw new IllegalArgumentException("Only root category ids are allowed: " + id);
        }
        switch (type) {
            case MCRAbstractMetadataVersion.CREATED, MCRAbstractMetadataVersion.DELETED -> CATEGORY_WORKSPACE.get()
                .put(id, type);
            case MCRAbstractMetadataVersion.UPDATED -> {
                final char oldType = CATEGORY_WORKSPACE.get().getOrDefault(id, '0');
                switch (oldType) {
                    case MCRAbstractMetadataVersion.CREATED:
                    case MCRAbstractMetadataVersion.UPDATED:
                        break;
                    case '0':
                        CATEGORY_WORKSPACE.get().put(id, type);
                        break;
                    case MCRAbstractMetadataVersion.DELETED:
                        throw new IllegalArgumentException("Cannot update a deleted classification: " + id);
                    default:
                        throw new IllegalStateException(
                            "Unsupported type in classification found: " + oldType + ", " + id);
                }
            }
            default -> throw new IllegalStateException(
                "Unsupported event type for classification found: " + type + ", " + id);
        }
    }

    @Override
    public int getCommitPriority() {
        return 6000;
    }

}
