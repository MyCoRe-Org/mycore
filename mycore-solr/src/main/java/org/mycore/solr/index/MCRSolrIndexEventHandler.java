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

package org.mycore.solr.index;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.common.MCRMarkManager;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRSolrIndexEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrIndexEventHandler.class);

    @Override
    protected synchronized void handleObjectCreated(MCREvent evt, MCRObject obj) {
        addObject(evt, obj);
    }

    @Override
    protected synchronized void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        if (MCRSolrUtils.useNestedDocuments()) {
            solrDelete(obj.getId());
        }
        addObject(evt, obj);
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        if (MCRSolrUtils.useNestedDocuments()) {
            solrDelete(obj.getId());
        }
        addObject(evt, obj);
    }

    @Override
    protected synchronized void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        solrDelete(obj.getId());
    }

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate derivate) {
        addObject(evt, derivate);
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate derivate) {
        if (MCRSolrUtils.useNestedDocuments()) {
            solrDelete(derivate.getId());
        }
        addObject(evt, derivate);
    }

    @Override
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate derivate) {
        if (MCRSolrUtils.useNestedDocuments()) {
            solrDelete(derivate.getId());
        }
        addObject(evt, derivate);
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate derivate) {
        deleteDerivate(derivate);
    }

    @Override
    protected void handlePathCreated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        addFile(path, attrs);
    }

    @Override
    protected void handlePathUpdated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        addFile(path, attrs);
    }

    @Override
    protected void updatePathIndex(MCREvent evt, Path file, BasicFileAttributes attrs) {
        addFile(file, attrs);
    }

    @Override
    protected void handlePathDeleted(MCREvent evt, Path file, BasicFileAttributes attrs) {
        removeFile(file);
    }

    @Override
    protected void updateDerivateFileIndex(MCREvent evt, MCRDerivate derivate) {
        MCRSessionMgr.getCurrentSession()
            .onCommit(() -> MCRSolrIndexer.rebuildContentIndex(Collections.singletonList(derivate.getId().toString()),
                MCRSolrIndexer.HIGH_PRIORITY));
    }

    @Override
    protected void handleObjectIndex(MCREvent evt, MCRObject obj) {
        handleObjectUpdated(evt, obj);
    }

    protected synchronized void addObject(MCREvent evt, MCRBase objectOrDerivate) {
        if (MCRMarkManager.instance().isMarkedForImport(objectOrDerivate.getId())) {
            return;
        }
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            long tStart = System.currentTimeMillis();
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Solr: submitting data of \"{}\" for indexing", objectOrDerivate.getId());
                }
                MCRContent content = (MCRContent) evt.get("content");
                if (content == null) {
                    content = new MCRBaseContent(objectOrDerivate);
                }
                MCRSolrIndexHandler indexHandler = MCRSolrIndexHandlerFactory.getInstance().getIndexHandler(content,
                    objectOrDerivate.getId());
                indexHandler.setCommitWithin(1000);
                MCRSolrIndexer.submitIndexHandler(indexHandler, MCRSolrIndexer.HIGH_PRIORITY);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Solr: submitting data of \"{}\" for indexing done in {}ms ", objectOrDerivate.getId(),
                        System.currentTimeMillis() - tStart);
                }
            } catch (Exception ex) {
                LOGGER.error("Error creating transfer thread for object {}", objectOrDerivate, ex);
            }
        });
    }

    protected synchronized void solrDelete(MCRObjectID id) {
        MCRSessionMgr.getCurrentSession().onCommit(() -> MCRSolrIndexer.deleteById(id.toString()));
    }

    protected synchronized void deleteDerivate(MCRDerivate derivate) {
        MCRSessionMgr.getCurrentSession().onCommit(() -> MCRSolrIndexer.deleteDerivate(derivate.getId().toString()));
    }

    protected synchronized void addFile(Path path, BasicFileAttributes attrs) {
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            try {
                MCRSolrIndexer.submitIndexHandler(MCRSolrIndexHandlerFactory.getInstance().getIndexHandler(path, attrs,
                    MCRSolrClientFactory.getSolrClient()), MCRSolrIndexer.HIGH_PRIORITY);
            } catch (Exception ex) {
                LOGGER.error("Error creating transfer thread for file {}", path, ex);
            }
        });
    }

    protected synchronized void removeFile(Path file) {
        if (isMarkedForDeletion(file)) {
            return;
        }
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            UpdateResponse updateResponse = MCRSolrIndexer.deleteById(file.toUri().toString());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Deleted file {}. Response:{}", file, updateResponse);
            }
        });
    }

    /**
     * Returns the derivate identifier for the given path.
     *
     * @param path the path
     * @return the derivate identifier
     */
    protected Optional<MCRObjectID> getDerivateId(Path path) {
        MCRPath mcrPath = MCRPath.toMCRPath(path);
        if (MCRObjectID.isValid(mcrPath.getOwner())) {
            return Optional.of(MCRObjectID.getInstance(mcrPath.getOwner()));
        }
        return Optional.empty();
    }

    /**
     * Checks if the derivate is marked for deletion.
     *
     * @param path the path to check
     */
    protected boolean isMarkedForDeletion(Path path) {
        return getDerivateId(path).map(MCRMarkManager.instance()::isMarkedForDeletion).orElse(false);
    }

}
