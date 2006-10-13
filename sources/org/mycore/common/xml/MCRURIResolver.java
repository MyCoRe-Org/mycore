/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.DOMBuilder;
import org.jdom.input.SAXBuilder;
import org.jdom.transform.JDOMSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.classifications.query.Classification;
import org.mycore.datamodel.classifications.query.ClassificationTransformer;
import org.mycore.datamodel.classifications.query.MCRClassificationQuery;
import org.mycore.datamodel.ifs.MCRDirectoryXML;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.services.fieldquery.MCRQueryClient;
import org.mycore.services.query.MCRQueryCache;

/**
 * Reads XML documents from various URI types. This resolver is used to read
 * DTDs, XML Schema files, XSL document() usages, xsl:include usages and MyCoRe
 * Editor include declarations. DTDs and Schema files are read from the
 * CLASSPATH of the application when XML is parsed. XML document() calls and
 * xsl:include calls within XSL stylesheets can be read from URIs of type
 * resource, webapp, file, session, query or mcrobject. MyCoRe editor include
 * declarations can read XML files from resource, webapp, file, session, http or
 * https, query, or mcrobject URIs.
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRURIResolver implements javax.xml.transform.URIResolver, EntityResolver {
    private static final Logger LOGGER = Logger.getLogger(MCRURIResolver.class);
    
    private static Map SUPPORTED_SCHEMES;

    private static final String CONFIG_PREFIX = "MCR.UriResolver.";

    private static final MCRResolverProvider EXT_RESOLVER = (MCRResolverProvider) MCRConfiguration.instance().getInstanceOf(
            CONFIG_PREFIX + "externalResolver.class", MCREmptyResolverProvider.class.getName());

    private static final MCRURIResolver singleton = new MCRURIResolver();

    private static ServletContext context;
    
    final static String SESSION_OBJECT_NAME="URI_RESOLVER_DEBUG";  

    private MCRCache bytesCache;

    /**
     * Creates a new MCRURIResolver
     */
    private MCRURIResolver() {
        MCRConfiguration config = MCRConfiguration.instance();
        String prefix = "MCR.URIResolver.";
        int cacheSize = config.getInt(prefix + "StaticFiles.CacheSize", 100);
        bytesCache = new MCRCache(cacheSize);
        SUPPORTED_SCHEMES = Collections.unmodifiableMap(getResolverMapping());
    }

    private HashMap getResolverMapping() {
        final Map extResolverMapping = EXT_RESOLVER.getResolverMapping();
        // set Map to final size with loadfactor: full
        HashMap supportedSchemes = new HashMap(10 + extResolverMapping.size(), 1);
        // don't let interal mapping be overwritten
        supportedSchemes.putAll(extResolverMapping);
        supportedSchemes.put("webapp", new MCRWebAppResolver());
        supportedSchemes.put("file", new MCRFileResolver());
        supportedSchemes.put("query", new MCRQueryResolver());
        supportedSchemes.put("ifs", new MCRIFSResolver());
        supportedSchemes.put("mcrobject", new MCRObjectResolver());
        supportedSchemes.put("mcrws", new MCRWSResolver());
        supportedSchemes.put("http", new MCRHttpResolver());
        supportedSchemes.put("request", new MCRRequestResolver());
        supportedSchemes.put("session", new MCRSessionResolver());
        supportedSchemes.put("access", new MCRACLResolver());
        supportedSchemes.put("resource", new MCRResourceResolver());
        supportedSchemes.put("localclass", new MCRLocalClassResolver());
        supportedSchemes.put("classification", new MCRClassificationResolver());
        return supportedSchemes;
    }

    /**
     * Returns the MCRURIResolver singleton
     */
    public static MCRURIResolver instance() {
        return singleton;
    }

    /**
     * Initializes the MCRURIResolver for servlet applications.
     * 
     * @param ctx
     *            the servlet context of this web application
     * @param webAppBase
     *            the base URL of this web application
     */
    public static synchronized void init(ServletContext ctx, String webAppBase) {
        context = ctx;
        LOGGER.debug("parameter webAppBase (" + webAppBase + ") will not be used");
        // FIXME: use webAppBase or remove it
    }

    /**
     * URI Resolver that resolves XSL document() or xsl:include calls.
     * 
     * @see javax.xml.transform.URIResolver
     */
    public Source resolve(String href, String base) throws TransformerException {
        if (LOGGER.isDebugEnabled()){
            if (base != null) {
                final String baseFileName = getFileName(base);
                LOGGER.debug("Including " + href + " from " + baseFileName);
                addDebugInfo(href, baseFileName);
            } else {
                LOGGER.debug("Including " + href);
                addDebugInfo(href, null);
            }
        }        
        if (href.indexOf(":") == -1) {
            return null;
        }

        String scheme = getScheme(href);

        if (SUPPORTED_SCHEMES.containsKey(scheme)) {
            try {
                return new JDOMSource(resolveURI(href));
            } catch (Exception e) {
                throw new TransformerException("Error while resolving: " + href, e);
            }
        }
        return null;
    }
    
    private void addDebugInfo(String href, String base){
        final MCRSession session=MCRSessionMgr.getCurrentSession();
        Object obj=session.get(SESSION_OBJECT_NAME);
        if (obj==null){
            LOGGER.debug("Please use MCRURIResolverFilter to add debug informations to HTML pages.");
            return;
        }
        List list=(List)obj;
        list.add(href+" from "+base);
    }

    /**
     * Implements the SAX EntityResolver interface. This resolver type is used
     * to read DTDs and XML Schema files when parsing XML documents. This
     * resolver searches such files in the CLASSPATH of the current application.
     * 
     * @see org.xml.sax.EntityResolver
     */
    public InputSource resolveEntity(String publicId, String systemId) throws org.xml.sax.SAXException, java.io.IOException {
        LOGGER.debug("Resolving " + publicId + " :: " + systemId);

        if (systemId == null) {
            return null; // Use default resolver
        }

        InputStream is = getCachedResource("/" + getFileName(systemId));

        if (is == null) {
            return null; // Use default resolver
        }

        LOGGER.debug("Reading " + getFileName(systemId));

        return new InputSource(is);
    }

    /**
     * Returns the filename part of a path.
     * 
     * @param path
     *            the path of a file
     * @return the part after the last / or \\
     */
    private String getFileName(String path) {
        int posA = path.lastIndexOf("/");
        int posB = path.lastIndexOf("\\");
        int pos = ((posA == -1) ? posB : posA);

        return ((pos == -1) ? path : path.substring(pos + 1));
    }

    private InputStream getCachedResource(String classResource) throws IOException {
        byte[] bytes = (byte[]) (bytesCache.get(classResource));

        if (bytes == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            LOGGER.debug("Resolving resource " + classResource);
            InputStream in = this.getClass().getResourceAsStream(classResource);

            if (in == null) {
                LOGGER.debug(classResource + " not found");
                return null;
            }

            MCRUtils.copyStream(in, baos);
            baos.close();
            in.close();
            bytes = baos.toByteArray();
            bytesCache.put(classResource, bytes);
        }

        return new ByteArrayInputStream(bytes);
    }

    /**
     * Reads XML from URIs of various type.
     * 
     * @param uri
     *            the URI where to read the XML from
     * @return the root element of the XML document
     */
    public Element resolve(String uri) {
        if (LOGGER.isDebugEnabled()){
            addDebugInfo(uri, "JAVA method invocation");
        }
        try {
            /**
             * rethrow Exception as RuntimException
             * TODO: need to refactor this and declare throw in method signature
             */
            return resolveURI(uri);
        } catch (Exception e) {
            throw new MCRException("Error while resolving: " + uri, e);
        }
    }

    private Element resolveURI(String uri) throws Exception {
        LOGGER.info("Reading xml from uri " + uri);
        String scheme = getScheme(uri);
        return getResolver(scheme).resolveElement(uri);
    }

    /**
     * Returns the protocol or scheme for the given URI.
     * 
     * @param uri
     *            the URI to parse
     * @return the protocol/scheme part before the ":"
     */
    public String getScheme(String uri) {
        return new StringTokenizer(uri, ":").nextToken();
    }

    MCRResolver getResolver(String scheme) {
        if (SUPPORTED_SCHEMES.containsKey(scheme)) {
            return (MCRResolver) SUPPORTED_SCHEMES.get(scheme);
        }
        String msg = "Unsupported scheme type: " + scheme;
        throw new MCRUsageException(msg);
    }

    /**
     * Reads xml from an InputStream and returns the parsed root element.
     * 
     * @param in
     *            the InputStream that contains the XML document
     * @return the root element of the parsed input stream
     * @throws IOException 
     * @throws JDOMException 
     */
    protected Element parseStream(InputStream in) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        builder.setEntityResolver(this);

        return builder.build(in).getRootElement();
    }

    /**
     * Resolver interface. use this to implement custom URI schemes.
     * 
     * @author Thomas Scheffler (yagee)
     * 
     * @version $Revision$ $Date$
     */
    public static interface MCRResolver {
        /**
         * resolves an Element for XSLT process
         * 
         * @param URI
         *            String in URI Syntax
         * @return
         * @throws Exception
         */
        public Element resolveElement(String URI) throws Exception;
    }
    
    /**
     * provides a URI -- Resolver Mapping
     * 
     * One can implement this interface to provide additional URI schemes this MCRURIResolver should handle, too.
     * To add your mapping you have to set the <code>MCR.UriResolver.externalResolver.class</code> property to the implementing class.
     * 
     * @author Thomas Scheffler
     *
     * @version $Revision$ $Date$
     */
    public static interface MCRResolverProvider {
        /**
         * provides a Map of Resolver mappings.
         * 
         * Key is the scheme, e.g. <code>http</code>, where value is an instance of MCRResolver.
         * @see MCRResolver
         * @return a Map of Resolver mappings
         */
        public Map getResolverMapping();
    }
    
    private static class MCREmptyResolverProvider implements MCRResolverProvider{
        
        public Map getResolverMapping() {
            return Collections.EMPTY_MAP;
        }
        
    }

    private static class MCRObjectResolver implements MCRResolver {
        protected static final SAXBuilder SAX_BUILDER = new org.jdom.input.SAXBuilder();


        /**
         * Reads local MCRObject with a given ID from the store.
         * 
         * @param uri
         *            for example, "mcrobject:DocPortal_document_07910401"
         * @returns XML representation from MCRXMLContainer
         */
        public Element resolveElement(String uri) {
            String id = uri.substring(uri.indexOf(":") + 1);
            LOGGER.debug("Reading MCRObject with ID " + id);

            MCRObjectID mcrid = new MCRObjectID(id);
            Document doc = MCRXMLTableManager.instance().readDocument(mcrid);

            LOGGER.debug("end resolving " + uri);
            return doc.getRootElement();
        }

    }
    
    private static class MCRWSResolver implements MCRResolver {
        // TODO: add support for remote classifications

        private static final String HOST_KEY = "host";

        private static final String OPERATION_KEY = "operation";

        //parameter for MCRDoRetrieveObject
        private static final String OBJECT_KEY = "ID";

        //parameter for MCRDoRetrieveClassification
        private static final String LEVEL_KEY = "level";
        private static final String TYPE_KEY = "type";
        private static final String CLASS_KEY = "classid";
        private static final String CATEG_KEY = "categid";
        private static final String FORMAT_KEY = "format";

        private static final DOMBuilder DOM_BUILDER = new DOMBuilder();

        public Element resolveElement(String uri) {
            String key = uri.substring(uri.indexOf(":") + 1);
            LOGGER.debug("Reading xml from WebService using key :" + key);

            String[] param;
            StringTokenizer tok = new StringTokenizer(key, "&");
            HashMap params = new HashMap();
            while (tok.hasMoreTokens()) {
                param = tok.nextToken().split("=");
                params.put(param[0], param[1]);
            }
            if (!params.containsKey(HOST_KEY) || !params.containsKey(OPERATION_KEY)) {
                LOGGER.warn("Either 'host' or 'operation' is not defined. Returning NULL.");
                return null;
            }
            if (params.get(OPERATION_KEY).equals("MCRDoRetrieveObject")) {
                org.w3c.dom.Document document = MCRQueryClient.doRetrieveObject(params.get(HOST_KEY).toString(), params.get(OBJECT_KEY).toString());
                return DOM_BUILDER.build(document).detachRootElement();
            }
            if (params.get(OPERATION_KEY).equals("MCRDoRetrieveClassification")) {
                String hostAlias=params.get(HOST_KEY).toString();
                String level=params.get(LEVEL_KEY).toString();
                String type=params.get(TYPE_KEY).toString();
                String classId=params.get(CLASS_KEY).toString();
                String categId=params.get(CATEG_KEY).toString();
                String format=params.get(FORMAT_KEY).toString();
                org.w3c.dom.Document document = MCRQueryClient.doRetrieveClassification(hostAlias, level, type, classId, categId, format);
                return DOM_BUILDER.build(document).detachRootElement();
            }
            // only WS "MCRDoRetrieveObject" implemented yet
            LOGGER.warn("Unknown 'operation' requested. Returning NULL.");
            return null;
        }

    }

    private static class MCRHttpResolver implements MCRResolver {

        /**
         * Reads XML from a http or https URL.
         * 
         * @param url
         *            the URL of the xml document
         * @return the root element of the xml document
         * @throws IOException 
         * @throws JDOMException 
         * @throws MalformedURLException 
         */
        public Element resolveElement(String url) throws MalformedURLException, JDOMException, IOException {
            LOGGER.debug("Reading xml from url " + url);

            return MCRURIResolver.instance().parseStream(new URL(url).openStream());
        }

    }

    private static class MCRRequestResolver implements MCRResolver {
        MCRResolver fallback;

        public MCRRequestResolver() {
            fallback = new MCRHttpResolver();
        }

        /**
         * Reads XML from a HTTP request to this web application.
         * 
         * @param uri
         *            the URI in the format request:path/to/servlet
         * @return the root element of the xml document
         * @throws Exception 
         */
        public Element resolveElement(String uri) throws Exception {
            String path = uri.substring(uri.indexOf(":") + 1);
            LOGGER.debug("Reading xml from request " + path);

            StringBuffer url = new StringBuffer(MCRServlet.getBaseURL());
            url.append(path);

            final MCRSession currentSession = MCRSessionMgr.getCurrentSession();
            final Object httpSessionID = currentSession.get("http.session");
            final String finalURL;
            if (httpSessionID == null) {
                if (path.indexOf("?") != -1) {
                    url.append("&");
                } else {
                    url.append("?");
                }
                url.append("MCRSessionID=");
                url.append(currentSession.getID());
                finalURL = url.toString();
            } else {
                finalURL = toEncoded(url.toString(), httpSessionID.toString());
            }

            return fallback.resolveElement(finalURL);
        }

        private String toEncoded(String url, String sessionId) {

            if ((url == null) || (sessionId == null)) {
                return (url);
            }
            String path = url;
            String query = "";
            int queryPos = url.indexOf('?');
            if (queryPos >= 0) {
                path = url.substring(0, queryPos);
                query = url.substring(queryPos);
            }
            StringBuffer sb = new StringBuffer(path);
            sb.append(";jsessionid=");
            sb.append(sessionId);
            sb.append(query);
            return (sb.toString());

        }
    }

    private static class MCRFileResolver implements MCRResolver {

        /**
         * A cache of parsed XML files *
         */
        private MCRCache fileCache;

        public MCRFileResolver() {
            int cacheSize = MCRConfiguration.instance().getInt(CONFIG_PREFIX + "StaticFiles.CacheSize", 100);
            fileCache = new MCRCache(cacheSize);
        }

        /**
         * Reads XML from a file URL.
         * 
         * @param uri
         *            the URL of the file in the format file://path/to/file
         * @return the root element of the xml document
         * @throws IOException 
         * @throws JDOMException 
         * @throws FileNotFoundException 
         */
        public Element resolveElement(String uri) throws FileNotFoundException, JDOMException, IOException {
            String path = uri.substring("file://".length());
            LOGGER.debug("Reading xml from file " + path);

            File file = new File(path);
            Element fromCache = (Element) fileCache.getIfUpToDate(path, file.lastModified());

            if (fromCache != null) {
                return (Element) (fromCache.clone());
            }

            Element parsed = MCRURIResolver.instance().parseStream(new FileInputStream(file));
            fileCache.put(path, parsed);

            return (Element) (parsed.clone());
        }

    }

    private static class MCRWebAppResolver implements MCRResolver {
        MCRResolver fallback;

        public MCRWebAppResolver() {
            fallback = new MCRFileResolver();
        }

        /**
         * Reads XML from a HTTP request to this web application.
         * 
         * @param uri
         *            the URI in the format request:path/to/servlet
         * @return the root element of the xml document
         * @throws Exception 
         */
        public Element resolveElement(String uri) throws Exception {
            String path = uri.substring(uri.indexOf(":") + 1);
            LOGGER.debug("Reading xml from webapp " + path);
            uri = "file://" + context.getRealPath(path);

            return fallback.resolveElement(uri);
        }
    }

    private static class MCRResourceResolver implements MCRResolver {

        private InputStream getResourceStream(String uri) {
            String path = uri.substring(uri.indexOf(":") + 1);
            LOGGER.debug("Reading xml from classpath resource " + path);

            return this.getClass().getResourceAsStream("/" + path);
        }

        /**
         * Reads XML from the CLASSPATH of the application.
         * 
         * @param uri
         *            the location of the file in the format
         *            resource:path/to/file
         * @return the root element of the XML document
         * @throws IOException 
         * @throws JDOMException 
         */
        public Element resolveElement(String uri) throws JDOMException, IOException {
            Element parsed = MCRURIResolver.instance().parseStream(getResourceStream(uri));
            return parsed;
        }

    }

    private static class MCRLocalClassResolver implements MCRResolver {

        /**
         * Delivers a jdom Element created by any local class that implements
         * MCRResolver
         * 
         * @param uri
         *            the class name of the file in the format
         *            localclass:org.mycore.ClassName?mode=getAll
         * 
         * @return the root element of the XML document
         * @throws Exception 
         */
        public Element resolveElement(String uri) throws Exception {
            String classname = uri.substring(uri.indexOf(":") + 1, uri.indexOf("?"));
            Class cl = null;
            Logger.getLogger(this.getClass()).debug("Loading Class: " + classname);
            cl = Class.forName(classname);
            Object o = cl.newInstance();
            MCRResolver resolver = (MCRResolver) o;
            return resolver.resolveElement(uri);
        }

    }

    private static class MCRSessionResolver implements MCRResolver {

        /**
         * Reads XML from URIs of type session:key. The method MCRSession.get(
         * key ) is called and must return a JDOM element.
         * 
         * @see org.mycore.common.MCRSession#get( java.lang.String )
         * 
         * @param uri
         *            the URI in the format session:key
         * @return the root element of the xml document
         */
        public Element resolveElement(String uri) {
            String key = uri.substring(uri.indexOf(":") + 1);

            LOGGER.debug("Reading xml from session using key " + key);

            Object value = MCRSessionMgr.getCurrentSession().get(key);

            return (Element) (((Element) value).clone());
        }

    }

    private static class MCRIFSResolver implements MCRResolver {

        /**
         * Reads XML from a http or https URL.
         * 
         * @param uri
         *            the URL of the xml document
         * @return the root element of the xml document
         */
        public Element resolveElement(String uri) {
            LOGGER.debug("Reading xml from url " + uri);

            String path = uri.substring(uri.indexOf(":") + 1);

            String hosts = null;
            int i = path.indexOf("?host");
            if (i > 0) {
                hosts = path.substring(i + 1 + 6);// "?host=".length()
                path = path.substring(0, i);
            }
            return MCRDirectoryXML.getInstance().getDirectory(path, hosts).getRootElement();
        }

    }

    private static class MCRQueryResolver implements MCRResolver {

        private static final String HOST_PARAM = "host";

        private static final String TYPE_PARAM = "type";

        private static final String QUERY_PARAM = "query";

        private static final String HOST_DEFAULT = "local";

        private static final String URL_ENCODING = MCRConfiguration.instance().getString("MCR.request_charencoding", "UTF-8");

        /**
         * Returns query results as XML
         */
        public Element resolveElement(String uri) {
            String key = uri.substring(uri.indexOf(":") + 1);
            LOGGER.debug("Reading xml from query result using key :" + key);

            String[] param;
            String host;
            String type;
            String query;
            StringTokenizer tok = new StringTokenizer(key, "&");
            Hashtable params = new Hashtable();

            while (tok.hasMoreTokens()) {
                param = tok.nextToken().split("=");
                params.put(param[0], param[1]);
            }

            if (params.get(HOST_PARAM) == null) {
                host = HOST_DEFAULT;
            } else {
                host = (String) params.get(HOST_PARAM);
            }

            type = (String) params.get(TYPE_PARAM);
            query = (String) params.get(QUERY_PARAM);

            if (type == null) {
                return null;
            }

            StringTokenizer hosts = new StringTokenizer(host, ",");
            MCRXMLContainer results = new MCRXMLContainer();

            while (hosts.hasMoreTokens()) {
                try {
                    results.importElements(query(hosts.nextToken(), type, query));
                } catch (NumberFormatException e) {
                    LOGGER.error("Error while processing query: " + key, e);
                } catch (UnsupportedEncodingException e) {
                    LOGGER.error("Error while processing query: " + key, e);
                }
            }

            return results.exportAllToDocument().getRootElement();
        }

        private MCRXMLContainer query(String host, String type, String query) throws NumberFormatException, UnsupportedEncodingException {
            if (query == null) {
                query = "";
            }

            return MCRQueryCache.getResultList(URLDecoder.decode(host, URL_ENCODING), URLDecoder.decode(query, URL_ENCODING), URLDecoder.decode(type,
                    URL_ENCODING), MCRConfiguration.instance().getInt("MCR.query_max_results", 10));
        }

    }

    private static class MCRACLResolver implements MCRResolver {

        private static final String ACTION_PARAM = "action";

        private static final String OBJECT_ID_PARAM = "object";

        /**
         * Returns access controll rules as XML
         */
        public Element resolveElement(String uri) {
            String key = uri.substring(uri.indexOf(":") + 1);
            LOGGER.debug("Reading xml from query result using key :" + key);

            String[] param;
            StringTokenizer tok = new StringTokenizer(key, "&");
            Hashtable params = new Hashtable();

            while (tok.hasMoreTokens()) {
                param = tok.nextToken().split("=");
                params.put(param[0], param[1]);
            }

            String action = (String) params.get(ACTION_PARAM);
            String objId = (String) params.get(OBJECT_ID_PARAM);

            if (action == null || objId == null) {
                return null;
            }

            Element container = new Element("servacls").setAttribute("class", "MCRMetaAccessRule");

            MCRAccessInterface AI = MCRAccessManager.getAccessImpl();

            if (action.equals("all")) {
                Iterator it = AI.getPermissionsForID(objId).iterator();
                while (it.hasNext()) {
                    action = it.next().toString();
                    // one pool Element under access per defined AccessRule in
                    // Pool
                    // for (Object-)ID
                    addRule(container, action, AI.getRule(objId, action));
                }
            } else {
                addRule(container, action, AI.getRule(objId, action));
            }

            return container;
        }

        private void addRule(Element root, String pool, Element rule) {
            if (rule != null && pool != null) {
                Element poolElement = new Element("servacl").setAttribute("permission", pool);
                poolElement.addContent(rule);
                root.addContent(poolElement);
            }
        }

    }

    private static class MCRClassificationResolver implements MCRResolver {

        private static final Pattern EDITORFORMAT_PATTERN = Pattern.compile("(\\[)([^\\]]*)(\\])");

        private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

        private static final String FORMAT_CONFIG_PREFIX = CONFIG_PREFIX+"classification.format.";
        
        private static MCRCache CLASS_CACHE;
        
        private static long CACHE_INIT_TIME;

        
        public MCRClassificationResolver(){
            initCache();
        }

        private void initCache() {
            int cacheSize = MCRConfiguration.instance().getInt(CONFIG_PREFIX + "classification.CacheSize", 1000);
            CLASS_CACHE = new MCRCache(cacheSize);
            CACHE_INIT_TIME=System.currentTimeMillis();
        }

        /**
         * returns a classification in a specific format.
         * 
         * Syntax:
         * <code>classification:{editor['['formatAlias']']|metadata}:{Levels}:{parents|children}:{ClassID}[:CategID]
         * 
         * formatAlias: MCRConfiguration property MCR.UriResolver.classification.format.formatAlias
         * 
         * @param uri
         *            URI in the syntax above
         *            
         * @return the root element of the XML document
         * @see ClassificationTransformer#getEditorDocument(Classification, String)
         */
        public Element resolveElement(String uri) {
            LOGGER.debug("start resolving "+uri);
            Element returns;
            if (CONFIG.getSystemLastModified() > CACHE_INIT_TIME){
                initCache();
                returns = getClassElement(uri);
                CLASS_CACHE.put(uri,returns);
            } else {
                returns=(Element)CLASS_CACHE.get(uri);
                if (returns==null){
                    returns = getClassElement(uri);
                    CLASS_CACHE.put(uri,returns);
                }
            }
            return returns;
        }

        private Element getClassElement(String uri) {
            String[] parameters = uri.split(":");
            if (parameters.length<4){
                //sanity check
                throw new IllegalArgumentException("Invalid format of uri for retrieval of classification: "+uri);
            }
            String format = parameters[1];
            String levelS = parameters[2];
            String axis = parameters[3];
            String classID = parameters[4];
            int levels;
            if (levelS.equals("all")) {
                levels = -1;
            } else {
                levels = Integer.parseInt(levelS);
            }
            StringBuffer categID = new StringBuffer();
            for (int i = 5; i < parameters.length; i++) {
                categID.append(':').append(parameters[i]);
            }
            if (categID.length() > 0) {
                // categID was specified
                // remove leading ":" from the for block above
                categID.deleteCharAt(0);
            }
            String categ = categID.toString();
            Classification cl=null;
            String labelFormat = getLabelFormat(format);
            boolean withCounter=false;
            if ((labelFormat!=null) && (labelFormat.indexOf("{count}")!=-1)){
                withCounter=true;
            }
            LOGGER.debug("start ClassificationQuery");
            if (axis.equals("children")) {
                if (categ.length() > 0) {
                    cl = MCRClassificationQuery.getClassification(classID, categ, levels, withCounter);
                } else {
                    cl = MCRClassificationQuery.getClassification(classID, levels, withCounter);
                }
            } else if (axis.equals("parents")){
                if (categ.length()==0){
                    LOGGER.error("Cannot resolve parent axis without a CategID. URI: "+uri);
                    throw new IllegalArgumentException("Invalid format (categID is required in mode 'parents') of uri for retrieval of classification: "+uri);
                }
                cl = MCRClassificationQuery.getClassificationHierarchie(classID,categ,levels, withCounter);
            }
            
            Element returns;
            LOGGER.debug("start transformation of ClassificationQuery");
            if (format.startsWith("editor")) {
                if (labelFormat == null) {
                    returns = ClassificationTransformer.getEditorDocument(cl).getRootElement();
                } else {
                    returns = ClassificationTransformer.getEditorDocument(cl, labelFormat).getRootElement();
                }
            } else if (format.equals("metadata")) {
                returns = ClassificationTransformer.getMetaDataDocument(cl).getRootElement();
            } else {
                LOGGER.error("Unknown target format given. URI: "+uri);
                throw new IllegalArgumentException("Invalid target format ("+format+ ") in uri for retrieval of classification: "+uri);
            }
            LOGGER.debug("end resolving "+uri);
            return returns;
        }

        private static String getLabelFormat(String editorString) {
            Matcher m = EDITORFORMAT_PATTERN.matcher(editorString);
            if ((m.find()) && (m.groupCount() == 3)) {
                String formatDef = m.group(2);
                return CONFIG.getString(FORMAT_CONFIG_PREFIX + formatDef);
            }
            return null;
        }

    }

}
