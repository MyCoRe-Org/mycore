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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.cs.MCRSolrBulkXMLStream;

/**
 * This class index a {@link MCRSolrBulkXMLStream}. The stream contains a list of xml elements (mycore objects)
 * which are indexed together. If one element couldn't be created (mycore server side), a fallback mechanism is implemented
 * to index in single threads.
 *
 * @author Matthias Eichner
 */
public class MCRSolrBulkXMLIndexHandler extends MCRSolrObjectStreamIndexHandler {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrBulkXMLIndexHandler.class);

    private Map<MCRObjectID, MCRContent> contentMap;

    protected List<MCRSolrIndexHandler> fallBackList;

    public MCRSolrBulkXMLIndexHandler(Map<MCRObjectID, MCRContent> contentMap) {
        this(contentMap, MCRSolrClientFactory.getSolrClient());
    }

    public MCRSolrBulkXMLIndexHandler(Map<MCRObjectID, MCRContent> contentMap, SolrClient solrClient) {
        super(solrClient);
        this.contentMap = contentMap;
        this.fallBackList = new ArrayList<>();
    }

    public MCRSolrBulkXMLStream getStream() {
        MCRSolrBulkXMLStream contentStream = new MCRSolrBulkXMLStream("MCRSolrObjs");
        List<Element> elementList = contentStream.getList();
        // filter and reassign content map (reassign for toString() method)
        contentMap = contentMap.entrySet().stream().filter(entry -> {
            MCRObjectID id = entry.getKey();
            boolean exists = MCRMetadataManager.exists(id);
            LOGGER.info(exists ? "Submitting data of \"" + id + "\" for indexing"
                : "Cannot submit \"" + id + "\" cause it does not exists anymore.");
            return exists;
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        contentMap.forEach((id, content) -> {
            try {
                Document mcrObjXML = content.asXML();
                elementList.add(mcrObjXML.getRootElement().detach());
            } catch (Exception e) {
                LOGGER.error("Cannot submit \"{}\" cause content couldn't be parsed.", id, e);
            }
        });
        return contentStream;
    }

    @Override
    public void index() throws IOException, SolrServerException {
        try {
            super.index();
        } catch (Exception exc) {
            // some index stuff failed on mycore side, try to index items in single threads
            contentMap.forEach((id, content) -> {
                MCRSolrIndexHandler indexHandler = new MCRSolrSingleObjectStreamIndexHandler(id, content);
                indexHandler.setCommitWithin(getCommitWithin());
                this.fallBackList.add(indexHandler);
            });
        }
    }

    @Override
    public List<MCRSolrIndexHandler> getSubHandlers() {
        return this.fallBackList;
    }

    @Override
    public int getDocuments() {
        return contentMap.size();
    }

    @Override
    public String toString() {
        return "bulk index " + contentMap.size() + " documents";
    }

}
