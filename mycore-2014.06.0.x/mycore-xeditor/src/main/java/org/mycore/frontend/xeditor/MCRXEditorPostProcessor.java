package org.mycore.frontend.xeditor;

import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRXSL2XMLTransformer;
import org.xml.sax.SAXException;

public class MCRXEditorPostProcessor {

    private String stylesheet;

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public Document process(Document xml) throws IOException, JDOMException, SAXException {
        if (stylesheet == null)
            return xml.clone();

        MCRContent source = new MCRJDOMContent(xml);
        MCRContent transformed = MCRXSL2XMLTransformer.getInstance("xsl/" + stylesheet).transform(source);
        return transformed.asXML();
    }
}
