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

package org.mycore.solr.index.handlers.stream;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.file.MCRSolrPathDocumentFactory;
import org.mycore.solr.index.handlers.MCRSolrAbstractIndexHandler;
import org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory;
import org.mycore.solr.index.handlers.document.MCRSolrInputDocumentsHandler;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;

/**
 * Commits <code>MCRFile</code> objects to solr, be aware that the files are
 * not indexed directly, but added to a list of sub index handlers.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrFilesIndexHandler extends MCRSolrAbstractIndexHandler {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrFilesIndexHandler.class);

    protected String mcrID;

    protected List<MCRSolrIndexHandler> subHandlerList;

    /**
     * Creates a new solr file index handler.
     * 
     * @param mcrID id of the derivate or mcrobject, if you put a mcrobject id here
     * all files of each derivate are indexed
     * @param solrClient where to index
     */
    public MCRSolrFilesIndexHandler(String mcrID, SolrClient solrClient) {
        super(solrClient);
        this.mcrID = mcrID;
        this.subHandlerList = new ArrayList<>();
    }

    @Override
    public void index() throws IOException {
        MCRObjectID mcrID = MCRObjectID.getInstance(getID());
        if (!MCRMetadataManager.exists(mcrID)) {
            LOGGER.warn("Unable to index '{}' cause it doesn't exists anymore!", mcrID);
            return;
        }
        if (mcrID.getTypeId().equals("derivate")) {
            indexDerivate(mcrID);
        } else {
            indexObject(mcrID);
        }
    }

    protected void indexDerivate(MCRObjectID derivateID) throws IOException {
        MCRPath rootPath = MCRPath.getPath(derivateID.toString(), "/");
        final MCRSolrIndexHandlerFactory ihf = MCRSolrIndexHandlerFactory.getInstance();
        final List<MCRSolrIndexHandler> subHandlerList = this.subHandlerList;
        final List<SolrInputDocument> docs = new ArrayList<>();
        final SolrClient solrClient = this.solrClient;
        Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                boolean sendContent = ihf.checkFile(file, attrs);
                try {
                    if (sendContent) {
                        subHandlerList.add(ihf.getIndexHandler(file, attrs, solrClient, true));
                    } else {
                        SolrInputDocument fileDoc = MCRSolrPathDocumentFactory.getInstance().getDocument(file, attrs);
                        docs.add(fileDoc);
                    }
                } catch (Exception ex) {
                    LOGGER.error("Error creating transfer thread", ex);
                }
                return super.visitFile(file, attrs);
            }

        });
        int fileCount = subHandlerList.size() + docs.size();
        LOGGER.info("Sending {} file(s) for derivate \"{}\"", fileCount, derivateID);
        if (!docs.isEmpty()) {
            MCRSolrInputDocumentsHandler subHandler = new MCRSolrInputDocumentsHandler(docs, solrClient);
            subHandler.setCommitWithin(getCommitWithin());
            this.subHandlerList.add(subHandler);
        }
    }

    protected void indexObject(MCRObjectID objectID) throws IOException {
        List<MCRObjectID> derivateIds = MCRMetadataManager.getDerivateIds(objectID, 0, TimeUnit.MILLISECONDS);
        for (MCRObjectID derivateID : derivateIds) {
            indexDerivate(derivateID);
        }
    }

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return this.subHandlerList;
    }

    public String getID() {
        return mcrID;
    }

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return new MCRSolrIndexStatistic("no index operation");
    }

    @Override
    public int getDocuments() {
        return 0;
    }

    @Override
    public String toString() {
        return "index files of " + this.mcrID;
    }

}
