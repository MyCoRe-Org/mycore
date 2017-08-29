/*
 * $Id$
 * $Revision: 5697 $ $Date: Apr 22, 2013 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.solr.index;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
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
import org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRSolrIndexEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrIndexEventHandler.class);

    @Override
    synchronized protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        addObject(evt, obj);
    }

    @Override
    synchronized protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        if (this.useNestedDocuments()) {
            solrDelete(obj.getId());
        }
        addObject(evt, obj);
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        if (this.useNestedDocuments()) {
            solrDelete(obj.getId());
        }
        addObject(evt, obj);
    }

    @Override
    synchronized protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        solrDelete(obj.getId());
    }

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate derivate) {
        addObject(evt, derivate);
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate derivate) {
        if (this.useNestedDocuments()) {
            solrDelete(derivate.getId());
        }
        addObject(evt, derivate);
    }

    @Override
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate derivate) {
        if (this.useNestedDocuments()) {
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
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            MCRSolrIndexer.rebuildContentIndex(Collections.singletonList(derivate.getId().toString()));
        });
    }

    @Override
    protected void handleObjectIndex(MCREvent evt, MCRObject obj) {
        handleObjectUpdated(evt, obj);
    }

    synchronized protected void addObject(MCREvent evt, MCRBase objectOrDerivate) {
        if (MCRMarkManager.instance().isMarkedForImport(objectOrDerivate.getId())) {
            return;
        }
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            long tStart = System.currentTimeMillis();
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER
                        .debug("Solr: submitting data of \"" + objectOrDerivate.getId().toString() + "\" for indexing");
                }
                MCRContent content = (MCRContent) evt.get("content");
                if (content == null) {
                    content = new MCRBaseContent(objectOrDerivate);
                }
                MCRSolrIndexHandler indexHandler = MCRSolrIndexHandlerFactory.getInstance().getIndexHandler(content,
                    objectOrDerivate.getId());
                indexHandler.setCommitWithin(1000);
                MCRSolrIndexer.submitIndexHandler(indexHandler);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Solr: submitting data of \"" + objectOrDerivate.getId().toString()
                        + "\" for indexing done in " + (System.currentTimeMillis() - tStart) + "ms ");
                }
            } catch (Exception ex) {
                LOGGER.error("Error creating transfer thread for object " + objectOrDerivate, ex);
            }
        });
    }

    synchronized protected void solrDelete(MCRObjectID id) {
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            MCRSolrIndexer.deleteById(id.toString());
        });
    }

    synchronized protected void deleteDerivate(MCRDerivate derivate) {
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            MCRSolrIndexer.deleteDerivate(derivate.getId().toString());
        });
    }

    synchronized protected void addFile(Path path, BasicFileAttributes attrs) {
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            try {
                MCRSolrIndexer.submitIndexHandler(MCRSolrIndexHandlerFactory.getInstance().getIndexHandler(path, attrs,
                    MCRSolrClientFactory.getSolrClient()));
            } catch (Exception ex) {
                LOGGER.error("Error creating transfer thread for file " + path.toString(), ex);
            }
        });
    }

    synchronized protected void removeFile(Path file) {
        if (isMarkedForDeletion(file)) {
            return;
        }
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            UpdateResponse updateResponse = MCRSolrIndexer.deleteById(file.toUri().toString());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Deleted file " + file + ". Response:" + updateResponse);
            }
        });
    }

    /**
     * Checks if the application uses nested documents. If so, each reindex requires
     * an extra deletion. Using nested documents slows the solr index performance.
     * 
     * @return true if nested documents are used, otherwise false
     */
    protected boolean useNestedDocuments() {
        return MCRConfiguration.instance().getBoolean("MCR.Module-solr.NestedDocuments", true);
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
