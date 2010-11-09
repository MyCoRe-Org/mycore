package org.mycore.frontend.editor;

import org.jdom.Element;
import org.mycore.frontend.servlets.MCRServlet;

public class MCREditorSession {

    private String id;

    private Element xml;

    private String cancelURL;

    private String sourceURI;

    private MCRParameters parameters;

    public MCREditorSession(Element xml, MCRParameters parameters) {
        this.xml = xml;
        this.parameters = parameters;

        buildAndSetID();
        buildSourceURIandCancelURL();
    }

    private void buildSourceURIandCancelURL() {
        MCRAttributeTokenSubstitutor pts = new MCRAttributeTokenSubstitutor(xml, parameters);
        String defaultCancelURL = MCRServlet.getBaseURL();
        String defaultSourceURI = "buildxml:_rootName_=emptySource";
        this.sourceURI = pts.substituteTokens("source", "uri", defaultSourceURI);
        this.cancelURL = pts.substituteTokens("cancel", "url", defaultCancelURL);
        xml.removeChildren("cancel");
        xml.addContent(new Element("cancel").setAttribute("url", cancelURL));
    }

    private void buildAndSetID() {
        this.id = MCRUniqueID.buildID();
        xml.setAttribute("session", id);
    }

    public String getID() {
        return id;
    }

    public Element getXML() {
        return xml;
    }

    public String getCancelURL() {
        return cancelURL;
    }

    public String getSourceURI() {
        return sourceURI;
    }
}
