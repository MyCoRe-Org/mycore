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
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;
import org.mycore.user.MCRUser;

public class MCRLayoutUtilities {
    final static String OBJIDPREFIX_WEBPAGE = "webpage:";

    final static String READ_PERMISSION_WEBPAGE = "read";

    // strategies for access verification
    public final static int ALLTRUE = 1;

    public final static int ONETRUE_ALLTRUE = 2;

    public final static int ALL2BLOCKER_TRUE = 3;

    private final static Logger LOGGER = Logger.getLogger(MCRLayoutUtilities.class);

    private static HashMap itemStore = new HashMap();

    private static long CACHE_INITTIME = 0;

    private static Document NAVI;

    private static final File NAVFILE = new File(MCRConfiguration.instance().getString("MCR.navigationFile").replace('/', File.separatorChar));

    private static final boolean ACCESS_CONTROLL_ON = MCRConfiguration.instance().getBoolean("MCR.Website.ReadAccessVerification", true);

    /**
     * Verifies a given $webpage-ID (//item/@href) from navigation.xml on read
     * permission, based on ACL-System. To be used by XSL with
     * Xalan-Java-Extension-Call. $blockerWebpageID can be used as already
     * verified item with read access. So, only items of the ancestor axis till
     * and exclusive $blockerWebpageID are verified. Use this, if you want to
     * speed up the check
     * 
     * @param webpageID,
     *            any item/@href from navigation.xml
     * @param blockerWebpageID,
     *            any ancestor item of webpageID from navigation.xml
     * @return true if access granted, false if not
     */
    public static boolean readAccess(String webpageID, String blockerWebpageID) {
        if (ACCESS_CONTROLL_ON) {
            long startTime = System.currentTimeMillis();
            boolean access = getAccess(webpageID, "read", ALL2BLOCKER_TRUE, blockerWebpageID);
            LOGGER.debug("checked read access for webpageID= " + webpageID + " (with blockerWebpageID =" + blockerWebpageID + ") => " + access + ": took "
                            + getDuration(startTime) + " msec.");
            return access;
        } else
            return true;
    }

