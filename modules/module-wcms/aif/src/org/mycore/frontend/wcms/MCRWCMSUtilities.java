package org.mycore.frontend.wcms;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import org.jdom.xpath.XPath;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

public class MCRWCMSUtilities {
    final static String OBJIDPREFIX_WEBPAGE = "webpage:";

    // strategies for access verification
    final static int ALLTRUE = 1;

    final static int ONETRUE_ALLTRUE = 2;

    final static XPath xpath;
    static {
        try {
            xpath = XPath.newInstance("*[@href and not(@type='extern')]");
        } catch (JDOMException e) {
            throw new MCRException("", e);
        }
    }

    private final static Logger LOGGER = Logger.getLogger("MCRWCMSUtilities");

    public static boolean readAccess(String webpageID, String blogWebpageID) throws JDOMException, IOException {
        LOGGER.debug("###############################################");
        LOGGER.debug("start check read access with blogWebpageID for webpageID="+webpageID+"...");
        boolean access =getAccess(webpageID, "read", ALLTRUE);
        LOGGER.debug("finished checking read access with blogWebpageID: "+webpageID+"="+access+"...");
        LOGGER.debug("###############################################");
        return access;
    }
    
    public static boolean readAccess(String webpageID) throws JDOMException, IOException {
        LOGGER.debug("###############################################");
        LOGGER.debug("start check read access for webpageID="+webpageID+"...");
        boolean access =getAccess(webpageID, "read", ALLTRUE);
        LOGGER.debug("finished checking read access: "+webpageID+"="+access+"...");
        LOGGER.debug("###############################################");
        return access;
    }

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
        return getWriteAccessGeneral() && getAccess(webpageID, "write", ONETRUE_ALLTRUE);
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
        origNavi.addContent(getNavi().getRootElement().detach());
        Document writableNavi = new Document(new Element("root"));
        buildWritableNavi(origNavi, writableNavi);
        return new DOMOutputter().output(writableNavi);
    }

    protected static boolean getWriteAccessGeneral() {
        return MCRAccessManager.getAccessImpl().checkPermission("wcms-access");
    }

    private static boolean getAccess(String webpageID, String permission, int strategy) throws JDOMException, IOException {
        // get item as JDOM-Element
        XPath xpath = XPath.newInstance("//.[@href='" + webpageID + "']");
        Element item = (Element) xpath.selectSingleNode(getNavi());
        // check permission according to $strategy
        boolean access = false;
        if (strategy == ALLTRUE) {
            access = true;
            do {
                access = itemAccess(permission, item, access);
                /*if (item.isRootElement())
                    item = null;
                else
                */
                    item = item.getParentElement();
            } while (item != null && access);
        } else if (strategy == ONETRUE_ALLTRUE) {
            access = false;
            do {
                access = itemAccess(permission, item, access);
                /*
                if (item.isRootElement())
                    item = null;
                else
                */
                    item = item.getParentElement();
            } while (item != null && !access);
        }
        return access;
    }

    private static boolean itemAccess(String permission, Element item, boolean access) {
        MCRAccessInterface am = MCRAccessManager.getAccessImpl();
        String objID = getWebpageACLID(item);
        if (am.hasRule(objID, permission))
            access = am.checkPermission(objID, permission);
        return access;
    }

    private static String getWebpageACLID(Element item) {
        return OBJIDPREFIX_WEBPAGE + getWebpageID(item);
    }

    private static String getWebpageID(Element item) {
        return item.getAttributeValue("href");
    }

    private static Document getNavi() throws JDOMException, IOException {
        final MCRConfiguration CONFIG = MCRConfiguration.instance();
        final File navFile = new File(CONFIG.getString("MCR.WCMS.navigationFile").replace('/', File.separatorChar));
        final Document navigation = new SAXBuilder().build(navFile);
        return navigation;
    }

    private static void buildWritableNavi(Element origNavi, Document writableNavi) throws JDOMException, IOException {
        List childs = xpath.selectNodes(origNavi);
        Iterator childIter = childs.iterator();
        while (childIter.hasNext()) {
            Element child = (Element) childIter.next();
            boolean access = itemAccess("write", child, false);
            if (access) {
                // mark root item, to be able proccessing by XSL
                child.setAttribute("ancestorLabels", getAncestorLabels(child));
                // cut node and add to target XML
                writableNavi.getRootElement().addContent(child.detach());
            } else
                buildWritableNavi(child, writableNavi);
        }
    }

    /**
     * Returns all labels of the ancestor axis for the given item within
     * navigation.xml
     * 
     * @param itemClone
     * @return Label as String, like "labelRoot > labelChild >
     *         labelChildOfChild"
     * @throws JDOMException
     * @throws IOException
     */
    private static final String getAncestorLabels(Element item) throws JDOMException, IOException {
        String label = "";
        String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage().trim();
        XPath xpath = XPath.newInstance("//.[@href='" + getWebpageID(item) + "']");
        Element ic = (Element) xpath.selectSingleNode(getNavi());
        while (ic.getName().equals("item")) {
            ic = ic.getParentElement();
            String webpageID = getWebpageID(ic);
            xpath = XPath.newInstance("//.[@href='" + webpageID + "']/label[@xml:lang='" + lang + "']");
            Element labelEl = (Element) xpath.selectSingleNode(getNavi());
            if (labelEl != null) {
                if (label.equals(""))
                    label = labelEl.getTextTrim();
                else
                    label = labelEl.getTextTrim() + " > " + label;
            }
        }
        return label;
    }

}
