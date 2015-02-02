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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.solr.MCRSolrServerFactory;
import org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRSolrIndexEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrIndexEventHandler.class);

    @Override
    synchronized protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        this.handleMCRBaseCreated(evt, obj);
    }

    @Override
    synchronized protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        //need to delete to handle nested documents
        this.handleObjectDeleted(evt, obj);
        this.handleMCRBaseCreated(evt, obj);
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        //need to delete to handle nested documents
        this.handleObjectDeleted(evt, obj);
        this.handleMCRBaseCreated(evt, obj);
    }

    @Override
    synchronized protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        MCRSolrIndexer.deleteById(obj.getId()
            .toString());
    }

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate derivate) {
        this.handleMCRBaseCreated(evt, derivate);
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate derivate) {
        //need to delete to handle nested documents
        this.handleDerivateDeleted(evt, derivate);
        this.handleMCRBaseCreated(evt, derivate);
    }

    @Override
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate derivate) {
        //need to delete to handle nested documents
        this.handleDerivateDeleted(evt, derivate);
        this.handleMCRBaseCreated(evt, derivate);
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate derivate) {
        MCRSolrIndexer.deleteById(derivate.getId().toString());
    }

    synchronized protected void handleMCRBaseCreated(MCREvent evt, MCRBase objectOrDerivate) {
        long tStart = System.currentTimeMillis();
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Solr: submitting data of \"" + objectOrDerivate.getId()
                    .toString() + "\" for indexing");
            }
            MCRContent content = (MCRContent) evt.get("content");
            if (content == null) {
                content = new MCRBaseContent(objectOrDerivate);
            }
            MCRSolrIndexHandler indexHandler = MCRSolrIndexHandlerFactory.getInstance()
                .getIndexHandler(content, objectOrDerivate.getId());
            indexHandler.setCommitWithin(1000);
            MCRSolrIndexer.submitIndexHandler(indexHandler, 10);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Solr: submitting data of \"" + objectOrDerivate.getId()
                    .toString() + "\" for indexing done in " + (System.currentTimeMillis() - tStart) + "ms ");
            }
        } catch (Exception ex) {
            LOGGER.error("Error creating transfer thread for object " + objectOrDerivate, ex);
        }
    }

    @Override
    protected void handlePathCreated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        try {
            BasicFileAttributeView fileAttributeView = Files.getFileAttributeView(path, BasicFileAttributeView.class);
            MCRSolrIndexer.submitIndexHandler(MCRSolrIndexHandlerFactory.getInstance()
                .getIndexHandler(path, fileAttributeView.readAttributes(), MCRSolrServerFactory.getSolrServer()));
        } catch (Exception ex) {
            LOGGER.error("Error creating transfer thread for file " + path.toString(), ex);
        }
    }

    @Override
    protected void handlePathUpdated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        this.handlePathCreated(evt, path, attrs);
    }

    @Override
    protected void handlePathDeleted(MCREvent evt, Path file, BasicFileAttributes attrs) {
        UpdateResponse updateResponse = MCRSolrIndexer.deleteById(file.toUri()
            .toString());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Deleted file " + file + ". Response:" + updateResponse);
        }
    }

    @Override
    protected void updatePathIndex(MCREvent evt, Path file, BasicFileAttributes attrs) {
        handlePathCreated(evt, file, attrs);
    }

    @Override
    protected void updateDerivateFileIndex(MCREvent evt, MCRDerivate derivate) {
        MCRSolrIndexer.rebuildContentIndex(Arrays.asList(derivate.getId()
            .toString()));
    }

    @Override
    protected void handleObjectIndex(MCREvent evt, MCRObject obj) {
        handleObjectUpdated(evt, obj);
    }
}
