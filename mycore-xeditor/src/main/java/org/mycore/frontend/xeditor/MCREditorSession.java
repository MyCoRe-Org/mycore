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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.frontend.xeditor.tracker.MCRBreakpoint;
import org.mycore.frontend.xeditor.tracker.MCRChangeTracker;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREditorSession {

    private final static Logger LOGGER = Logger.getLogger(MCREditorSession.class);

    private String id;

    private Map<String, String[]> requestParameters;

    private String cancelURL;

    private Document editedXML;

    private MCRChangeTracker tracker = new MCRChangeTracker();

    private MCREditorSubmission submission = new MCREditorSubmission(this);

    private MCRXEditorValidator validator = new MCRXEditorValidator();

    private MCRXMLCleaner cleaner = new MCRXMLCleaner();

    private MCRXEditorPostProcessor postProcessor = new MCRXEditorPostProcessor();

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

    public Document getEditedXML() {
        return editedXML;
    }

    public void setEditedXML(Document editedXML) throws JDOMException {
        this.editedXML = editedXML;
        MCRUsedNamespaces.addNamespacesFrom(editedXML.getRootElement());
    }

    public MCRBinding getRootBinding() throws JDOMException {
        return new MCRBinding(editedXML, tracker);
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
