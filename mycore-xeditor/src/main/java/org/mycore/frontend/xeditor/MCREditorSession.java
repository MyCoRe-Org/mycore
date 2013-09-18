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
import org.jaxen.JaxenException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRXSL2XMLTransformer;
import org.xml.sax.SAXException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREditorSession {

    private final static Logger LOGGER = Logger.getLogger(MCREditorSession.class);

    private String id;

    private Map<String, String[]> requestParameters;

    private String cancelURL;

    private String postProcessorXSL;

    private List<MCREditorStep> steps = new ArrayList<MCREditorStep>();

    private MCRXMLCleaner cleaner = new MCRXMLCleaner();

    private MCRXEditorValidator validator = new MCRXEditorValidator();

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

    public void setPostProcessorXSL(String stylesheet) {
        this.postProcessorXSL = stylesheet;
    }

    public Document getPostProcessedXML() throws IOException, JDOMException, SAXException {
        if (postProcessorXSL == null)
            return getCurrentStep().getDocument();

        MCRContent source = new MCRJDOMContent(getCurrentStep().getDocument());
        MCRContent transformed = MCRXSL2XMLTransformer.getInstance("xsl/" + postProcessorXSL).transform(source);
        return transformed.asXML();
    }

    public void setInitialStep(MCREditorStep step) {
        steps.clear();
        steps.add(step);
    }

    public MCREditorStep getCurrentStep() {
        return steps.isEmpty() ? null : steps.get(steps.size() - 1);
    }

    public String getCombinedSessionStepID() {
        return id + "-" + steps.size();
    }

    public List<MCREditorStep> getSteps() {
        return steps;
    }

    public MCREditorStep startNextStepFrom(int stepID) {
        if (stepID < steps.size()) {
            LOGGER.info("Detected resubmission of old editor step, going back in time now...");
            steps = steps.subList(0, stepID); // Forget all following steps
        }

        MCREditorStep nextStep = getCurrentStep().clone();
        steps.add(nextStep);
        return nextStep;
    }

    public MCRXMLCleaner getXMLCleaner() {
        return cleaner;
    }

    public MCRXEditorValidator getValidator() {
        return validator;
    }

    public MCRXEditorValidator validate() throws JDOMException, JaxenException {
        validator.validate(getCurrentStep().getDocument());
        return validator;
    }
}
