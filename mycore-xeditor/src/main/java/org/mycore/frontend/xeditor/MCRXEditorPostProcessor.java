package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.util.Iterator;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRXSL2XMLTransformer;
import org.mycore.common.xml.MCRXMLFunctions;
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
        MCRContent normalized = new MCRNormalizeUnicodeTransformer().transform(transformed);
        return normalized.asXML();
    }
}

class MCRNormalizeUnicodeTransformer extends MCRContentTransformer {

    public MCRJDOMContent transform(MCRContent source) throws IOException {
        try {
            Element root = source.asXML().getRootElement().clone();
            for (Iterator<Text> iter = root.getDescendants(Filters.text()).iterator(); iter.hasNext();) {
                Text text = iter.next();
                text.setText(MCRXMLFunctions.normalizeUnicode(text.getText()));
            }
            return new MCRJDOMContent(root);
        } catch (JDOMException ex) {
            throw new IOException(ex);
        } catch (SAXException ex) {
            throw new IOException(ex);
        }
    }
}
