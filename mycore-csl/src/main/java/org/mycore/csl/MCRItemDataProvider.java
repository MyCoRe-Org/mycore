package org.mycore.csl;

import de.undercouch.citeproc.ItemDataProvider;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.xml.sax.SAXException;

import java.io.IOException;

public abstract class MCRItemDataProvider implements ItemDataProvider {
    public  abstract void addContent(MCRContent content) throws IOException, JDOMException, SAXException;
    public abstract void reset();
}
