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

package org.mycore.solr.index.document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.solr.index.document.jaxb.MCRSolrInputDocument;
import org.mycore.solr.index.document.jaxb.MCRSolrInputDocumentList;
import org.mycore.solr.index.document.jaxb.MCRSolrInputField;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrInputDocumentGenerator {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final JAXBContext JAXB_CONTEXT = initContext();

    private static JAXBContext initContext() {
        try {
            return JAXBContext.newInstance(MCRSolrInputDocument.class.getPackage().getName(),
                Thread.currentThread().getContextClassLoader());
        } catch (JAXBException e) {
            throw new MCRException("Could not instantiate JAXBContext.", e);
        }
    }

    public static SolrInputDocument getSolrInputDocument(MCRSolrInputDocument jaxbDoc) {
        SolrInputDocument doc = new SolrInputDocument();
        Set<MCRSolrInputField> duplicateFilter = new HashSet<>();
        for (Object o : jaxbDoc.getFieldOrDoc()) {
            if (o instanceof MCRSolrInputField field) {
                if (field.getValue().isEmpty() || duplicateFilter.contains(field)) {
                    continue;
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("adding {}={}", field.getName(), field.getValue());
                }
                duplicateFilter.add(field);
                doc.addField(field.getName(), field.getValue());
            } else if (o instanceof MCRSolrInputDocument child) {
                SolrInputDocument solrChild = getSolrInputDocument(child);
                doc.addChildDocument(solrChild);
            }
        }
        return doc;
    }

    public static List<SolrInputDocument> getSolrInputDocument(MCRContent source) throws JAXBException, IOException {
        if (source instanceof MCRJAXBContent) {
            @SuppressWarnings("unchecked")
            MCRJAXBContent<MCRSolrInputDocumentList> jaxbContent = (MCRJAXBContent<MCRSolrInputDocumentList>) source;
            return getSolrInputDocuments(jaxbContent.getObject().getDoc());
        }
        Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
        MCRSolrInputDocumentList solrDocuments = (MCRSolrInputDocumentList) unmarshaller.unmarshal(source.getSource());
        return getSolrInputDocuments(solrDocuments.getDoc());
    }

    public static List<SolrInputDocument> getSolrInputDocuments(List<MCRSolrInputDocument> inputDocuments) {
        List<SolrInputDocument> returnList = new ArrayList<>(inputDocuments.size());
        for (MCRSolrInputDocument doc : inputDocuments) {
            returnList.add(getSolrInputDocument(doc));
        }
        return returnList;
    }

    public static SolrInputDocument getSolrInputDocument(Element input) {
        SolrInputDocument doc = new SolrInputDocument();
        Set<MCRSolrInputField> duplicateFilter = new HashSet<>();
        List<Element> fieldElements = input.getChildren("field");
        for (Element fieldElement : fieldElements) {
            MCRSolrInputField field = new MCRSolrInputField();
            field.setName(fieldElement.getAttributeValue("name"));
            field.setValue(fieldElement.getText());
            if (field.getValue().isEmpty() || duplicateFilter.contains(field)) {
                continue;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("adding {}={}", field.getName(), field.getValue());
            }
            duplicateFilter.add(field);
            doc.addField(field.getName(), field.getValue());
        }
        List<Element> docElements = input.getChildren("doc");
        for (Element child : docElements) {
            SolrInputDocument solrChild = getSolrInputDocument(child);
            doc.addChildDocument(solrChild);
        }
        return doc;
    }
}
