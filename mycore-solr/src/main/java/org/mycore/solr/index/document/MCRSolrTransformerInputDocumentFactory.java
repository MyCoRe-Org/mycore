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

package org.mycore.solr.index.document;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.solr.common.SolrInputDocument;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.content.transformer.MCRXSL2JAXBTransformer;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.index.document.jaxb.MCRSolrInputDocumentList;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrTransformerInputDocumentFactory extends MCRSolrInputDocumentFactory {

    //    private static MCRXSL2JAXBTransformer<JAXBElement<MCRSolrInputDocument>> transformer = getTransformer();
    private static MCRContentTransformer transformer = getTransformer();

    private static boolean isJAXBTransformer;

    /* (non-Javadoc)
     * @see org.mycore.solr.index.document.MCRSolrInputDocumentFactory#getDocument(org.mycore.datamodel.metadata.MCRObjectID, org.mycore.common.content.MCRContent)
     */
    @Override
    public SolrInputDocument getDocument(MCRObjectID id, MCRContent content) throws SAXException, IOException {
        //we need no parameter for searchfields - hopefully
        try {
            SolrInputDocument document;
            if (isJAXBTransformer) {
                MCRParameterCollector param = new MCRParameterCollector();
                @SuppressWarnings("unchecked")
                MCRXSL2JAXBTransformer<MCRSolrInputDocumentList> jaxbTransformer = (MCRXSL2JAXBTransformer<MCRSolrInputDocumentList>) transformer;
                MCRSolrInputDocumentList input = jaxbTransformer.getJAXBObject(content, param);
                document = MCRSolrInputDocumentGenerator.getSolrInputDocument(input.getDoc().iterator().next());
            } else {
                MCRContent result = transformer.transform(content);
                document = MCRSolrInputDocumentGenerator.getSolrInputDocument(result.asXML().getRootElement());
            }
            return document;
        } catch (TransformerConfigurationException | JAXBException | JDOMException | ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    private static MCRContentTransformer getTransformer() {
        String property = CONFIG_PREFIX + "SolrInputDocument.Transformer";
        String transformerId = MCRConfiguration.instance().getString(property);
        MCRContentTransformer contentTransformer = MCRContentTransformerFactory.getTransformer(transformerId);
        isJAXBTransformer = contentTransformer instanceof MCRXSL2JAXBTransformer;
        return contentTransformer;
    }

    @Override
    public Iterator<SolrInputDocument> getDocuments(Map<MCRObjectID, MCRContent> contentMap) throws IOException,
        SAXException {
        if (contentMap.isEmpty()) {
            return Collections.emptyIterator();
        }
        try {
            Document doc = getMergedDocument(contentMap);
            if (isJAXBTransformer) {
                MCRParameterCollector param = new MCRParameterCollector();
                @SuppressWarnings("unchecked")
                MCRXSL2JAXBTransformer<MCRSolrInputDocumentList> jaxbTransformer = (MCRXSL2JAXBTransformer<MCRSolrInputDocumentList>) transformer;
                MCRSolrInputDocumentList input = jaxbTransformer.getJAXBObject(new MCRJDOMContent(doc), param);
                return MCRSolrInputDocumentGenerator.getSolrInputDocuments(input.getDoc()).iterator();
            } else {
                MCRContent result = transformer.transform(new MCRJDOMContent(doc));
                return getSolrInputDocuments(result);
            }
        } catch (TransformerConfigurationException | JAXBException | JDOMException | ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    private Iterator<SolrInputDocument> getSolrInputDocuments(MCRContent result) throws IOException, SAXException,
        JDOMException {
        final Iterator<Element> delegate;
        delegate = result.asXML().getRootElement().getChildren("doc").iterator();
        return new Iterator<SolrInputDocument>() {

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public SolrInputDocument next() {
                return MCRSolrInputDocumentGenerator.getSolrInputDocument(delegate
                    .next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove() is not supported on this iterator.");
            }
        };
    }

    private Document getMergedDocument(Map<MCRObjectID, MCRContent> contentMap) throws IOException, SAXException,
        JDOMException {
        Element rootElement = new Element("add");
        Document doc = new Document(rootElement);
        for (Map.Entry<MCRObjectID, MCRContent> entry : contentMap.entrySet()) {
            rootElement.addContent(entry.getValue().asXML().detachRootElement());
        }
        return doc;
    }

}
