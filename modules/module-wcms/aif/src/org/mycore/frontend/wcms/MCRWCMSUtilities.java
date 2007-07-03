package org.mycore.frontend.wcms;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.DOMOutputter;
import org.jdom.xpath.XPath;
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
    public static boolean writeAccess(String webpageID) {
        long startTime = System.currentTimeMillis();
        boolean access = MCRLayoutUtilities.getAccess(webpageID, "write", MCRLayoutUtilities.ONETRUE_ALLTRUE);
        LOGGER.debug("checked write access for webpage=" + webpageID + "=" + access + ": took " + MCRLayoutUtilities.getDuration(startTime) + " msec.");
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
        origNavi.addContent((Element) MCRLayoutUtilities.getNavi().getRootElement().clone());
        Document writableNavi = new Document(new Element("root"));
        buildWritableNavi(origNavi, writableNavi);
        return new DOMOutputter().output(writableNavi);
    }

    /**
     * Returns a boolean, signalling if the user has at least write acces for 1
     * item.
     * 
     * @return true, if access is granted for at least 1 item OR false, if the
     *         user has no access for any item
     * @throws JDOMException
     */
    protected static boolean writeAccessGeneral() {
        Element navi = (new Element("root")).addContent((Element) MCRLayoutUtilities.getNavi().getRootElement().clone());
        long startTime = System.currentTimeMillis();
        HashMap accessMap = new HashMap();
        getWriteAccessGeneral(navi, accessMap);
        boolean access = !accessMap.isEmpty();
        LOGGER.debug("checked write access in general=" + access + ": took " + MCRLayoutUtilities.getDuration(startTime) + " msec.");
        return access;
    }

    /**
     * The implementation for writeAccessGeneral()
     * 
     * @param navigation
     * @return
     * @throws JDOMException
     */
    private static void getWriteAccessGeneral(Element navigation, HashMap accessMap) {
        List childs = null;
        try {
            childs = xpath.selectNodes(navigation);
        } catch (JDOMException e) {
            e.printStackTrace();
        }
        Iterator childIter = childs.iterator();
        while (accessMap.isEmpty() && childIter.hasNext()) {
            Element child = (Element) childIter.next();
            boolean access = MCRLayoutUtilities.itemAccess("write", child, false);
            if (access)
                accessMap.put("access", "true");
            else
                getWriteAccessGeneral(child, accessMap);
        }
    }

    /**
     * Returns a DOM-Object containing only items the user have write access
     * for. The writable items structure will be put into writableNavi.
     * 
     * @param origNavi
     *            The navigation.xml with an additional dummy root Element
     * @param writableNavi
     *            A Document with only o root tag to be filled with writeable
     *            items
     * @throws JDOMException
     * @throws IOException
     */
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
