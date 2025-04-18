/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend;

import static org.mycore.access.MCRAccessManager.PERMISSION_READ;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.output.DOMOutputter;
import org.jdom2.output.support.AbstractDOMOutputProcessor;
import org.jdom2.output.support.FormatStack;
import org.jdom2.util.NamespaceStack;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRRuleAccessInterface;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRURLContent;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.resource.MCRResourceHelper;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

/**
 *
 * Xalan extention for navigation.xsl
 *
 */
public class MCRLayoutUtilities {
    // strategies for access verification
    public static final int ALLTRUE = 1;

    public static final int ONETRUE_ALLTRUE = 2;

    public static final int ALL2BLOCKER_TRUE = 3;

    public static final String NAV_RESOURCE = MCRConfiguration2.getString("MCR.NavigationFile")
        .orElse("/config/navigation.xml");

    static final String OBJIDPREFIX_WEBPAGE = "webpage:";

    private static final int STANDARD_CACHE_SECONDS = 10;

    private static final XPathFactory XPATH_FACTORY = XPathFactory.instance();

    private static final Logger LOGGER = LogManager.getLogger();

    private static final boolean ACCESS_CONTROLL_ON = MCRConfiguration2
        .getOrThrow("MCR.Website.ReadAccessVerification", Boolean::parseBoolean);

    private static Map<String, Element> itemStore = new HashMap<>();

    private static final LoadingCache<String, DocumentHolder> NAV_DOCUMENT_CACHE = CacheBuilder.newBuilder()
        .refreshAfterWrite(STANDARD_CACHE_SECONDS, TimeUnit.SECONDS).build(new CacheLoader<>() {

            Executor executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "navigation.xml refresh"));

            @Override
            public DocumentHolder load(String key) throws Exception {
                URL url = MCRResourceHelper.getWebResourceUrl(key);
                try {
                    return new DocumentHolder(url);
                } finally {
                    itemStore.clear();
                }

            }

