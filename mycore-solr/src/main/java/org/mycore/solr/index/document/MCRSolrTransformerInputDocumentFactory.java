/*
 * $Id$
 * $Revision: 5697 $ $Date: Apr 17, 2013 $
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

package org.mycore.solr.index.document;

import java.io.IOException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.content.transformer.MCRXSL2JAXBTransformer;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.index.document.jaxb.MCRSolrInputDocument;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrTransformerInputDocumentFactory extends MCRSolrInputDocumentFactory {

    private static MCRXSL2JAXBTransformer<JAXBElement<MCRSolrInputDocument>> transformer = getTransformer();

    /* (non-Javadoc)
     * @see org.mycore.solr.index.document.MCRSolrInputDocumentFactory#getDocument(org.mycore.datamodel.metadata.MCRObjectID, org.mycore.common.content.MCRContent)
     */
    @Override
    public SolrInputDocument getDocument(MCRObjectID id, MCRContent content) throws SAXException, IOException {
        MCRParameterCollector param = MCRParameterCollector.getInstanceFromUserSession();
        try {
            MCRSolrInputDocument input = transformer.getJAXBObject(content, param).getValue();
            SolrInputDocument document = MCRSolrInputDocumentGenerator.getSolrInputDocument(input);
            return document;
        } catch (TransformerConfigurationException | JAXBException e) {
            throw new IOException(e);
        }
    }

    private static MCRXSL2JAXBTransformer<JAXBElement<MCRSolrInputDocument>> getTransformer() {
        String property = "MCR.Module-solr.SolrInputDocumentTransformer";
        String transformerId = MCRConfiguration.instance().getString(property);
        MCRContentTransformer contentTransformer = MCRContentTransformerFactory.getTransformer(transformerId);
        if (!(contentTransformer instanceof MCRXSL2JAXBTransformer)) {
            throw new MCRConfigurationException(property + ".Class does not define an instance of "
                + MCRXSL2JAXBTransformer.class.getCanonicalName());
        }
        @SuppressWarnings("unchecked")
        MCRXSL2JAXBTransformer<JAXBElement<MCRSolrInputDocument>> jaxbTransformer = (MCRXSL2JAXBTransformer<JAXBElement<MCRSolrInputDocument>>) contentTransformer;
        return jaxbTransformer;
    }

}
