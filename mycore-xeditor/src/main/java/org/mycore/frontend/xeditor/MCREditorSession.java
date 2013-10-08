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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.Verifier;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.frontend.xeditor.tracker.MCRBreakpoint;
import org.mycore.frontend.xeditor.tracker.MCRChangeTracker;
import org.xml.sax.SAXException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREditorSession {

    private final static Logger LOGGER = Logger.getLogger(MCREditorSession.class);

    private String id;

    private Map<String, String[]> requestParameters;

    private Map<String, Object> variables;

    private String cancelURL;

    private Document editedXML;

    private MCRChangeTracker tracker = new MCRChangeTracker();

    private MCREditorSubmission submission = new MCREditorSubmission(this);

    private MCRXEditorValidator validator = new MCRXEditorValidator();

    private MCRXMLCleaner cleaner = new MCRXMLCleaner();

    private MCRXEditorPostProcessor postProcessor = new MCRXEditorPostProcessor();

    public MCREditorSession(Map<String, String[]> requestParameters, MCRParameterCollector collector) {
        this.requestParameters = requestParameters;
        this.variables = collector.getParameterMap();
        removeIllegalVariables();
    }

    public MCREditorSession() {
        this(Collections.<String, String[]> emptyMap(), new MCRParameterCollector());
    }

    private void removeIllegalVariables() {
        for (Iterator<Entry<String, Object>> entries = variables.entrySet().iterator(); entries.hasNext();) {
            String name = entries.next().getKey();
            String result = Verifier.checkXMLName(name);
            if (result != null) {
                LOGGER.warn("Illegally named transformation parameter, removing " + name);
                entries.remove();
            }
        }
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getID() {
        return id;
    }

    public String getCombinedSessionStepID() {
        setBreakpoint("After transformation to HTML");
        return id + "-" + tracker.getChangeCounter();
    }

    public Map<String, String[]> getRequestParameters() {
        return requestParameters;
    }

    public String getCancelURL() {
        return cancelURL;
    }

    public void setCancelURL(String cancelURL) {
        if (this.cancelURL != null)
            return;

        cancelURL = replaceParameters(cancelURL);
        if (!cancelURL.contains("{")) {
            LOGGER.debug(id + " set cancel URL to " + cancelURL);
            this.cancelURL = cancelURL;
        }
    }

    private final static Pattern PATTERN_URI = Pattern.compile("\\{\\$([^\\}]+)\\}");

    public String replaceParameters(String uri) {
        Matcher m = PATTERN_URI.matcher(uri);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String token = m.group(1);
            Object value = variables.get(token);
            m.appendReplacement(sb, value == null ? m.group().replace("$", "\\$") : value.toString());
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public Document getEditedXML() {
        return editedXML;
    }

    public void setEditedXML(Document editedXML) throws JDOMException {
        this.editedXML = editedXML;
        MCRUsedNamespaces.addNamespacesFrom(editedXML.getRootElement());
    }

    public void setEditedXML(String uri) throws JDOMException, IOException, SAXException, TransformerException {
        if ((editedXML != null) || (uri = replaceParameters(uri)).contains("{"))
            return;

        LOGGER.info("Reading edited XML from " + uri);
        Document xml = MCRSourceContent.getInstance(uri).asXML();
        setEditedXML(xml);
        setBreakpoint("Reading XML from " + uri);
    }

    public MCRBinding getRootBinding() throws JDOMException {
        MCRBinding binding = new MCRBinding(editedXML, tracker);
        binding.setVariables(variables);
        return binding;
    }

    public void setBreakpoint(String msg) {
        if (editedXML != null)
            tracker.track(MCRBreakpoint.setBreakpoint(editedXML.getRootElement(), msg));
    }

    public MCRChangeTracker getChangeTracker() {
        return tracker;
    }

    public MCREditorSubmission getSubmission() {
        return submission;
    }

    public MCRXEditorValidator getValidator() {
        return validator;
    }

    public MCRXMLCleaner getXMLCleaner() {
        return cleaner;
    }

    public MCRXEditorPostProcessor getPostProcessor() {
        return postProcessor;
    }
}