            @Override
            public ListenableFuture<DocumentHolder> reload(final String key, DocumentHolder oldValue) throws Exception {
                URL url = MCRResourceHelper.getWebResourceUrl(key);
                if (oldValue.isValid(url)) {
                    LOGGER.debug("Keeping {} in cache", url);
                    return Futures.immediateFuture(oldValue);
                }
                ListenableFutureTask<DocumentHolder> task = ListenableFutureTask.create(() -> load(key));
                executor.execute(task);
                return task;
            }
        });

    /**
     * Verifies a given $webpage-ID (//item/@href) from navigation.xml on read
     * permission, based on ACL-System. To be used by XSL with
     * Xalan-Java-Extension-Call. $blockerWebpageID can be used as already
     * verified item with read access. So, only items of the ancestor axis till
     * and exclusive $blockerWebpageID are verified. Use this, if you want to
     * speed up the check
     *
     * @param webpageID
     *            any item/@href from navigation.xml
     * @param blockerWebpageID
     *            any ancestor item of webpageID from navigation.xml
     * @return true if access granted, false if not
     */
    public static boolean readAccess(String webpageID, String blockerWebpageID) {
        if (ACCESS_CONTROLL_ON) {
            long startTime = System.currentTimeMillis();
            boolean access = getAccess(webpageID, PERMISSION_READ, ALL2BLOCKER_TRUE, blockerWebpageID);
            LOGGER.debug("checked read access for webpageID= {} (with blockerWebpageID ={}) => {}: took {} msec.",
                () -> webpageID, () -> blockerWebpageID, () -> access, () -> getDuration(startTime));
            return access;
        } else {
            return true;
        }
    }

    /**
     * Verifies a given $webpage-ID (//item/@href) from navigation.xml on read
     * permission, based on ACL-System. To be used by XSL with
     * Xalan-Java-Extension-Call.
     *
     * @param webpageID
     *            any item/@href from navigation.xml
     * @return true if access granted, false if not
     */
    public static boolean readAccess(String webpageID) {
        if (ACCESS_CONTROLL_ON) {
            long startTime = System.currentTimeMillis();
            boolean access = getAccess(webpageID, PERMISSION_READ, ALLTRUE);
            LOGGER.debug("checked read access for webpageID= {} => {}: took {} msec.",
                () -> webpageID, () -> access, () -> getDuration(startTime));
            return access;
        } else {
            return true;
        }
    }

    /**
     * Returns all labels of the ancestor axis for the given item within
     * navigation.xml
     *
     * @param item a navigation item
     * @return Label as String, like "labelRoot &gt; labelChild &gt;
     *         labelChildOfChild"
     */
    public static String getAncestorLabels(Element item) {
        StringBuilder label = new StringBuilder();
        String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage().trim();
        XPathExpression<Element> xpath
            = XPATH_FACTORY.compile("//.[@href='" + getWebpageID(item) + "']", Filters.element());
        Element ic = xpath.evaluateFirst(getNavi());
        while (ic.getName().equals("item")) {
            ic = ic.getParentElement();
            String webpageID = getWebpageID(ic);
            xpath = XPATH_FACTORY.compile("//.[@href='" + webpageID + "']/label[@xml:lang='" + lang + "']",
                Filters.element());
            Element labelEl = xpath.evaluateFirst(getNavi());
            if (labelEl != null) {
                if (label.isEmpty()) {
                    label = new StringBuilder(labelEl.getTextTrim());
                } else {
                    label.insert(0, labelEl.getTextTrim() + " > ");
                }
            }
        }
        return label.toString();
    }

    /**
     * Verifies, if an item of navigation.xml has a given $permission.
     *
     * @param webpageID
     *            item/@href
     * @param permission
     *            permission to look for
     * @param strategy
     *            ALLTRUE =&gt; all ancestor items of webpageID must have the
     *            permission, ONETRUE_ALLTRUE =&gt; only 1 ancestor item must have
     *            the permission
     * @return true, if access, false if no access
     */
    public static boolean getAccess(String webpageID, String permission, int strategy) {
        Element item = getItem(webpageID);
        // check permission according to $strategy
        boolean access = strategy == ALLTRUE;
        if (strategy == ALLTRUE) {
            while (item != null && access) {
                access = itemAccess(permission, item, access);
                item = item.getParentElement();
            }
        } else if (strategy == ONETRUE_ALLTRUE) {
            while (item != null && !access) {
                access = itemAccess(permission, item, access);
                item = item.getParentElement();
            }
        }
        return access;
    }

    /**
     * Verifies, if an item of navigation.xml has a given $permission with a
     * stop item ($blockerWebpageID)
     *
     * @param webpageID
     *            item/@href
     * @param permission
     *            permission to look for
     * @param strategy
     *            ALL2BLOCKER_TRUE =&gt; all ancestor items of webpageID till and
     *            exlusiv $blockerWebpageID must have the permission
     * @param blockerWebpageID
     *            any ancestor item of webpageID from navigation.xml
     * @return true, if access, false if no access
     */
    public static boolean getAccess(String webpageID, String permission, int strategy, String blockerWebpageID) {
        Element item = getItem(webpageID);
        // check permission according to $strategy
        boolean access = false;
        if (strategy == ALL2BLOCKER_TRUE) {
            access = true;
            String itemHref;
            do {
                access = itemAccess(permission, item, access);
                item = item.getParentElement();
                itemHref = getWebpageID(item);
            } while (item != null && access && !(itemHref != null && itemHref.equals(blockerWebpageID)));
        }
        return access;
    }

    /**
     * Returns a Element presentation of an item[@href=$webpageID]
     */
    private static Element getItem(String webpageID) {
        Element item = itemStore.get(webpageID);
        if (item == null) {
            XPathExpression<Element> xpath = XPATH_FACTORY.compile("//.[@href='" + webpageID + "']", Filters.element());
            item = xpath.evaluateFirst(getNavi());
            itemStore.put(webpageID, item);
        }
        return item;
    }

    /**
     * Verifies a single item on access according to $permission Falls back to version without query
     * if no rule for exact query string exists.
     *
     * @param permission an ACL permission
     * @param item element to check
     * @param access
     *            initial value
     */
    public static boolean itemAccess(String permission, Element item, boolean access) {
        return webpageAccess(permission, getWebpageID(item), access);
    }

    /**
     * Verifies a single webpage on access according to $permission. Falls back to version without query
     * if no rule for exact query string exists.
     *
     * @param permission an ACL permission
     * @param webpageId webpage to check
     * @param access
     *            initial value
     */
    public static boolean webpageAccess(String permission, String webpageId, boolean access) {
        List<String> ruleIDs = getAllWebpageACLIDs(webpageId);
        return ruleIDs.stream()
            .filter(objID -> MCRAccessManager.hasRule(objID, permission))
            .findFirst()
            .map(objID -> MCRAccessManager.checkPermission(objID, permission))
            .orElse(access);
    }

    private static List<String> getAllWebpageACLIDs(String webpageID) {
        String webpageACLID = getWebpageACLID(webpageID);
        List<String> webpageACLIDs = new ArrayList<>(2);
        webpageACLIDs.add(webpageACLID);
        int queryIndex = webpageACLID.indexOf('?');
        if (queryIndex != -1) {
            String baseWebpageACLID = webpageACLID.substring(0, queryIndex);
            webpageACLIDs.add(baseWebpageACLID);
        }
        return webpageACLIDs;
    }

    public static String getWebpageACLID(String webpageID) {
        return OBJIDPREFIX_WEBPAGE + webpageID;
    }

    private static String getWebpageID(Element item) {
        return item == null ? null : item.getAttributeValue("href", item.getAttributeValue("dir"));
    }

    /**
     * Returns the navigation.xml as org.jdom2.document, using a cache the
     * enhance loading time.
     *
     * @return navigation.xml as org.jdom2.document
     */
    public static Document getNavi() {
        return NAV_DOCUMENT_CACHE.getUnchecked(NAV_RESOURCE).parsedDocument;
    }

    /**
     * Returns the navigation.xml as URL.
     * <p>
     * Use this method if you need to parse it on your own.
     */
    public static URL getNavigationURL() {
        return MCRResourceHelper.getWebResourceUrl(NAV_RESOURCE);
    }

    public static org.w3c.dom.Document getPersonalNavigation() throws JDOMException, XPathExpressionException {
        Document navi = getNavi();
        DOMOutputter accessCleaner = new DOMOutputter(new AccessCleaningDOMOutputProcessor());
        org.w3c.dom.Document personalNavi = accessCleaner.output(navi);
        XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
        NodeList emptyGroups = (NodeList) xpath.evaluate("/navigation/menu/group[not(item)]", personalNavi,
            XPathConstants.NODESET);
        for (int i = 0; i < emptyGroups.getLength(); ++i) {
            org.w3c.dom.Element group = (org.w3c.dom.Element) emptyGroups.item(i);
            group.getParentNode().removeChild(group);
        }
        NodeList emptyMenu = (NodeList) xpath.evaluate("/navigation/menu[not(item or group)]", personalNavi,
            XPathConstants.NODESET);
        for (int i = 0; i < emptyMenu.getLength(); ++i) {
            org.w3c.dom.Element menu = (org.w3c.dom.Element) emptyMenu.item(i);
            menu.getParentNode().removeChild(menu);
        }
        NodeList emptyNodes = (NodeList) xpath.evaluate("//text()[normalize-space(.) = '']", personalNavi,
            XPathConstants.NODESET);
        for (int i = 0; i < emptyNodes.getLength(); ++i) {
            Node emptyTextNode = emptyNodes.item(i);
            emptyTextNode.getParentNode().removeChild(emptyTextNode);
        }
        NodeList userNameNodes = (NodeList) xpath.evaluate("//@href[contains(.,'{CurrentUser}')]", personalNavi,
            XPathConstants.NODESET);
        for (int i = 0; i < userNameNodes.getLength(); i++) {
            Attr href = (Attr) userNameNodes.item(i);
            String userID = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
            try {
                href.setValue(href.getValue().replace("{CurrentUser}", MCRXMLFunctions.encodeURIPath(userID)));
            } catch (URISyntaxException e) {
                throw new MCRException("Unexpected exception while encoding user name: " + userID, e);
            }
        }
        personalNavi.normalizeDocument();
        if (LOGGER.isDebugEnabled()) {
            try {
                String encoding = "UTF-8";
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                transformer.transform(new DOMSource(personalNavi), new StreamResult(bout));
                LOGGER.debug("personal navigation: {}", bout.toString(encoding));
            } catch (IllegalArgumentException | TransformerFactoryConfigurationError | TransformerException
                | UnsupportedEncodingException e) {
                LOGGER.warn("Error while getting debug information.", e);
            }

        }
        return personalNavi;
    }

    public static long getDuration(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    public static String getWebpageObjIDPrefix() {
        return OBJIDPREFIX_WEBPAGE;
    }

    public static boolean hasRule(String permission, String webpageID) {
        MCRAccessInterface am = MCRAccessManager.getAccessImpl();
        if (am instanceof MCRRuleAccessInterface ruleAccessInterface) {
            return ruleAccessInterface.hasRule(getWebpageACLID(webpageID), permission);
        } else {
            return true;
        }
    }

    public static String getRuleID(String permission, String webpageID) {
        MCRAccessStore as = MCRAccessStore.obtainInstance();
        String ruleID = as.getRuleID(getWebpageACLID(webpageID), permission);
        return Objects.requireNonNullElse(ruleID, "");
    }

    public static String getRuleDescr(String permission, String webpageID) {
        MCRAccessInterface am = MCRAccessManager.getAccessImpl();
        String ruleDes = null;
        if (am instanceof MCRRuleAccessInterface ruleAccessInterface) {
            ruleDes = ruleAccessInterface.getRuleDescription(getWebpageACLID(webpageID), permission);
        }
        return Objects.requireNonNullElse(ruleDes, "");
    }

    public static String getPermission2ReadWebpage() {
        return PERMISSION_READ;
    }

    private static class DocumentHolder {
        URL docURL;

        Document parsedDocument;

        long lastModified;

        DocumentHolder(URL url) throws JDOMException, IOException {
            docURL = url;
            parseDocument();
        }

        public boolean isValid(URL url) throws IOException {
            return docURL.toString().equals(url.toString()) && lastModified == getLastModified();
        }

        private void parseDocument() throws JDOMException, IOException {
            lastModified = getLastModified();
            LOGGER.info("Parsing: {}", docURL);
            MCRURLContent urlContent = new MCRURLContent(docURL);
            parsedDocument = urlContent.asXML();
        }

        private long getLastModified() throws IOException {
            URLConnection urlConnection = docURL.openConnection();
            return urlConnection.getLastModified();
        }
    }

    private static final class AccessCleaningDOMOutputProcessor extends AbstractDOMOutputProcessor {

        @Override
        protected org.w3c.dom.Element printElement(FormatStack fstack, NamespaceStack nstack,
            org.w3c.dom.Document basedoc, Element element) {
            Attribute href = element.getAttribute("href");
            return (href == null || itemAccess(PERMISSION_READ, element, true)) ? super.printElement(fstack, nstack,
                basedoc, element) : null;
        }

    }
}
