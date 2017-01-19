/*
 * $Id$
 * $Revision: 5697 $ $Date: Apr 16, 2013 $
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

package org.mycore.solr.index.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.document.MCRSolrInputDocumentFactory;
import org.mycore.solr.index.handlers.document.MCRSolrInputDocumentHandler;
import org.mycore.solr.index.handlers.document.MCRSolrInputDocumentsHandler;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrInputDocumentHandlerFactory extends MCRSolrIndexHandlerFactory {

    private static Logger LOGGER = LogManager.getLogger(MCRSolrInputDocumentHandlerFactory.class);

    /* (non-Javadoc)
     * @see org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory#getIndexHandler(org.mycore.common.content.MCRContent, org.mycore.datamodel.metadata.MCRObjectID)
     */
    @Override
    public MCRSolrIndexHandler getIndexHandler(MCRContent content, MCRObjectID id) {
        SolrInputDocument document = getDocument(id, content);
        MCRSolrIndexHandler indexHandler = new MCRSolrInputDocumentHandler(document);
        return indexHandler;
    }

    private SolrInputDocument getDocument(MCRObjectID id, MCRContent content) {
        SolrInputDocument document;
        try {
            document = MCRSolrInputDocumentFactory.getInstance().getDocument(id, content);
        } catch (SAXException | IOException e) {
            throw new MCRException(e);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(id + " results in: " + document);
        }
        return document;
    }

    /* (non-Javadoc)
     * @see org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory#getIndexHandler(java.util.Map)
     */
    @Override
    public MCRSolrIndexHandler getIndexHandler(Map<MCRObjectID, MCRContent> contentMap) {
        ArrayList<SolrInputDocument> documents = new ArrayList<>(contentMap.size());
        for (Map.Entry<MCRObjectID, MCRContent> entry : contentMap.entrySet()) {
            SolrInputDocument document = getDocument(entry.getKey(), entry.getValue());
            documents.add(document);
        }
        MCRSolrIndexHandler indexHandler = new MCRSolrInputDocumentsHandler(documents);
        return indexHandler;
    }

}
