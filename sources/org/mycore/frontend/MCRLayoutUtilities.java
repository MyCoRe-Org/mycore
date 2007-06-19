package org.mycore.frontend;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;

public class MCRLayoutUtilities {
    final static String OBJIDPREFIX_WEBPAGE = "webpage:";

    // strategies for access verification
    public final static int ALLTRUE = 1;

    public final static int ONETRUE_ALLTRUE = 2;

    public final static int ALL2BLOCKER_TRUE = 3;

    private final static Logger LOGGER = Logger.getLogger(MCRLayoutUtilities.class);
    
    private static HashMap itemStore = new HashMap();
    
    private static long CACHE_INITTIME = 0;
    
    private static Document NAVI = null;
    
    public static boolean readAccess(String webpageID, String blockerWebpageID) throws JDOMException, IOException {
        LOGGER.debug("start to check read access for webpageID= " + webpageID + " (with blockerWebpageID =" + blockerWebpageID + ")");
        long startTime = System.currentTimeMillis();
        boolean access = getAccess(webpageID, "read", ALL2BLOCKER_TRUE, blockerWebpageID);
        long finishTime = (System.currentTimeMillis() - startTime) ;
        LOGGER.debug("verified read access for webpageID= " + webpageID + " (with blockerWebpageID =" + blockerWebpageID + ") => " + access + ": took "
                        + finishTime + " msec.");
        return access;
    }

    public static boolean readAccess(String webpageID) throws JDOMException, IOException {
        long startTime = System.currentTimeMillis();
        boolean access = getAccess(webpageID, "read", ALLTRUE);
        long finishTime = (System.currentTimeMillis() - startTime) ;
        LOGGER.debug("verified read access for webpageID= " + webpageID + " => " + access + ": took " + finishTime + " msec.");
        return access;
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
    public static final String getAncestorLabels(Element item) throws JDOMException, IOException {
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

    public static boolean getAccess(String webpageID, String permission, int strategy) throws JDOMException, IOException {
        Element item = getItem(webpageID);
        // check permission according to $strategy
        boolean access = false;
        if (strategy == ALLTRUE) {
            access = true;
            do {
                access = itemAccess(permission, item, access);
                item = item.getParentElement();
            } while (item != null && access);
        } else if (strategy == ONETRUE_ALLTRUE) {
            access = false;
            do {
                access = itemAccess(permission, item, access);
                item = item.getParentElement();
            } while (item != null && !access);
        }
        return access;
    }

    public static boolean getAccess(String webpageID, String permission, int strategy, String blockerWebpageID) throws JDOMException, IOException {
        Element item = getItem(webpageID);
        // check permission according to $strategy
        boolean access = false;
        if (strategy == ALL2BLOCKER_TRUE) {
            access = true;
            do {
                access = itemAccess(permission, item, access);
                item = item.getParentElement();
            } while (item != null && access && !getWebpageID(item).equals(blockerWebpageID));
        }
        return access;
    }

    private static Element getItem(String webpageID) throws JDOMException, IOException {
        Element item = (Element)itemStore.get(webpageID);
        if (item==null) {
            XPath xpath = XPath.newInstance("//.[@href='" + webpageID + "']");
            item = (Element) xpath.selectSingleNode(getNavi());
            itemStore.put(webpageID,item);
        }
        return item;
    }

    public static boolean itemAccess(String permission, Element item, boolean access) {
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

    public static Document getNavi() throws JDOMException, IOException {
        final MCRConfiguration CONFIG = MCRConfiguration.instance();
        final File navFile = new File(CONFIG.getString("MCR.navigationFile").replace('/', File.separatorChar));
        // cache does not exist or to old
        if (CACHE_INITTIME <= navFile.lastModified()) {
            NAVI = new SAXBuilder().build(navFile);
            CACHE_INITTIME = System.currentTimeMillis();
        } 
        return NAVI;
    }

}