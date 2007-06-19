package org.mycore.frontend.wcms;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.DOMOutputter;
import org.jdom.xpath.XPath;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.frontend.MCRLayoutUtilities;

public class MCRWCMSUtilities {

    final static XPath xpath;
    static {
        try {
            xpath = XPath.newInstance("*[@href and not(@type='extern')]");
        } catch (JDOMException e) {
            throw new MCRException("", e);
        }
    }

    private final static Logger LOGGER = Logger.getLogger(MCRWCMSUtilities.class);

    /**
     * Verifies a given webpage-ID (//item/@href) from navigation.xml on write
     * permission, based on ACL-System. To be used by XSL with
     * Xalan-Java-Extension-Call
     * 
     * @param webpageID
     * @return True if access granted, false if access is forbidden
     * @throws JDOMException
     * @throws IOException
     */
    public static boolean writeAccess(String webpageID) throws JDOMException, IOException {
        LOGGER.debug("start check write access for webpageID=" + webpageID + "...");
        boolean access = getWriteAccessGeneral() && MCRLayoutUtilities.getAccess(webpageID, "write", MCRLayoutUtilities.ONETRUE_ALLTRUE);
        LOGGER.debug("finished checking write access for webpage=" + webpageID + "=" + access + "...");
        return access;
    }

    /**
     * Returns a filtered navigation.xml, according to current logged in users
     * WCMS-Write permissions. Only menu items that are permitted to write on
     * are given back.
     * 
     * @return org.w3c.dom.Document with writable menu items.
     * @throws JDOMException
     * @throws IOException
     */
    public static org.w3c.dom.Document getWritableNavi() throws JDOMException, IOException {
        Element origNavi = new Element("root");
        origNavi.addContent(MCRLayoutUtilities.getNavi().getRootElement().detach());
        Document writableNavi = new Document(new Element("root"));
        buildWritableNavi(origNavi, writableNavi);
        return new DOMOutputter().output(writableNavi);
    }

    protected static boolean getWriteAccessGeneral() {
        return MCRAccessManager.getAccessImpl().checkPermission("wcms-access");
    }

    private static void buildWritableNavi(Element origNavi, Document writableNavi) throws JDOMException, IOException {
        List childs = xpath.selectNodes(origNavi);
        Iterator childIter = childs.iterator();
        while (childIter.hasNext()) {
            Element child = (Element) childIter.next();
            boolean access = MCRLayoutUtilities.itemAccess("write", child, false);
            if (access) {
                // mark root item, to be able proccessing by XSL
                child.setAttribute("ancestorLabels", MCRLayoutUtilities.getAncestorLabels(child));
                // cut node and add to target XML
                writableNavi.getRootElement().addContent(child.detach());
            } else
                buildWritableNavi(child, writableNavi);
        }
    }

}