    /**
     * Verifies a given $webpage-ID (//item/@href) from navigation.xml on read
     * permission, based on ACL-System. To be used by XSL with
     * Xalan-Java-Extension-Call.
     * 
     * @param webpageID,
     *            any item/@href from navigation.xml
     * @return true if access granted, false if not
     */
    public static boolean readAccess(String webpageID) {
        if (ACCESS_CONTROLL_ON) {
            long startTime = System.currentTimeMillis();
            boolean access = getAccess(webpageID, "read", ALLTRUE);
            LOGGER.debug("checked read access for webpageID= " + webpageID + " => " + access + ": took " + getDuration(startTime) + " msec.");
            return access;
        } else
            return true;
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
    public static final String getAncestorLabels(Element item) {
        String label = "";
        String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage().trim();
        XPath xpath;
        Element ic = null;
        try {
            xpath = XPath.newInstance("//.[@href='" + getWebpageID(item) + "']");
            ic = (Element) xpath.selectSingleNode(getNavi());
        } catch (JDOMException e) {
            e.printStackTrace();
        }
        while (ic.getName().equals("item")) {
            ic = ic.getParentElement();
            String webpageID = getWebpageID(ic);
            Element labelEl = null;
            try {
                xpath = XPath.newInstance("//.[@href='" + webpageID + "']/label[@xml:lang='" + lang + "']");
                labelEl = (Element) xpath.selectSingleNode(getNavi());
            } catch (JDOMException e) {
                e.printStackTrace();
            }
            if (labelEl != null) {
                if (label.equals(""))
                    label = labelEl.getTextTrim();
                else
                    label = labelEl.getTextTrim() + " > " + label;
            }
        }
        return label;
    }

    /**
     * Verifies, if an item of navigation.xml has a given $permission.
     * 
     * @param webpageID,
     *            item/@href
     * @param permission,
     *            permission to look for
     * @param strategy:
     *            ALLTRUE => all ancestor items of webpageID must have the
     *            permission, ONETRUE_ALLTRUE => only 1 ancestor item must have
     *            the permission
     * @return true, if access, false if no access
     */
    public static boolean getAccess(String webpageID, String permission, int strategy) {
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

    /**
     * Verifies, if an item of navigation.xml has a given $permission with a
     * stop item ($blockerWebpageID)
     * 
     * @param webpageID,
     *            item/@href
     * @param permission,
     *            permission to look for
     * @param strategy:
     *            ALL2BLOCKER_TRUE => all ancestor items of webpageID till and
     *            exlusiv $blockerWebpageID must have the permission
     * @param blockerWebpageID:
     *            any ancestor item of webpageID from navigation.xml
     * @return true, if access, false if no access
     */
    public static boolean getAccess(String webpageID, String permission, int strategy, String blockerWebpageID) {
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

    /**
     * Returns a Element presentation of an item[@href=$webpageID]
     * 
     * @param webpageID
     * @return Element
     */
    private static Element getItem(String webpageID) {
        if (!naviCacheValid())
            itemStore.clear();
        Element item = (Element) itemStore.get(webpageID);
        if (item == null) {
            XPath xpath;
            try {
                xpath = XPath.newInstance("//.[@href='" + webpageID + "']");
                item = (Element) xpath.selectSingleNode(getNavi());
            } catch (JDOMException e) {
                e.printStackTrace();
            }
            itemStore.put(webpageID, item);
        }
        return item;
    }

    /**
     * Verifies a single item on access according to $permission
     * 
     * @param permission
     * @param item
     * @param access,
     *            initial value
     * @return
     */
    public static boolean itemAccess(String permission, Element item, boolean access) {
        MCRAccessInterface am = MCRAccessManager.getAccessImpl();
        String objID = getWebpageACLID(item);
        if (am.hasRule(objID, permission))
            access = am.checkPermission(objID, permission);
        return access;
    }

    /**
     * Verifies a single item on access according to $permission and for a given
     * user
     * 
     * @param permission
     * @param item
     * @param access,
     *            initial value
     * @param user
     * @return
     */
    public static boolean itemAccess(String permission, Element item, boolean access, MCRUser user) {
        MCRAccessInterface am = MCRAccessManager.getAccessImpl();
        String objID = getWebpageACLID(item);
        if (am.hasRule(objID, permission))
            access = am.checkPermission(objID, permission, user);
        return access;
    }

    /**
     * Verifies if the cache of navigation.xml is valid.
     * 
     * @return true if valid, false if note
     */
    private static boolean naviCacheValid() {
        if (CACHE_INITTIME < NAVFILE.lastModified())
            return false;
        else
            return true;
    }

    private static String getWebpageACLID(Element item) {
        return OBJIDPREFIX_WEBPAGE + getWebpageID(item);
    }

    public static String getWebpageACLID(String webpageID) {
        return OBJIDPREFIX_WEBPAGE + webpageID;
    }

    private static String getWebpageID(Element item) {
        return item.getAttributeValue("href");
    }

    /**
     * Returns the navigation.xml as org.jdom.document, using a cache the
     * enhance loading time.
     * 
     * @return navigation.xml as org.jdom.document
     */
    public static Document getNavi() {
        if (!naviCacheValid()) {
            try {
                NAVI = new SAXBuilder().build(NAVFILE);
            } catch (JDOMException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            CACHE_INITTIME = System.currentTimeMillis();
        }
        return NAVI;
    }

    public static long getDuration(long startTime) {
        return (System.currentTimeMillis() - startTime);
    }

    public static String getOBJIDPREFIX_WEBPAGE() {
        return OBJIDPREFIX_WEBPAGE;
    }

    public static boolean hasRule(String permission, String webpageID) {
        MCRAccessInterface am = MCRAccessManager.getAccessImpl();
        return am.hasRule(getWebpageACLID(webpageID), permission);
    }

    public static String getRuleID(String permission, String webpageID) {
        MCRAccessStore as = MCRAccessStore.getInstance();
        String ruleID = as.getRuleID(getWebpageACLID(webpageID), permission);
        if (ruleID != null)
            return ruleID;
        else
            return "";
    }

    public static String getRuleDescr(String permission, String webpageID) {
        MCRAccessInterface am = MCRAccessManager.getAccessImpl();
        String ruleDes = am.getRuleDescription(getWebpageACLID(webpageID), permission);
        if (ruleDes != null)
            return ruleDes;
        else
            return "";
    }

    public static String getPermission2ReadWebpage() {
        return READ_PERMISSION_WEBPAGE;
    }

    public static String getLastValidPageID() {
        String page = (String) MCRSessionMgr.getCurrentSession().get("lastPageID");
        return (page == null ? "" : page);
    }

    public static String setLastValidPageID(String pageID) {
        MCRSessionMgr.getCurrentSession().put("lastPageID", pageID);
        return "";
    }
}