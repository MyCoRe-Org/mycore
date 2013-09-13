/*
 * $Revision$ 
 * $Date$
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

package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.content.transformer.MCRXSL2XMLTransformer;
import org.xml.sax.SAXException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREditorSession {

    private final static Logger LOGGER = Logger.getLogger(MCREditorSession.class);

    private String id;

    private Map<String, String[]> requestParameters;

    private Document editedXML;

    private Set<String> xPathsOfDisplayedFields = new HashSet<String>();

    private String sourceURI;

    private String cancelURL;

    private String postProcessorXSL;

    private MCRXEditorValidator validator = new MCRXEditorValidator();

    private MCRXMLCleaner cleaner;

    public MCREditorSession(Map<String, String[]> requestParameters) {
        this.requestParameters = requestParameters;
    }

    public MCREditorSession() {
        this(new HashMap<String, String[]>());
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getID() {
        return id;
    }

    public void setEditedXML(Document xml) throws JDOMException {
        if (editedXML == null) {
            editedXML = xml;
            MCRUsedNamespaces.addNamespacesFrom(editedXML.getRootElement());
            cleaner = new MCRXMLCleaner(editedXML);
        }
    }

    public void setEditedXML(String uri) throws JDOMException, IOException, SAXException, TransformerException {
        if (editedXML == null) {
            LOGGER.info(id + " reading edited XML from " + uri);
            sourceURI = uri;
            setEditedXML(MCRSourceContent.getInstance(uri).asXML());
        }
    }

    public Document getEditedXML() {
        return editedXML;
    }

    public void setPostProcessorXSL(String stylesheet) {
        this.postProcessorXSL = stylesheet;
    }

    public Document getPostProcessedXML() throws IOException, JDOMException, SAXException {
        if (postProcessorXSL == null)
            return editedXML;

        MCRContent source = new MCRJDOMContent(editedXML);
        MCRContent transformed = MCRXSL2XMLTransformer.getInstance("xsl/" + postProcessorXSL).transform(source);
        return transformed.asXML();
    }

    public MCRXMLCleaner getXMLCleaner() {
        return cleaner;
    }

    public String getSourceURI() {
        return sourceURI;
    }

    public Map<String, String[]> getRequestParameters() {
        return requestParameters;
    }

    public String getCancelURL() {
        return cancelURL;
    }

    public void setCancelURL(String cancelURL) {
        if (cancelURL == null) {
            LOGGER.debug(id + " set cancel URL to " + cancelURL);
            this.cancelURL = cancelURL;
        }
    }

    public void markAsTransformedToInputField(Object node) {
        String xPath = MCRXPathBuilder.buildXPath(node);
        LOGGER.debug(id + " uses " + xPath);
        xPathsOfDisplayedFields.add(xPath);
    }

    private void markAsResubmittedFromInputField(Object node) {
        String xPath = MCRXPathBuilder.buildXPath(node);
        LOGGER.debug(id + " set value of " + xPath);
        xPathsOfDisplayedFields.remove(xPath);
    }

    public void setSubmittedValues(String xPath, String[] values) throws JDOMException, ParseException {
        MCRBinding rootBinding = new MCRBinding(editedXML);
        MCRBinding binding = new MCRBinding(xPath, rootBinding);
        List<Object> boundNodes = binding.getBoundNodes();

        while (boundNodes.size() < values.length) {
            Element newElement = binding.cloneBoundElement(boundNodes.size() - 1);
            markAsTransformedToInputField(newElement);
        }

        for (int i = 0; i < values.length; i++) {
            String value = values[i] == null ? "" : values[i].trim();
            binding.setValue(i, value);
            if (!value.isEmpty())
                markAsResubmittedFromInputField(boundNodes.get(i));
        }
    }

    public void removeDeletedNodes() throws JDOMException, ParseException {
        MCRBinding root = new MCRBinding(editedXML);
        for (String xPath : xPathsOfDisplayedFields)
            new MCRBinding(xPath, root).detachBoundNodes();

        forgetDisplayedFields();
    }

    public void forgetDisplayedFields() {
        xPathsOfDisplayedFields.clear();
    }

    public MCRXEditorValidator getValidator() {
        return validator;
    }

    public MCRXEditorValidator validate() throws JDOMException, ParseException {
        validator.validate(editedXML);
        return validator;
    }
}
