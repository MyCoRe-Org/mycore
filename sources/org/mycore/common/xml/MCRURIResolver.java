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
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.transform.JDOMSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRDirectoryXML;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;
import org.mycore.frontend.servlets.MCRServlet;
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

    private static final MCRURIResolver singleton = new MCRURIResolver();

    private static ServletContext context;

    private MCRCache bytesCache;

    /**
     * Creates a new MCRURIResolver
     */
    private MCRURIResolver() {
        MCRConfiguration config = MCRConfiguration.instance();
        String prefix = "MCR.URIResolver.";
        int cacheSize = config.getInt(prefix + "StaticFiles.CacheSize", 100);
        bytesCache = new MCRCache(cacheSize);
        HashMap supportedSchemes = new HashMap(10, 1);//set to final size, loadfactor: full
        supportedSchemes.put("webapp", new MCRWebAppResolver());
        supportedSchemes.put("file", new MCRFileResolver());
        supportedSchemes.put("query", new MCRQueryResolver());
        supportedSchemes.put("ifs", new MCRIFSResolver());
        supportedSchemes.put("mcrobject", new MCRObjectResolver());
        supportedSchemes.put("http", new MCRHttpResolver());
        supportedSchemes.put("request", new MCRRequestResolver());
        supportedSchemes.put("session", new MCRSessionResolver());
        supportedSchemes.put("access", new MCRACLResolver());
        supportedSchemes.put("resource", new MCRResourceResolver());
        SUPPORTED_SCHEMES = Collections.unmodifiableMap(supportedSchemes);
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
        if (base != null) {
            LOGGER.debug("Including " + href + " from " + getFileName(base));
        } else {
            LOGGER.debug("Including " + href);
        }

        if (href.indexOf(":") == -1) {
            return null;
        }

        String scheme = getScheme(href);

        if ("resource request webapp file ifs session query mcrobject access".indexOf(scheme) != -1) {
            return new JDOMSource(resolve(href));
        }
        return null;
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
    
        InputStream is = getCachedResource(getFileName(systemId));
    
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
            InputStream in = this.getClass().getResourceAsStream(classResource);

            if (in == null) {
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
     */
    public Element parseStream(InputStream in) {
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        builder.setEntityResolver(this);

        try {
            return builder.build(in).getRootElement();
        } catch (MalformedURLException e) {
            e.printStackTrace();

            String msg = "Exception while reading and parsing XML InputStream: " + e.getMessage();
            throw new MCRUsageException(msg, e);
        } catch (Exception ex) {
            String msg = "Exception while reading and parsing XML InputStream: " + ex.getMessage();
            throw new MCRUsageException(msg, ex);
        }
    }

    private static interface MCRResolver {
        public Element resolveElement(String URI);
    }

    private static class MCRObjectResolver implements MCRResolver {

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

            try {
                MCRXMLContainer result = new MCRXMLContainer();
                MCRObjectID mcrid = new MCRObjectID(id);
                byte[] xml = MCRXMLTableManager.instance().retrieve(mcrid);
                result.add("local", id, 0, xml);

                return result.exportAllToDocument().getRootElement();
            } catch (Exception ex) {
                LOGGER.debug("Exception while reading MCRObject as XML", ex);

                return null;
            }
        }

    }

    private static class MCRHttpResolver implements MCRResolver {

        /**
         * Reads XML from a http or https URL.
         * 
         * @param url
         *            the URL of the xml document
         * @return the root element of the xml document
         */
        public Element resolveElement(String url) {
            LOGGER.debug("Reading xml from url " + url);

            try {
                return MCRURIResolver.instance().parseStream(new URL(url).openStream());
            } catch (java.net.MalformedURLException ex) {
                String msg = "Malformed http url: " + url;
                throw new MCRUsageException(msg, ex);
            } catch (IOException ex) {
                String msg = "Unable to open input stream at " + url;
                throw new MCRUsageException(msg, ex);
            }
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
         */
        public Element resolveElement(String uri) {
            String path = uri.substring(uri.indexOf(":") + 1);
            LOGGER.debug("Reading xml from request " + path);

            StringBuffer url = new StringBuffer(MCRServlet.getBaseURL());
            url.append(path);

            if (path.indexOf("?") != -1) {
                url.append("&");
            } else {
                url.append("?");
            }

            url.append("MCRSessionID=");
            url.append(MCRSessionMgr.getCurrentSession().getID());

            return fallback.resolveElement(url.toString());
        }
    }

    private static class MCRFileResolver implements MCRResolver {

        /**
         * A cache of parsed XML files *
         */
        private MCRCache fileCache;

        public MCRFileResolver() {
            String prefix = "MCR.URIResolver.";
            int cacheSize = MCRConfiguration.instance().getInt(prefix + "StaticFiles.CacheSize", 100);
            fileCache = new MCRCache(cacheSize);
        }

        /**
         * Reads XML from a file URL.
         * 
         * @param uri
         *            the URL of the file in the format file://path/to/file
         * @return the root element of the xml document
         */
        public Element resolveElement(String uri) {
            String path = uri.substring("file://".length());
            LOGGER.debug("Reading xml from file " + path);

            File file = new File(path);
            Element fromCache = (Element) fileCache.getIfUpToDate(path, file.lastModified());

            if (fromCache != null) {
                return (Element) (fromCache.clone());
            }

            try {
                Element parsed = MCRURIResolver.instance().parseStream(new FileInputStream(file));
                fileCache.put(path, parsed);

                return (Element) (parsed.clone());
            } catch (FileNotFoundException ex) {
                String msg = "Could not find file for URI " + uri;
                throw new MCRUsageException(msg, ex);
            }
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
         */
        public Element resolveElement(String uri) {
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
         */
        public Element resolveElement(String uri) {
            Element parsed = MCRURIResolver.instance().parseStream(getResourceStream(uri));
            return parsed;
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

            // :NOTE: until real export format is defined: start some
            // selfdefined
            // one access Element per (Object-)ID
            Element container = new Element("access").setAttribute("id", objId);

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
                Element poolElement = new Element("pool").setAttribute("id", pool);
                poolElement.addContent(rule);
                root.addContent(poolElement);
            }
        }

    }
}
