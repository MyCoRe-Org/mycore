package org.mycore.frontend.xeditor;

import java.io.IOException;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

/**
 * If you implement this interface then you should have a default constructor if you want it to use with.
 * @author Sebastian Hofmann (mcrshofm)
 */
public interface MCRXEditorPostProcessor {
    /**
     * Do the post processing.
     * @param xml the document which has to be post processed
     * @return the post processed document
     * @throws IOException
     * @throws JDOMException
     * @throws SAXException
     */
    Document process(Document xml) throws IOException, JDOMException, SAXException;

    /**
     * Will be called before {@link #process(Document)}.
     * @param attributeMap a map which contains the name(key) and value of attributes of the postprocessor element.
     */
    void setAttributes(Map<String, String> attributeMap);
}
