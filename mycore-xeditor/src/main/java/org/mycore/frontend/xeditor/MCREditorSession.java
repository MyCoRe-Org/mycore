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

package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Verifier;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.frontend.xeditor.tracker.MCRBreakpoint;
import org.mycore.frontend.xeditor.tracker.MCRChangeTracker;
import org.mycore.frontend.xeditor.validation.MCRXEditorValidator;

/**
 * @author Frank Lützenkirchen
 */
public class MCREditorSession {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Pattern PATTERN_URI = Pattern.compile("\\{\\$([^\\}]+)\\}");

    private String id;

    private String url;

    private Map<String, String[]> requestParameters = new HashMap<>();

    private Map<String, Object> variables;

    private String cancelURL;

    private Document editedXML;

    private MCRChangeTracker tracker = new MCRChangeTracker();

    private MCREditorSubmission submission = new MCREditorSubmission(this);

    private MCRXEditorValidator validator = new MCRXEditorValidator(this);

    private MCRXMLCleaner cleaner = new MCRXMLCleaner();

    private MCRXEditorPostProcessor postProcessor = getDefaultPostProcessorImplementation();

    private static MCRPostProcessorXSL getDefaultPostProcessorImplementation() {
        return MCRConfiguration2.getInstanceOfOrThrow(MCRPostProcessorXSL.class, "MCR.XEditor.PostProcessor.Default");
    }

    public MCREditorSession(Map<String, String[]> requestParameters, MCRParameterCollector collector) {
        // make a copy, the original may be re-used by servlet container
        this.requestParameters.putAll(requestParameters);
        this.variables = new HashMap<>(collector.getParameterMap());
        removeIllegalVariables();
    }

    public MCREditorSession() {
        this(Collections.emptyMap(), new MCRParameterCollector());
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void update(MCRParameterCollector collector) {
        String currentLang = collector.getParameter("CurrentLang", null);
        if (currentLang != null) {
            variables.put("CurrentLang", currentLang);
        }
    }

    private void removeIllegalVariables() {
        for (Iterator<Entry<String, Object>> entries = variables.entrySet().iterator(); entries.hasNext();) {
            String name = entries.next().getKey();
            String result = Verifier.checkXMLName(name);
            if (result != null) {
                LOGGER.warn("Illegally named transformation parameter, removing {}", name);
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
        return id + "-" + tracker.getChangeCounter();
    }

    public void setPageURL(String pageURL) {
        if (url == null) {
            this.url = pageURL.contains("?") ? pageURL.substring(0, pageURL.indexOf('?')) : pageURL;
            LOGGER.debug("Editor page URL set to {}", this.url);
        }
    }

    public String getPageURL() {
        return this.url;
    }

    public String getRedirectURL(String anchor) {
        return url + "?" + MCREditorSessionStore.XEDITOR_SESSION_PARAM + "=" + id
            + (anchor != null ? "#" + anchor : "");
    }

    public Map<String, String[]> getRequestParameters() {
        return requestParameters;
    }

    public String getCancelURL() {
        return cancelURL;
    }

    public void setCancelURL(String cancelURL) {
        if (this.cancelURL != null) {
            return;
        }

        String cURL = replaceParameters(cancelURL);
        if (!cURL.contains("{")) {
            LOGGER.debug("{} set cancel URL to {}", id, cURL);
            this.cancelURL = cURL;
        }
    }

    public String replaceParameters(String uri) {
        Matcher m = PATTERN_URI.matcher(uri);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, getReplacement(m.group(1)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String getReplacement(String token) {
        Object value = variables.get(token);
        if (value == null) {
            return "{\\$" + token + "}";
        } else {
            return Matcher.quoteReplacement(value.toString());
        }
    }

    public Document getEditedXML() {
        return editedXML;
    }

    public void setEditedXML(Document editedXML) {
        this.editedXML = editedXML;
        addNamespacesFrom(editedXML.getRootElement());
    }

    private void addNamespacesFrom(Element element) {
        MCRConstants.registerNamespace(element.getNamespace());
        for (Namespace ns : element.getAdditionalNamespaces()) {
            MCRConstants.registerNamespace(ns);
        }
        for (Element child : element.getChildren()) {
            addNamespacesFrom(child);
        }
    }

    public void setEditedXML(String uri) throws JDOMException, IOException, TransformerException {
        String uriRe = replaceParameters(uri);
        if ((editedXML != null) || uriRe.contains("{")) {
            return;
        }

        LOGGER.info("Reading edited XML from {}", uriRe);
        Document xml = MCRSourceContent.createInstance(uriRe).asXML();
        setEditedXML(xml);
        setBreakpoint("Reading XML from " + uriRe);
    }

    public MCRBinding getRootBinding() {
        MCRBinding binding = new MCRBinding(editedXML, tracker);
        binding.setVariables(variables);
        return binding;
    }

    public void setBreakpoint(String msg) {
        if (editedXML != null) {
            tracker.track(MCRBreakpoint.setBreakpoint(editedXML.getRootElement(), msg));
        }
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

    protected void setPostProcessor(MCRXEditorPostProcessor postProcessor) {
        this.postProcessor = postProcessor;
    }

}
