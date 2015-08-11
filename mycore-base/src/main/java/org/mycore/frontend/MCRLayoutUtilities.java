package org.mycore.frontend;

import static org.mycore.access.MCRAccessManager.PERMISSION_READ;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
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

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.output.DOMOutputter;
import org.jdom2.output.support.AbstractDOMOutputProcessor;
import org.jdom2.output.support.FormatStack;
import org.jdom2.util.NamespaceStack;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRURIResolver;
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
    private static final int STANDARD_CACHE_SECONDS = 10;

    private static final XPathFactory XPATH_FACTORY = XPathFactory.instance();

    final static String OBJIDPREFIX_WEBPAGE = "webpage:";

    // strategies for access verification
    public final static int ALLTRUE = 1;

    public final static int ONETRUE_ALLTRUE = 2;

    public final static int ALL2BLOCKER_TRUE = 3;

    private final static Logger LOGGER = Logger.getLogger(MCRLayoutUtilities.class);

    private static HashMap<String, Element> itemStore = new HashMap<String, Element>();

    public static final String NAV_RESOURCE = MCRConfiguration.instance().getString("MCR.NavigationFile",
        "/config/navigation.xml");

    private static final ServletContext SERVLET_CONTEXT = MCRURIResolver.getServletContext();

    private static class DocumentHolder {
        URL docURL;

        Document parsedDocument;

        long lastModified;

        public DocumentHolder(URL url) throws JDOMException, IOException {
            docURL = url;
            parseDocument();
        }

        public boolean isValid(URL url) throws IOException {
            return docURL.equals(url) && lastModified == getLastModified();
        }

        private void parseDocument() throws JDOMException, IOException {
            lastModified = getLastModified();
            LOGGER.info("Parsing: " + docURL);
            parsedDocument = new SAXBuilder(XMLReaders.NONVALIDATING).build(docURL);
        }

        private long getLastModified() throws IOException {
            URLConnection urlConnection = docURL.openConnection();
            long modified = urlConnection.getLastModified();
            return modified;
        }
    }

    private static final LoadingCache<String, DocumentHolder> NAV_DOCUMENT_CACHE = CacheBuilder.newBuilder()
        .refreshAfterWrite(STANDARD_CACHE_SECONDS, TimeUnit.SECONDS).build(new CacheLoader<String, DocumentHolder>() {

            Executor executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "navigation.xml refresh");
                }
            });

            @Override
            public DocumentHolder load(String key) throws Exception {
                URL url = SERVLET_CONTEXT.getResource(key);
                try {
                    DocumentHolder holder = new DocumentHolder(url);
                    return holder;
                } finally {
                    itemStore.clear();
                }

            }

            @Override
            public ListenableFuture<DocumentHolder> reload(final String key, DocumentHolder oldValue) throws Exception {
                URL url = SERVLET_CONTEXT.getResource(key);
                if (oldValue.isValid(url)) {
                    LOGGER.info("Keeping " + url + " in cache");
                    return Futures.immediateFuture(oldValue);
                }
                ListenableFutureTask<DocumentHolder> task = ListenableFutureTask.create(new Callable<DocumentHolder>() {
                    @Override
                    public DocumentHolder call() throws Exception {
                        return load(key);
                    }
                });
                executor.execute(task);
                return task;
            }
        });

    private static final boolean ACCESS_CONTROLL_ON = MCRConfiguration.instance().getBoolean(
        "MCR.Website.ReadAccessVerification");

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
            LOGGER.debug("checked read access for webpageID= " + webpageID + " (with blockerWebpageID ="
                + blockerWebpageID + ") => " + access + ": took " + getDuration(startTime) + " msec.");
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
            LOGGER.debug("checked read access for webpageID= " + webpageID + " => " + access + ": took "
                + getDuration(startTime) + " msec.");
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
        String label = "";
        String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage().trim();
        XPathExpression<Element> xpath;
        Element ic = null;
        xpath = XPATH_FACTORY.compile("//.[@href='" + getWebpageID(item) + "']", Filters.element());
        ic = xpath.evaluateFirst(getNavi());
        while (ic.getName().equals("item")) {
            ic = ic.getParentElement();
            String webpageID = getWebpageID(ic);
            Element labelEl = null;
            xpath = XPATH_FACTORY.compile("//.[@href='" + webpageID + "']/label[@xml:lang='" + lang + "']",
                Filters.element());
            labelEl = xpath.evaluateFirst(getNavi());
            if (labelEl != null) {
                if (label.equals("")) {
                    label = labelEl.getTextTrim();
                } else {
                    label = labelEl.getTextTrim() + " > " + label;
                }
            }
        }
        return label;
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
     * 
     * @param webpageID
     * @return Element
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
     * Verifies a single item on access according to $permission
     * 
     * @param permission an ACL permission
     * @param item element to check
     * @param access
     *            initial value
     */
    public static boolean itemAccess(String permission, Element item, boolean access) {
        String objID = getWebpageACLID(item);
        if (MCRAccessManager.hasRule(objID, permission)) {
            access = MCRAccessManager.checkPermission(objID, permission);
        }
        return access;
    }

    /**
     * Verifies a single item on access according to $permission and for a given
     * user
     * 
     * @param permission an ACL permission
     * @param item element to check
     * @param access
     *            initial value
     * @param userID a user id
     */
    public static boolean itemAccess(String permission, Element item, boolean access, String userID) {
        MCRAccessInterface am = MCRAccessManager.getAccessImpl();
        String objID = getWebpageACLID(item);
        if (am.hasRule(objID, permission)) {
            access = am.checkPermission(objID, permission, userID);
        }
        return access;
    }

    private static String getWebpageACLID(Element item) {
        return OBJIDPREFIX_WEBPAGE + getWebpageID(item);
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
     * Returns the navigation.xml as File.
     * This file may not exist yet as navigation.xml may be served as a web resource.
     * Use {@link #getNavigationURL()} to get access to the actual web resource.
     */
    public static File getNavigationFile() {
        String realPath = SERVLET_CONTEXT.getRealPath(NAV_RESOURCE);
        if (realPath == null) {
            return null;
        }
        return new File(realPath);
    }

    /**
     * Returns the navigation.xml as URL.
     * 
     * Use this method if you need to parse it on your own.
     */
    public static URL getNavigationURL() {
        try {
            return SERVLET_CONTEXT.getResource(NAV_RESOURCE);
        } catch (MalformedURLException e) {
            throw new MCRException("Error while resolving navigation.xml", e);
        }
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
                LOGGER.debug("personal navigation: " + bout.toString(encoding));
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
        if (ruleID != null) {
            return ruleID;
        } else {
            return "";
        }
    }

    public static String getRuleDescr(String permission, String webpageID) {
        MCRAccessInterface am = MCRAccessManager.getAccessImpl();
        String ruleDes = am.getRuleDescription(getWebpageACLID(webpageID), permission);
        if (ruleDes != null) {
            return ruleDes;
        } else {
            return "";
        }
    }

    public static String getPermission2ReadWebpage() {
        return PERMISSION_READ;
    }

    private static class AccessCleaningDOMOutputProcessor extends AbstractDOMOutputProcessor {

        @Override
        protected org.w3c.dom.Element printElement(FormatStack fstack, NamespaceStack nstack,
            org.w3c.dom.Document basedoc, Element element) {
            Attribute href = element.getAttribute("href");
            return (href == null || itemAccess(PERMISSION_READ, element, true)) ? super.printElement(fstack, nstack,
                basedoc, element) : null;
        }

    }
}
