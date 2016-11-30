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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.cs.MCRSolrBulkXMLStream;
import org.mycore.solr.index.cs.MCRSolrContentStream;
import org.mycore.solr.index.handlers.stream.MCRSolrBulkXMLIndexHandler;
import org.mycore.solr.index.handlers.stream.MCRSolrDefaultIndexHandler;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrContentStreamHandlerFactory extends MCRSolrIndexHandlerFactory {

    private static final Logger LOGGER = Logger.getLogger(MCRSolrContentStreamHandlerFactory.class);

    /* (non-Javadoc)
     * @see org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory#getIndexHandler(org.mycore.common.content.MCRContent, org.mycore.datamodel.metadata.MCRObjectID)
     */
    @Override
    public MCRSolrIndexHandler getIndexHandler(MCRContent content, MCRObjectID id) {
        MCRSolrContentStream contentStream = new MCRSolrContentStream(id.toString(), content);
        MCRSolrDefaultIndexHandler indexHandler = new MCRSolrDefaultIndexHandler(contentStream);
        return indexHandler;
    }

    @Override
    public MCRSolrIndexHandler getIndexHandler(Map<MCRObjectID, MCRContent> contentMap) {
        MCRSolrBulkXMLStream contentStream = new MCRSolrBulkXMLStream("MCRSolrObjs");
        List<Element> elementList = contentStream.getList();
        for (Map.Entry<MCRObjectID, MCRContent> entry : contentMap.entrySet()) {
            LOGGER.info("Submitting data of \"" + entry.getKey() + "\" for indexing");
            Document mcrObjXML;
            try {
                mcrObjXML = entry.getValue().asXML();
                elementList.add(mcrObjXML.getRootElement().detach());
            } catch (JDOMException | IOException | SAXException e) {
                LOGGER.error("Error while parsing content for id: " + entry.getKey(), e);
            }
        }
        MCRSolrBulkXMLIndexHandler indexHandler = new MCRSolrBulkXMLIndexHandler(contentStream);
        return indexHandler;
    }

}
