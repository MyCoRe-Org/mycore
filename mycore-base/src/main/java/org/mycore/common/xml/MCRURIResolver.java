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

package org.mycore.common.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRCoreVersion;
import org.mycore.common.MCRException;
import org.mycore.common.MCRHTTPClient;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.common.xsl.MCRLazyStreamSource;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.common.MCRAbstractMetadataVersion;
import org.mycore.datamodel.common.MCRDataURL;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRFileMetadata;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.datamodel.niofs.MCRPathXML;
import org.mycore.frontend.MCRLayoutUtilities;
import org.mycore.resource.MCRResourceHelper;
import org.mycore.resource.MCRResourcePath;
import org.mycore.services.http.MCRURLQueryParameter;
import org.mycore.services.i18n.MCRTranslation;
import org.mycore.tools.MCRObjectFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import jakarta.servlet.ServletContext;

/**
 * Reads XML documents from various URI types. This resolver is used to read DTDs, XML Schema files, XSL document()
 * usages, xsl:include usages and MyCoRe Editor include declarations. DTDs and Schema files are read from the CLASSPATH
 * of the application when XML is parsed. XML document() calls and xsl:include calls within XSL stylesheets can be read
 * from URIs of type resource, webapp, file, session, query or mcrobject. MyCoRe editor include declarations can read
 * XML files from resource, webapp, file, session, http or https, query, or mcrobject URIs.
 *
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 */
public final class MCRURIResolver implements URIResolver {
    static final Logger LOGGER = LogManager.getLogger(MCRURIResolver.class);

    static final String SESSION_OBJECT_NAME = "URI_RESOLVER_DEBUG";

    private static final String CONFIG_PREFIX = "MCR.URIResolver.";

    private static final String HTTP_CLIENT_CLASS = "MCR.HTTPClient.Class";

    private static final Marker UNIQUE_MARKER = MarkerManager.getMarker("tryResolveXML");

    private static Map<String, URIResolver> SUPPORTED_SCHEMES;

    private static MCRResolverProvider EXT_RESOLVER;

    private static MCRURIResolver singleton;

    private static ServletContext context;

    static {
        try {
            reInit();
            singleton = new MCRURIResolver();
        } catch (Exception exc) {
            LOGGER.error("Unable to initialize MCRURIResolver", exc);
        }
    }

    public static void reInit() {
        EXT_RESOLVER = getExternalResolverProvider();
        SUPPORTED_SCHEMES = Collections.unmodifiableMap(getResolverMapping());
    }

    private static MCRResolverProvider getExternalResolverProvider() {
        return MCRConfiguration2.getClass(CONFIG_PREFIX + "ExternalResolver.Class")
            .map(c -> {
                try {
                    return (MCRResolverProvider) c.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    LOGGER.warn("Could not instantiate external Resolver class", e);
                    return null;
                }
            }).orElse(HashMap::new);
    }

    private static void findAndThrowTransformerException(Exception e) throws TransformerException {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof TransformerException te) {
                throw te;
            }
            cause = cause.getCause();
        }
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
     */
    public static synchronized void init(ServletContext ctx) {
        context = ctx;
    }

    public static Hashtable<String, String> getParameterMap(String key) {
        String[] param;
        StringTokenizer tok = new StringTokenizer(key, "&");
        Hashtable<String, String> params = new Hashtable<>();

        while (tok.hasMoreTokens()) {
            param = tok.nextToken().split("=");
            params.put(param[0], param.length >= 2 ? param[1] : "");
        }
        return params;
    }

    static URI resolveURI(String href, String base) {
        return Optional.ofNullable(base)
            .map(URI::create)
            .map(u -> u.resolve(href))
            .orElse(URI.create(href));
    }

    public static ServletContext getServletContext() {
        return context;
    }

    private static HashMap<String, URIResolver> getResolverMapping() {
        final Map<String, URIResolver> extResolverMapping = EXT_RESOLVER.getURIResolverMapping();
        extResolverMapping.putAll(new MCRModuleResolverProvider().getURIResolverMapping());
        // set Map to final size with loadfactor: full
        HashMap<String, URIResolver> supportedSchemes = new HashMap<>(10 + extResolverMapping.size(), 1);
        // don't let interal mapping be overwritten
        supportedSchemes.putAll(extResolverMapping);
        supportedSchemes.put("webapp", new MCRWebAppResolver());
        supportedSchemes.put("ifs", new MCRIFSResolver());
        supportedSchemes.put("mcrfile", new MCRMCRFileResolver());
        supportedSchemes.put("mcrobject", new MCRObjectResolver());
        supportedSchemes.put("session", new MCRSessionResolver());
        supportedSchemes.put("access", new MCRACLResolver());
        supportedSchemes.put("resource", new MCRResourceResolver());
        supportedSchemes.put("localclass", new MCRLocalClassResolver());
        supportedSchemes.put("classification", new MCRClassificationResolver());
        supportedSchemes.put("buildxml", new MCRBuildXMLResolver());
        supportedSchemes.put("catchEx", new MCRExceptionAsXMLResolver());
        supportedSchemes.put("notnull", new MCRNotNullResolver());
        supportedSchemes.put("xslStyle", new MCRXslStyleResolver());
        supportedSchemes.put("xslTransform", new MCRLayoutTransformerResolver());
        supportedSchemes.put("xslInclude", new MCRXslIncludeResolver());
        supportedSchemes.put("xslImport", new MCRXslImportResolver());
        supportedSchemes.put("currentUserInfo", new MCRCurrentUserInfoResolver());
        supportedSchemes.put("version", new MCRVersionResolver());
        supportedSchemes.put("layoutUtils", new MCRLayoutUtilsResolver());
        supportedSchemes.put("versioninfo", new MCRVersionInfoResolver());
        supportedSchemes.put("deletedMcrObject", new MCRDeletedObjectResolver());
        supportedSchemes.put("fileMeta", new MCRFileMetadataResolver());
        supportedSchemes.put("basket", new org.mycore.frontend.basket.MCRBasketResolver());
        supportedSchemes.put("language", new org.mycore.datamodel.language.MCRLanguageResolver());
        supportedSchemes.put("redirect", new MCRRedirectResolver());
        supportedSchemes.put("data", new MCRDataURLResolver());
        supportedSchemes.put("i18n", new MCRI18NResolver());
        supportedSchemes.put("checkPermissionChain", new MCRCheckPermissionChainResolver());
        supportedSchemes.put("checkPermission", new MCRCheckPermissionResolver());
        MCRRESTResolver restResolver = new MCRRESTResolver();
        supportedSchemes.put("http", restResolver);
        supportedSchemes.put("https", restResolver);
        supportedSchemes.put("file", new MCRFileResolver());
        supportedSchemes.put("cache", new MCRCachingResolver());
        return supportedSchemes;
    }

    /**
     * Tries to calculate the resource uri to the directory of the stylesheet that includes the given file.
     *
     * @param base the base uri of the stylesheet that includes the given file
     * @return the resource uri to the directory of the stylesheet that includes the given file.
     */
    static String getParentDirectoryResourceURI(String base) {
        if (base == null) {
            // the file was not included from another file, so we need to use the default resource directory
            final String xslFolder = MCRConfiguration2.getStringOrThrow("MCR.Layout.Transformer.Factory.XSLFolder");
            return "resource:" + xslFolder + "/";
        } else {
            String resolvingBase = null;
            MCRResourcePath resourcePath = MCRResourceHelper.getResourcePath(base);
            if (resourcePath != null) {
                String path = resourcePath.asRelativePath();
                resolvingBase = "resource:" + path.substring(0, path.lastIndexOf('/') + 1);
            }

            return resolvingBase;
        }
    }

    /**
     * URI Resolver that resolves XSL document() or xsl:include calls.
     *
     * @see javax.xml.transform.URIResolver
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        if (LOGGER.isDebugEnabled()) {
            if (base != null) {
                LOGGER.debug("Including {} from {}", href, base);
                addDebugInfo(href, base);
            } else {
                LOGGER.debug("Including {}", href);
                addDebugInfo(href, null);
            }
        }
        if (!href.contains(":")) {
            if (!href.endsWith(".xsl")) {
                return null;
            }
            return tryResolveXSL(href, base);
        }

        String scheme = getScheme(href, base);

        URIResolver uriResolver = SUPPORTED_SCHEMES.get(scheme);
        if (uriResolver != null) {
            Source resolved = uriResolver.resolve(href, base);
            if (resolved == null) {
                LOGGER.warn("Could not resolve URI for {} from {}", href, base);
                return null;
            }
            if (resolved.getSystemId() == null) {
                resolved.setSystemId(href);
            }
            return resolved;
        } else { // try to handle as URL, use default resolver for file:// and
            try {
                InputSource entity = MCREntityResolver.instance().resolveEntity(null, href);
                if (entity != null) {
                    LOGGER.debug("Resolved via EntityResolver: {}", entity.getSystemId());
                    return new MCRLazyStreamSource(entity::getByteStream, entity.getSystemId());
                }
            } catch (IOException e) {
                LOGGER.debug("Error while resolving uri: {}", href);
            }
            // http://
            if (href.endsWith("/") && scheme.equals("file")) {
                //cannot stream directories
                return null;
            }
            StreamSource streamSource = new StreamSource();
            streamSource.setSystemId(href);
            return streamSource;
        }
    }

    private Source tryResolveXSL(String href, String base) throws TransformerException {
        String baseUri = getParentDirectoryResourceURI(base);

        final String initialUri = baseUri + href;
        String saneHref = href;
        String saneBaseUri = baseUri;
        while (saneHref.startsWith("../") & saneBaseUri.endsWith("/")) {
            if (saneBaseUri.length() == 1) {
                throw new TransformerException("Relative href points outside of base URI:" + initialUri);
            }
            saneBaseUri = saneBaseUri.substring(0, saneBaseUri.lastIndexOf('/', saneBaseUri.length() - 2) + 1);
            saneHref = saneHref.substring("../".length());
        }

        final String finalUri = saneBaseUri + saneHref;
        LOGGER.debug("Trying to resolve {} from uri {}", saneHref, finalUri);
        Source newResolveMethodResult = SUPPORTED_SCHEMES.get("resource").resolve(finalUri, base);
        if (newResolveMethodResult != null) {
            return newResolveMethodResult;
        }

        // new relative include did not work, now fall back to old behaviour and print a warning if it works
        final String xslFolder = MCRConfiguration2.getStringOrThrow("MCR.Layout.Transformer.Factory.XSLFolder");
        Source oldResolveMethodResult = SUPPORTED_SCHEMES.get("resource")
            .resolve("resource:" + xslFolder + "/" + href, base);
        if (oldResolveMethodResult != null) {
            LOGGER.warn(UNIQUE_MARKER, "The Stylesheet {} has include {} which only works with an old " +
                "absolute include mechanism. Please change the include to relative!", base, href);
        }
        return oldResolveMethodResult;
    }

    private void addDebugInfo(String href, String base) {
        MCRURIResolverFilter.uriList.get().add(href + " from " + base);
    }

    /**
     * Reads XML from URIs of various type.
     *
     * @param uri
     *            the URI where to read the XML from
     * @return the root element of the XML document
     */
    public Element resolve(String uri) {
        if (LOGGER.isDebugEnabled()) {
            addDebugInfo(uri, "JAVA method invocation");
        }
        MCRSourceContent content;
        try {
            content = MCRSourceContent.getInstance(uri);
            return content == null ? null : content.asXML().getRootElement().detach();
        } catch (Exception e) {
            throw new MCRException("Error while resolving " + uri, e);
        }
    }

    /**
     * Returns the protocol or scheme for the given URI.
     *
     * @param uri
     *            the URI to parse
     * @param base
     *            if uri is relative, resolve scheme from base parameter
     * @return the protocol/scheme part before the ":"
     */
    public String getScheme(String uri, String base) {
        StringTokenizer uriTokenizer = new StringTokenizer(uri, ":");
        if (uriTokenizer.hasMoreTokens()) {
            return uriTokenizer.nextToken();
        }
        if (base != null) {
            uriTokenizer = new StringTokenizer(base, ":");
            if (uriTokenizer.hasMoreTokens()) {
                return uriTokenizer.nextToken();
            }
        }
        return null;
    }

    @Deprecated
    URIResolver getResolver(String scheme) {
        if (SUPPORTED_SCHEMES.containsKey(scheme)) {
            return SUPPORTED_SCHEMES.get(scheme);
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
    @Deprecated
    protected Element parseStream(InputStream in) throws JDOMException, IOException {
        final MCRStreamContent streamContent = new MCRStreamContent(in);
        return MCRXMLParserFactory
            .getNonValidatingParser()
            .parseXML(streamContent)
            .getRootElement();
    }

    /**
     * provides a URI -- Resolver Mapping One can implement this interface to provide additional URI schemes this
     * MCRURIResolver should handle, too. To add your mapping you have to set the
     * <code>MCR.URIResolver.ExternalResolver.Class</code> property to the implementing class.
     *
     * @author Thomas Scheffler
     */
    public interface MCRResolverProvider {
        /**
         * provides a Map of URIResolver mappings. Key is the scheme, e.g. <code>http</code>, where value is an
         * implementation of {@link URIResolver}.
         *
         * @see URIResolver
         * @return a Map of URIResolver mappings
         */
        Map<String, URIResolver> getURIResolverMapping();
    }

    public interface MCRXslIncludeHrefs {
        List<String> getHrefs();
    }

    private static class MCRModuleResolverProvider implements MCRResolverProvider {
        private final Map<String, URIResolver> resolverMap = new HashMap<>();

        MCRModuleResolverProvider() {
            MCRConfiguration2.getSubPropertiesMap(CONFIG_PREFIX + "ModuleResolver.")
                .forEach(this::registerUriResolver);
        }

        @Override
        public Map<String, URIResolver> getURIResolverMapping() {
            return resolverMap;
        }

        private void registerUriResolver(String scheme, String className) {
            try {
                resolverMap.put(scheme, MCRConfiguration2.instantiateClass(className));
            } catch (RuntimeException re) {
                throw new MCRException("Cannot instantiate " + className + " for URI scheme " + scheme, re);
            }
        }

    }

    private static class MCRFileResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            URI hrefURI = resolveURI(href, base);
            if (!hrefURI.getScheme().equals("file")) {
                throw new TransformerException("Unsupport file uri scheme: " + hrefURI.getScheme());
            }
            Path path = Paths.get(hrefURI);
            StreamSource source;
            try {
                source = new StreamSource(Files.newInputStream(path), hrefURI.toASCIIString());
                return source;
            } catch (IOException e) {
                throw new TransformerException(e);
            }
        }
    }

    private static class MCRRESTResolver implements URIResolver {
        private final MCRHTTPClient client;

        MCRRESTResolver() {
            this.client = MCRConfiguration2.getInstanceOfOrThrow(MCRHTTPClient.class, HTTP_CLIENT_CLASS);
        }

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            URI hrefURI = resolveURI(href, base);
            try {
                final Source source = client.get(hrefURI).getSource();
                source.setSystemId(hrefURI.toASCIIString());
                return source;
            } catch (IOException e) {
                throw new TransformerException(e);
            }
        }
    }

    private static class MCRObjectResolver implements URIResolver {

        /**
         * Reads local MCRObject with a given ID from the store.
         *
         * @param href
         *            for example, "mcrobject:DocPortal_document_07910401"
         * @return XML representation from MCRXMLContainer
         */
        @Override
        public Source resolve(String href, String base) throws TransformerException {
            String id = href.substring(href.indexOf(":") + 1);
            LOGGER.debug("Reading MCRObject with ID {}", id);
            Map<String, String> params;
            StringTokenizer tok = new StringTokenizer(id, "?");
            id = tok.nextToken();

            if (tok.hasMoreTokens()) {
                params = getParameterMap(tok.nextToken());
            } else {
                params = Collections.emptyMap();
            }

            MCRObjectID mcrid = MCRObjectID.getInstance(id);
            try {
                MCRXMLMetadataManager xmlmm = MCRXMLMetadataManager.instance();
                MCRContent content = params.containsKey("r")
                    ? xmlmm.retrieveContent(mcrid, params.get("r"))
                    : xmlmm.retrieveContent(mcrid);
                if (content == null) {
                    return null;
                }
                LOGGER.debug("end resolving {}", href);
                return content.getSource();
            } catch (IOException e) {
                throw new TransformerException(e);
            }
        }

    }

    /**
     * Reads XML from a static file within the web application. the URI in the format webapp:path/to/servlet
     */
    private static class MCRWebAppResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            String path = href.substring(href.indexOf(":") + 1);
            try {
                URL resource = MCRResourceHelper.getWebResourceUrl(path);
                if (resource != null) {
                    return new StreamSource(resource.toURI().toASCIIString());
                } else {
                    throw new TransformerException("Could not find web resource: " + path);
                }
            } catch (Exception ex) {
                throw new TransformerException("Could not load web resource: " + path, ex);
            }
        }
    }

    /**
     * Reads XML from the CLASSPATH of the application. the location of the file in the format resource:path/to/file
     */
    private static class MCRResourceResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            String path = href.substring(href.indexOf(":") + 1);
            URL resource = MCRResourceHelper.getResourceUrl(path);
            if (resource != null) {
                //have to use SAX here to resolve entities
                if (path.endsWith(".xsl")) {
                    XMLReader reader;
                    try {
                        reader = MCRXMLParserFactory.getNonValidatingParser().getXMLReader();
                    } catch (SAXException | ParserConfigurationException e) {
                        throw new TransformerException(e);
                    }
                    reader.setEntityResolver(MCREntityResolver.instance());
                    InputSource input = new InputSource(resource.toString());
                    SAXSource saxSource = new SAXSource(reader, input);
                    LOGGER.debug("include stylesheet: {}", saxSource.getSystemId());
                    return saxSource;
                } else {
                    return instance().resolve(resource.toString(), base);
                }
            }
            return null;
        }

    }

    /**
     * Delivers a jdom Element created by any local class that implements URIResolver
     * interface. the class name of the file in the format localclass:org.mycore.ClassName?mode=getAll
     */
    private static class MCRLocalClassResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            String classname = href.substring(href.indexOf(":") + 1, href.indexOf("?"));
            Class<? extends URIResolver> cl = null;
            LogManager.getLogger(this.getClass()).debug("Loading Class: {}", classname);
            URIResolver resolver;
            try {
                cl = MCRClassTools.forName(classname);
                resolver = cl.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new TransformerException(e);
            }
            return resolver.resolve(href, base);
        }

    }

    private static class MCRSessionResolver implements URIResolver {

        /**
         * Reads XML from URIs of type session:key. The method MCRSession.get( key ) is called and must return a JDOM
         * element.
         *
         * @see org.mycore.common.MCRSession#get(Object)
         * @param href
         *            the URI in the format session:key
         * @return the root element of the xml document
         */
        @Override
        public Source resolve(String href, String base) {
            String key = href.substring(href.indexOf(":") + 1);
            LOGGER.debug("Reading xml from session using key {}", key);
            Element value = (Element) MCRSessionMgr.getCurrentSession().get(key);
            return new JDOMSource(value.clone());
        }

    }

    private static class MCRIFSResolver implements URIResolver {

        /**
         * Reads XML from a http or https URL.
         *
         * @param href
         *            the URL of the xml document
         * @return the root element of the xml document
         */
        @Override
        public Source resolve(String href, String base) throws TransformerException {
            LOGGER.debug("Reading xml from url {}", href);

            String path = href.substring(href.indexOf(":") + 1);

            int i = path.indexOf("?host");
            if (i > 0) {
                path = path.substring(0, i);
            }
            StringTokenizer st = new StringTokenizer(path, "/");

            String ownerID = st.nextToken();
            try {
                String aPath = MCRXMLFunctions.decodeURIPath(path.substring(ownerID.length() + 1));
                // TODO: make this more pretty
                if (ownerID.endsWith(":")) {
                    ownerID = ownerID.substring(0, ownerID.length() - 1);
                }
                LOGGER.debug("Get {} path: {}", ownerID, aPath);
                return new JDOMSource(MCRPathXML.getDirectoryXML(MCRPath.getPath(ownerID, aPath)));
            } catch (IOException | URISyntaxException e) {
                throw new TransformerException(e);
            }
        }
    }

    private static class MCRMCRFileResolver implements URIResolver {
        @Override
        public Source resolve(String href, String base) throws TransformerException {
            LOGGER.debug("Reading xml from MCRFile {}", href);
            MCRPath file = null;
            String id = href.substring(href.indexOf(":") + 1);
            if (id.contains("/")) {
                // assume thats a derivate with path
                try {
                    MCRObjectID derivateID = MCRObjectID.getInstance(id.substring(0, id.indexOf("/")));
                    String path = id.substring(id.indexOf("/"));
                    file = MCRPath.getPath(derivateID.toString(), path);
                } catch (MCRException exc) {
                    // just check if the id is valid, don't care about the exception
                }
            }
            if (file == null) {
                throw new TransformerException("mcrfile: Resolver needs a path: " + href);
            }
            try {
                return new MCRPathContent(file).getSource();
            } catch (Exception e) {
                throw new TransformerException(e);
            }
        }
    }

    private static class MCRACLResolver implements URIResolver {

        private static final String ACTION_PARAM = "action";

        private static final String OBJECT_ID_PARAM = "object";

        /**
         * Returns access controll rules as XML
         */
        @Override
        public Source resolve(String href, String base) {
            String key = href.substring(href.indexOf(":") + 1);
            LOGGER.debug("Reading xml from query result using key :{}", key);

            String[] param;
            StringTokenizer tok = new StringTokenizer(key, "&");
            Hashtable<String, String> params = new Hashtable<>();

            while (tok.hasMoreTokens()) {
                param = tok.nextToken().split("=");
                params.put(param[0], param[1]);
            }

            String action = params.get(ACTION_PARAM);
            String objId = params.get(OBJECT_ID_PARAM);

            if (action == null || objId == null) {
                return null;
            }

            Element container = new Element("servacls").setAttribute("class", "MCRMetaAccessRule");
            if (MCRAccessManager.implementsRulesInterface()) {
                if (action.equals("all")) {
                    for (String permission : MCRAccessManager.getPermissionsForID(objId)) {
                        // one pool Element under access per defined AccessRule in pool for (Object-)ID
                        addRule(container, permission,
                            MCRAccessManager.requireRulesInterface().getRule(objId, permission));
                    }
                } else {
                    addRule(container, action, MCRAccessManager.requireRulesInterface().getRule(objId, action));
                }
            }
            return new JDOMSource(container);
        }

        private void addRule(Element root, String pool, Element rule) {
            if (rule != null && pool != null) {
                Element poolElement = new Element("servacl").setAttribute("permission", pool);
                poolElement.addContent(rule);
                root.addContent(poolElement);
            }
        }

    }

    private static class MCRClassificationResolver implements URIResolver {

        private static final Pattern EDITORFORMAT_PATTERN = Pattern.compile("(\\[)([^\\]]*)(\\])");

        private static final String FORMAT_CONFIG_PREFIX = CONFIG_PREFIX + "Classification.Format.";

        private static final String SORT_CONFIG_PREFIX = CONFIG_PREFIX + "Classification.Sort.";

        private static MCRCache<String, Element> categoryCache;

        private static MCRCategoryDAO DAO;

        static {
            try {
                DAO = MCRCategoryDAOFactory.getInstance();
                categoryCache = new MCRCache<>(
                    MCRConfiguration2.getInt(CONFIG_PREFIX + "Classification.CacheSize").orElse(1000),
                    "URIResolver categories");
            } catch (Exception exc) {
                LOGGER.error("Unable to initialize classification resolver", exc);
            }
        }

        MCRClassificationResolver() {
        }

        private static String getLabelFormat(String editorString) {
            Matcher m = EDITORFORMAT_PATTERN.matcher(editorString);
            if (m.find() && m.groupCount() == 3) {
                String formatDef = m.group(2);
                return MCRConfiguration2.getStringOrThrow(FORMAT_CONFIG_PREFIX + formatDef);
            }
            return null;
        }

        private static boolean shouldSortCategories(String classId) {
            return MCRConfiguration2.getBoolean(SORT_CONFIG_PREFIX + classId).orElse(true);
        }

        private static long getSystemLastModified() {
            long xmlLastModified = MCRXMLMetadataManager.instance().getLastModified();
            long classLastModified = DAO.getLastModified();
            return Math.max(xmlLastModified, classLastModified);
        }

        /**
         * returns a classification in a specific format. Syntax:
         * <code>classification:{editor[Complete]['['formatAlias']']|metadata}:{Levels}[:noEmptyLeaves]:{parents|
         * children}:{ClassID}[:CategID] formatAlias: MCRConfiguration property
         * MCR.UURResolver.Classification.Format.FormatAlias
         *
         * @param href
         *            URI in the syntax above
         * @return the root element of the XML document
         * @see MCRCategoryTransformer
         */
        @Override
        public Source resolve(String href, String base) {
            LOGGER.debug("start resolving {}", href);
            String cacheKey = getCacheKey(href);
            Element returns = categoryCache.getIfUpToDate(cacheKey, getSystemLastModified());
            if (returns == null) {
                returns = getClassElement(href);
                if (returns != null) {
                    categoryCache.put(cacheKey, returns);
                }
            }
            return new JDOMSource(returns);
        }

        protected String getCacheKey(String uri) {
            return uri;
        }

        private Element getClassElement(String uri) {
            StringTokenizer pst = new StringTokenizer(uri, ":", true);
            if (pst.countTokens() < 9) {
                // sanity check
                throw new IllegalArgumentException("Invalid format of uri for retrieval of classification: " + uri);
            }

            pst.nextToken(); // "classification"
            pst.nextToken(); // :
            String format = pst.nextToken();
            pst.nextToken(); // :

            String levelS = pst.nextToken();
            pst.nextToken(); // :
            int levels = Objects.equals(levelS, "all") ? -1 : Integer.parseInt(levelS);

            String axis;
            String token = pst.nextToken();
            pst.nextToken(); // :
            boolean emptyLeaves = !Objects.equals(token, "noEmptyLeaves");
            if (!emptyLeaves) {
                axis = pst.nextToken();
                pst.nextToken(); // :
            } else {
                axis = token;
            }

            String classID = pst.nextToken();
            StringBuilder categID = new StringBuilder();
            if (pst.hasMoreTokens()) {
                pst.nextToken(); // :
                while (pst.hasMoreTokens()) {
                    categID.append(pst.nextToken());
                }
            }

            String categ;
            try {
                categ = MCRXMLFunctions.decodeURIPath(categID.toString());
            } catch (URISyntaxException e) {
                categ = categID.toString();
            }
            MCRCategory cl = getMcrCategory(uri, axis, categ, classID, levels);
            if (cl == null) {
                return null;
            }
            return getElement(uri, format, classID, cl, emptyLeaves);
        }

        private static MCRCategory getMcrCategory(String uri, String axis, String categ, String classID, int levels) {
            MCRCategory cl = null;
            LOGGER.debug("categoryCache entry invalid or not found: start MCRClassificationQuery");
            if (axis.equals("children")) {
                if (categ.length() > 0) {
                    cl = DAO.getCategory(new MCRCategoryID(classID, categ), levels);
                } else {
                    cl = DAO.getCategory(MCRCategoryID.rootID(classID), levels);
                }
            } else if (axis.equals("parents")) {
                if (categ.length() == 0) {
                    LOGGER.error("Cannot resolve parent axis without a CategID. URI: {}", uri);
                    throw new IllegalArgumentException(
                        "Invalid format (categID is required in mode 'parents') "
                            + "of uri for retrieval of classification: "
                            + uri);
                }
                cl = DAO.getRootCategory(new MCRCategoryID(classID, categ), levels);
            }
            if (cl == null) {
                return null;
            }
            return cl;
        }

        private static Element getElement(String uri, String format, String classID, MCRCategory cl,
            boolean emptyLeaves) {
            Element returns;
            LOGGER.debug("start transformation of ClassificationQuery");
            if (format.startsWith("editor")) {
                boolean completeId = format.startsWith("editorComplete");
                boolean sort = shouldSortCategories(classID);
                String labelFormat = getLabelFormat(format);
                if (labelFormat == null) {
                    returns = MCRCategoryTransformer.getEditorItems(cl, sort, emptyLeaves, completeId);
                } else {
                    returns = MCRCategoryTransformer.getEditorItems(cl, labelFormat, sort, emptyLeaves, completeId);
                }
            } else if (format.equals("metadata")) {
                returns = MCRCategoryTransformer.getMetaDataDocument(cl, false).getRootElement().detach();
            } else {
                LOGGER.error("Unknown target format given. URI: {}", uri);
                throw new IllegalArgumentException(
                    "Invalid target format (" + format + ") in uri for retrieval of classification: " + uri);
            }
            LOGGER.debug("end resolving {}", uri);
            return returns;
        }

    }

    private static class MCRExceptionAsXMLResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) {
            String target = href.substring(href.indexOf(":") + 1);

            try {
                return instance().resolve(target, base);
            } catch (Exception ex) {
                LOGGER.debug("Caught {}. Put it into XML to process in XSL!", ex.getClass().getName());
                Element exception = new Element("exception");
                Element message = new Element("message");
                Element stacktraceElement = new Element("stacktrace");

                stacktraceElement.setAttribute("space", "preserve", Namespace.XML_NAMESPACE);

                exception.addContent(message);
                exception.addContent(stacktraceElement);

                message.setText(ex.getMessage());

                try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
                    ex.printStackTrace(pw);
                    stacktraceElement.setText(pw.toString());
                } catch (IOException e) {
                    throw new MCRException("Error while writing Exception to String!", e);
                }

                return new JDOMSource(exception);
            }
        }
    }

    /**
     * Ensures that the return of the given uri is never null. When the return is null, or the uri throws an exception,
     * this resolver will return an empty XML element instead. Usage: notnull:<anyMyCoReURI>
     */
    private static class MCRNotNullResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) {
            String target = href.substring(href.indexOf(":") + 1);
            // fixes exceptions if suburi is empty like "mcrobject:"
            String subUri = target.substring(target.indexOf(":") + 1);
            if (subUri.length() == 0) {
                return new JDOMSource(new Element("null"));
            }
            // end fix
            LOGGER.debug("Ensuring xml is not null: {}", target);
            try {
                Source result = instance().resolve(target, base);
                if (result != null) {
                    // perform actual construction of xml document, as in MCRURIResolver#resolve(String),
                    // by performing the same actions as MCRSourceContent#asXml(),
                    // but with a MCRXMLParser configured to be silent to suppress undesirable log messages
                    MCRContent content = new MCRSourceContent(result).getBaseContent();
                    Document document = MCRXMLParserFactory.getParser(false, true).parseXML(content);
                    return new JDOMSource(document.getRootElement().detach());
                } else {
                    LOGGER.debug("MCRNotNullResolver returning empty xml");
                    return new JDOMSource(new Element("null"));
                }
            } catch (Exception ex) {
                LOGGER.info("MCRNotNullResolver caught exception: {}", ex.getLocalizedMessage());
                LOGGER.debug(ex.getStackTrace());
                LOGGER.debug("MCRNotNullResolver returning empty xml");
                return new JDOMSource(new Element("null"));
            }
        }
    }

    /**
     * Transform result of other resolver with stylesheet. Usage: xslStyle:<stylesheet><,stylesheet><?param1=value1
     * <&param2=value2>><#flavor>:<anyMyCoReURI> To <stylesheet> is extension .xsl added.
     * File is searched in classpath.
     */
    private static class MCRXslStyleResolver implements URIResolver {

        public static final String PREFIX = "MCR.URIResolver.XSLStyle.Flavor.";

        private final Flavor defaultFlavor;

        private final Map<String, Flavor> flavors;

        private MCRXslStyleResolver() {

            Class<? extends TransformerFactory> defaultFactoryClass = MCRConfiguration2
                .<TransformerFactory>getClass("MCR.LayoutService.TransformerFactoryClass")
                .orElseGet(TransformerFactory.newInstance()::getClass);
            String defaultXslFolder = MCRConfiguration2
                .getStringOrThrow("MCR.Layout.Transformer.Factory.XSLFolder");

            defaultFlavor = new Flavor(defaultFactoryClass, defaultXslFolder);
            LOGGER.info("Working with default flavor {}", defaultFlavor);

            flavors = new HashMap<>();
            for (String flavorName : getFlavorNames()) {

                String factoryClassProperty = PREFIX + flavorName + ".TransformerFactoryClass";
                Class<? extends TransformerFactory> factoryClass = MCRConfiguration2
                    .<TransformerFactory>getClass(factoryClassProperty)
                    .orElseThrow(() -> MCRConfiguration2.createConfigurationException(factoryClassProperty));

                String xslFolderProperty = PREFIX + flavorName + ".XSLFolder";
                String xslFolder = MCRConfiguration2.getStringOrThrow(xslFolderProperty);

                Flavor flavor = new Flavor(factoryClass, xslFolder);
                LOGGER.info("Working with {} flavor {}", flavorName, flavor);
                flavors.put(flavorName, flavor);

            }

        }

        private static Set<String> getFlavorNames() {
            return MCRConfiguration2
                .getSubPropertiesMap(PREFIX)
                .keySet()
                .stream()
                .map(key -> key.substring(0, key.indexOf('.')))
                .collect(Collectors.toSet());
        }

        @Override
        public Source resolve(String href, String base) throws TransformerException {

            String help = href.substring(href.indexOf(":") + 1);

            // check if target URI is present
            int configurationEnd = help.indexOf(':');
            if (configurationEnd == -1) {
                throw new MCRUsageException("Target URI missing in " + href);
            }

            //  copy target URI from end of href, ensure that resolved element won't be null
            int targetStart = configurationEnd + 1;
            String targetUri = help.substring(targetStart);
            if (!targetUri.startsWith("notnull:")) {
                targetUri = "notnull:" + targetUri;
            }

            //  copy flavor from end of href, if present
            String flavorName = "";
            Flavor flavor = defaultFlavor;
            int flavorNameStart = help.lastIndexOf('#', configurationEnd);
            if (flavorNameStart != -1) {
                flavorName = help.substring(flavorNameStart + 1, configurationEnd);
                configurationEnd = flavorNameStart;
            }

            if (!flavorName.isEmpty()) {
                flavor = flavors.get(flavorName);
                if (flavor == null) {
                    throw new MCRUsageException("Unknown flavor " + flavorName + " in " + href);
                }
            }

            //  copy parameters from end of href, if present
            String parameters = "";
            int paramsStart = help.lastIndexOf('?', configurationEnd);
            if (paramsStart != -1) {
                parameters = help.substring(paramsStart + 1, configurationEnd);
                configurationEnd = paramsStart;
            }

            //  copy stylesheets from href
            String stylesheetPaths = help.substring(0, configurationEnd);

            // resolve target URI
            Source resolved = instance().resolve(targetUri, base);
            assert resolved != null;

            try {

                if (resolved.getSystemId() == null) {
                    resolved.setSystemId(targetUri);
                }

                // prepare transformer
                String[] stylesheets = augmentStylesheetsPaths(stylesheetPaths.split(","), flavor.xslFolder);
                MCRXSLTransformer transformer = MCRXSLTransformer.getInstance(flavor.transformerFactory, stylesheets);

                //prepare parameter collector
                MCRParameterCollector parameterCollector = MCRParameterCollector.getInstanceFromUserSession();
                parameterCollector.setParameters(getParameterMap(parameters));

                // perform transformation
                MCRSourceContent content = new MCRSourceContent(resolved);
                return transformer.transform(content, parameterCollector).getSource();

            } catch (IOException e) {
                findAndThrowTransformerException(e);
                throw new TransformerException(e);
            }

        }

        private String[] augmentStylesheetsPaths(String[] stylesheets, String xslFolder) {
            for (int i = 0; i < stylesheets.length; i++) {
                stylesheets[i] = xslFolder + "/" + stylesheets[i] + ".xsl";
            }
            return stylesheets;
        }

        private record Flavor(Class<? extends TransformerFactory> transformerFactory, String xslFolder) {
        }
    }

    /**
     * Transform result of other resolver with stylesheet. Usage: xslTransform:<transformer><?param1=value1
     * <&param2=value2>>:<anyMyCoReURI>
     */
    static class MCRLayoutTransformerResolver implements URIResolver {

        private static final String TRANSFORMER_FACTORY_PROPERTY = "MCR.Layout.Transformer.Factory";

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            String help = href.substring(href.indexOf(":") + 1);
            String transformerId = new StringTokenizer(help, ":").nextToken();
            String target = help.substring(help.indexOf(":") + 1);

            String subUri = target.substring(target.indexOf(":") + 1);
            if (subUri.length() == 0) {
                return new JDOMSource(new Element("null"));
            }

            Map<String, String> params;
            StringTokenizer tok = new StringTokenizer(transformerId, "?");
            transformerId = tok.nextToken();

            if (tok.hasMoreTokens()) {
                params = getParameterMap(tok.nextToken());
            } else {
                params = Collections.emptyMap();
            }
            Source resolved = instance().resolve(target, base);

            try {
                if (resolved != null) {
                    MCRSourceContent content = new MCRSourceContent(resolved);
                    MCRLayoutTransformerFactory factory = MCRConfiguration2.getInstanceOfOrThrow(
                        MCRLayoutTransformerFactory.class, TRANSFORMER_FACTORY_PROPERTY);
                    MCRContentTransformer transformer = factory.getTransformer(transformerId);
                    MCRContent result;
                    if (transformer instanceof MCRParameterizedTransformer parameterizedTransformer) {
                        MCRParameterCollector paramcollector = MCRParameterCollector.getInstanceFromUserSession();
                        paramcollector.setParameters(params);
                        result = parameterizedTransformer.transform(content, paramcollector);
                    } else {
                        result = transformer.transform(content);
                    }
                    return result.getSource();
                } else {
                    LOGGER.debug("MCRLayoutStyleResolver returning empty xml");
                    return new JDOMSource(new Element("null"));
                }
            } catch (Exception e) {
                findAndThrowTransformerException(e);
                throw new TransformerException(e);
            }
        }

    }

    /**
     * <p>
     * Includes xsl files which are set in the mycore.properties file.
     * </p>
     * Example: MCR.URIResolver.xslIncludes.components=iview.xsl,wcms.xsl
     * <p>
     * Or retrieve the include hrefs from a class implementing
     * {@link org.mycore.common.xml.MCRURIResolver.MCRXslIncludeHrefs}. The class. part have to be set, everything after
     * class. can be freely choosen.
     * </p>
     * Example: MCR.URIResolver.xslIncludes.class.template=org.foo.XSLHrefs
     *
     * @return A xsl file with the includes as href.
     */
    private static class MCRXslIncludeResolver implements URIResolver {
        private static final Logger LOGGER = LogManager.getLogger(MCRXslIncludeResolver.class);

        @Override
        public Source resolve(String href, String base) {
            String includePart = href.substring(href.indexOf(":") + 1);
            Namespace xslNamespace = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");

            Element root = new Element("stylesheet", xslNamespace);
            root.setAttribute("version", "1.0");

            // get the parameters from mycore.properties
            String propertyName = "MCR.URIResolver.xslIncludes." + includePart;
            List<String> propValue;
            if (includePart.startsWith("class.")) {
                MCRXslIncludeHrefs incHrefClass = MCRConfiguration2
                    .getOrThrow(propertyName, MCRConfiguration2::instantiateClass);
                propValue = incHrefClass.getHrefs();
            } else {
                propValue = MCRConfiguration2.getString(propertyName)
                    .map(MCRConfiguration2::splitValue)
                    .map(s -> s.collect(Collectors.toList()))
                    .orElseGet(Collections::emptyList);
            }

            final String xslFolder = MCRConfiguration2.getStringOrThrow("MCR.Layout.Transformer.Factory.XSLFolder");
            for (String include : propValue) {
                // create a new include element
                Element includeElement = new Element("include", xslNamespace);
                includeElement.setAttribute("href",
                    include.contains(":") ? include : "resource:" + xslFolder + "/" + include);
                root.addContent(includeElement);
                LOGGER.debug("Resolved XSL include: {}", include);
            }
            return new JDOMSource(root);
        }
    }

    /**
     * Imports xsl files which are set in the mycore.properties file. Example:
     * MCR.URIResolver.xslImports.components=first.xsl,second.xsl Every file must import this URIResolver to form a
     * import chain:
     *
     * <pre>
     *  &lt;xsl:import href="xslImport:components:first.xsl"&gt;
     * </pre>
     *
     * @return A xsl file with the import as href.
     */
    private static class MCRXslImportResolver implements URIResolver {

        URIResolver fallback = new MCRResourceResolver();

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            final String baseURI = getParentDirectoryResourceURI(base);
            // set xslt folder
            final String xslFolder;
            if (StringUtils.startsWith(baseURI, "resource:xsl/")) {
                xslFolder = "xsl";
            } else if (StringUtils.startsWith(baseURI, "resource:xslt/")) {
                xslFolder = "xslt";
            } else {
                xslFolder = MCRConfiguration2.getStringOrThrow("MCR.Layout.Transformer.Factory.XSLFolder");
            }

            String importXSL = MCRXMLFunctions.nextImportStep(href.substring(href.indexOf(':') + 1));
            if (importXSL.isEmpty()) {
                LOGGER.debug("End of import queue: {}", href);
                Namespace xslNamespace = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
                Element root = new Element("stylesheet", xslNamespace);
                root.setAttribute("version", "1.0");
                return new JDOMSource(root);
            }
            LOGGER.debug("xslImport importing {}", importXSL);

            return fallback.resolve("resource:" + xslFolder + "/" + importXSL, base);
        }
    }

    /**
     * Builds XML trees from a string representation. Multiple XPath expressions can be separated by &amp; Example:
     * buildxml:_rootName_=mycoreobject&metadata/parents/parent/@href= 'FooBar_Document_4711' This will return:
     * &lt;mycoreobject&gt; &lt;metadata&gt; &lt;parents&gt; &lt;parent href="FooBar_Document_4711" /&gt;
     * &lt;/parents&gt; &lt;/metadata&gt; &lt;/mycoreobject&gt;
     */
    private static class MCRBuildXMLResolver implements URIResolver {

        private static Hashtable<String, String> getParameterMap(String key) {
            String[] param;
            StringTokenizer tok = new StringTokenizer(key, "&");
            Hashtable<String, String> params = new Hashtable<>();

            while (tok.hasMoreTokens()) {
                param = tok.nextToken().split("=");
                params.put(URLDecoder.decode(param[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(param[1], StandardCharsets.UTF_8));
            }
            return params;
        }

        private static void constructElement(Element current, String xpath, String value) {
            StringTokenizer st = new StringTokenizer(xpath, "/");
            Element currentToken = current;
            String name = null;
            while (st.hasMoreTokens()) {
                name = st.nextToken();
                if (name.startsWith("@")) {
                    break;
                }
                String localName = getLocalName(name);
                Namespace namespace = getNamespace(name);

                Element child = currentToken.getChild(localName, namespace);
                if (child == null) {
                    child = new Element(localName, namespace);
                    currentToken.addContent(child);
                }
                currentToken = child;
            }
            if (name.startsWith("@")) {
                name = name.substring(1);
                String localName = getLocalName(name);
                Namespace namespace = getNamespace(name);
                currentToken.setAttribute(localName, value, namespace);
            } else {
                currentToken.setText(value);
            }
        }

        private static Namespace getNamespace(String name) {
            if (!name.contains(":")) {
                return Namespace.NO_NAMESPACE;
            }
            String prefix = name.split(":")[0];
            Namespace ns = MCRConstants.getStandardNamespace(prefix);
            return ns == null ? Namespace.NO_NAMESPACE : ns;
        }

        private static String getLocalName(String name) {
            if (!name.contains(":")) {
                return name;
            } else {
                return name.split(":")[1];
            }
        }

        /**
         * Builds a simple xml node tree on basis of name value pair
         */
        @Override
        public Source resolve(String href, String base) {
            String key = href.substring(href.indexOf(":") + 1);
            LOGGER.debug("Building xml from {}", key);

            Hashtable<String, String> params = getParameterMap(key);

            Element defaultRoot = new Element("root");
            Element root = defaultRoot;
            String rootName = params.get("_rootName_");
            if (rootName != null) {
                root = new Element(getLocalName(rootName), getNamespace(rootName));
                params.remove("_rootName_");
            }

            for (Map.Entry<String, String> entry : params.entrySet()) {
                constructElement(root, entry.getKey(), entry.getValue());
            }
            if (root.equals(defaultRoot) && root.getChildren().size() > 1) {
                LOGGER.warn("More than 1 root node defined, returning first");
                return new JDOMSource(root.getChildren().getFirst().detach());
            }
            return new JDOMSource(root);
        }

    }

    /**
     * Resolves the current user information. Example: currentUserInfo:attribute=eMail&attribute=realName&role=administrator <br>
     * Returns: <br>
     * <code>
     *     &lt;user id="admin"&gt; <br>
     *     &lt;attribute name="eMail"&gt;example@mycore.de&lt;/attribute&gt;<br>
     *     &lt;attribute name="realName"&gt;Administrator&lt;/attribute&gt;<br>
     *     &lt;role name="administrator"&gt;true&lt;/role&gt;<br>
     *     &lt;/user&gt;<br>
     * </code>
     */
    private static class MCRCurrentUserInfoResolver implements URIResolver {
        @Override
        public Source resolve(String href, String base) throws TransformerException {
            MCRUserInformation userInformation = MCRSessionMgr.getCurrentSession().getUserInformation();
            String userID = userInformation.getUserID();

            String[] split = href.split(":");

            Set<String> suppliedAttributes = new HashSet<>();
            Set<String> suppliedRoles = new HashSet<>();
            Element root = new Element("user");
            root.setAttribute("id", userID);

            if (split.length == 2) {
                String req = split[1];
                MCRURLQueryParameter.parse(req).forEach(nv -> {
                    if (nv.name().equals("attribute")) {
                        if (suppliedAttributes.contains(nv.value())) {
                            LOGGER.warn("Duplicate attribute {} in user info request", nv.value());
                            return;
                        }
                        suppliedAttributes.add(nv.value());
                        Element attribute = new Element("attribute");
                        attribute.setAttribute("name", nv.value());
                        attribute.setText(userInformation.getUserAttribute(nv.value()));
                        root.addContent(attribute);
                    } else if (nv.name().equals("role")) {
                        if (suppliedRoles.contains(nv.value())) {
                            LOGGER.warn("Duplicate role {} in user info request", nv.value());
                            return;
                        }
                        suppliedRoles.add(nv.value());
                        Element role = new Element("role");
                        role.setAttribute("name", nv.value());
                        role.setText(String.valueOf(userInformation.isUserInRole(nv.value())));
                        root.addContent(role);
                    }
                });
            }

            return new JDOMSource(root);
        }
    }

    /**
     * Resolves the software Version of the MyCoRe Instance. The following types are supported: gitDescribe, abbrev,
     * branch, version, revision, completeVersion. The default is completeVersion.
     * The resulting XML looks like this:
     * <code>
     *     &lt;version&gt;MyCoRe 2022.06.3-SNAPSHOT 2022.06.x:v2022.06.2-1-g881e24d&lt;/version&gt;
     * </code>
     */
    private static class MCRVersionResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            String versionType = href.substring(href.indexOf(":") + 1);
            final Element versionElement = new Element("version");
            versionElement.setText(
                switch (versionType) {
                    case "gitDescribe" -> MCRCoreVersion.getGitDescribe();
                    case "abbrev" -> MCRCoreVersion.getAbbrev();
                    case "branch" -> MCRCoreVersion.getBranch();
                    case "version" -> MCRCoreVersion.getVersion();
                    case "revision" -> MCRCoreVersion.getRevision();
                    default -> MCRCoreVersion.getCompleteVersion();
                });
            return new JDOMSource(versionElement);
        }
    }

    /**
     * Resolver for MCRLayoutUtils. The following types are supported: readAccess:$webpageID,
     * readAccess:$webpageID:split:$blockerWebpageID
     * returns
     * <code>
     *     &lt;true /&gt;
     * </code>
     * or
     * <code>
     *     &lt;false /&gt;
     * </code>
     */
    private static class MCRLayoutUtilsResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            String[] args = href.split(":", 3);

            if (args.length < 2) {
                throw new TransformerException("No arguments given");
            }
            String function = args[1];

            if (function.equals("readAccess")) {
                String[] params = args[2].split(":split:");
                if (params.length == 1) {
                    return new JDOMSource(new Element(String.valueOf(MCRLayoutUtilities.readAccess(params[0]))));
                } else if (params.length == 2) {
                    return new JDOMSource(
                        new Element(String.valueOf(MCRLayoutUtilities.readAccess(params[0], params[1]))));
                }
            } else if (function.equals("personalNavigation")) {
                try {
                    return new DOMSource(MCRLayoutUtilities.getPersonalNavigation());
                } catch (JDOMException | XPathExpressionException e) {
                    throw new MCRException("Error while loading personal navigation!", e);
                }
            }
            throw new TransformerException("Unknown argument: " + args[2]);
        }
    }

    private static class MCRVersionInfoResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            String id = href.substring(href.indexOf(":") + 1);
            LOGGER.debug("Reading version info of MCRObject with ID {}", id);
            MCRObjectID mcrId = MCRObjectID.getInstance(id);
            MCRXMLMetadataManager metadataManager = MCRXMLMetadataManager.instance();
            try {
                List<? extends MCRAbstractMetadataVersion<?>> versions = metadataManager.listRevisions(mcrId);
                if (versions != null && !versions.isEmpty()) {
                    return getSource(versions);
                } else {
                    return getSource(Instant.ofEpochMilli(metadataManager.getLastModified(mcrId))
                        .truncatedTo(ChronoUnit.MILLIS));
                }
            } catch (Exception e) {
                throw new TransformerException(e);
            }
        }

        private Source getSource(Instant lastModified) {
            Element e = new Element("versions");
            Element v = new Element("version");
            e.addContent(v);
            v.setAttribute("date", lastModified.toString());
            return new JDOMSource(e);
        }

        private Source getSource(List<? extends MCRAbstractMetadataVersion<?>> versions) {
            Element e = new Element("versions");
            for (MCRAbstractMetadataVersion<?> version : versions) {
                Element v = new Element("version");
                v.setAttribute("user", version.getUser());
                v.setAttribute("date", MCRXMLFunctions.getISODate(version.getDate(), null));
                v.setAttribute("r", version.getRevision());
                v.setAttribute("action", Character.toString(version.getType()));
                e.addContent(v);
            }
            return new JDOMSource(e);
        }
    }

    private static class MCRDeletedObjectResolver implements URIResolver {

        /**
         * Returns a deleted mcr object xml for the given id. If there is no such object a dummy object with an empty
         * metadata element is returned.
         *
         * @param href
         *            an uri starting with <code>deletedMcrObject:</code>
         * @param base
         *            may be null
         */
        @Override
        public Source resolve(String href, String base) throws TransformerException {
            String[] parts = href.split(":");
            MCRObjectID mcrId = MCRObjectID.getInstance(parts[parts.length - 1]);
            LOGGER.info("Resolving deleted object {}", mcrId);
            try {
                MCRContent lastPresentVersion = MCRXMLMetadataManager.instance().retrieveContent(mcrId);
                if (lastPresentVersion == null) {
                    LOGGER.warn("Could not resolve deleted object {}", mcrId);
                    return new JDOMSource(MCRObjectFactory.getSampleObject(mcrId));
                }
                return lastPresentVersion.getSource();
            } catch (IOException e) {
                throw new TransformerException(e);
            }
        }
    }

    private static class MCRFileMetadataResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) {
            String[] parts = href.split(":");
            String completePath = parts[1];
            String[] pathParts = completePath.split("/", 2);
            MCRObjectID derivateID = MCRObjectID.getInstance(pathParts[0]);
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
            MCRObjectDerivate objectDerivate = derivate.getDerivate();
            if (pathParts.length == 1) {
                //only derivate is given;
                Element fileset = new Element("fileset");
                if (objectDerivate.getURN() != null) {
                    fileset.setAttribute("urn", objectDerivate.getURN());
                    for (MCRFileMetadata fileMeta : objectDerivate.getFileMetadata()) {
                        fileset.addContent(fileMeta.createXML());
                    }
                }
                return new JDOMSource(fileset);
            }
            MCRFileMetadata fileMetadata = objectDerivate.getOrCreateFileMetadata("/" + pathParts[1]);
            return new JDOMSource(fileMetadata.createXML());
        }
    }

    /**
     * Redirect to different URIResolver that is defined via property. This resolver is meant to serve static content as
     * no variable substitution takes place Example: MCR.URIResolver.redirect.alias=webapp:path/to/alias.xml
     */
    private static class MCRRedirectResolver implements URIResolver {
        private static final Logger LOGGER = LogManager.getLogger(MCRRedirectResolver.class);

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            String configsuffix = href.substring(href.indexOf(":") + 1);

            // get the parameters from mycore.properties
            String propertyName = "MCR.URIResolver.redirect." + configsuffix;
            String propValue = MCRConfiguration2.getStringOrThrow(propertyName);
            LOGGER.info("Redirect {} to {}", href, propValue);
            return singleton.resolve(propValue, base);
        }
    }

    /**
     * Resolves an data url and returns the content.
     *
     * @see MCRDataURL
     */
    private static class MCRDataURLResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            try {
                final MCRDataURL dataURL = MCRDataURL.parse(href);

                final MCRByteContent content = new MCRByteContent(dataURL.getData());
                content.setSystemId(href);
                content.setMimeType(dataURL.getMimeType());
                content.setEncoding(dataURL.getCharset().name());

                return content.getSource();
            } catch (IOException e) {
                throw new TransformerException(e);
            }
        }

    }

    private static class MCRI18NResolver implements URIResolver {

        /**
         * Resolves the I18N String value for the given property.<br><br>
         * <br>
         * Syntax: <code>i18n:{i18n-code},{i18n-prefix}*,{i18n-prefix}*...</code> or <br>
         *         <code>i18n:{i18n-code}[:param1:param2:…paramN]</code>
         * <br>
         * Result: <code> <br>
         *     &lt;i18n&gt; <br>
         *   &lt;translation key=&quot;key1&quot;&gt;translation1&lt;/translation&gt; <br>
         *   &lt;translation key=&quot;key2&quot;&gt;translation2&lt;/translation&gt; <br>
         *   &lt;translation key=&quot;key3&quot;&gt;translation3&lt;/translation&gt; <br>
         * &lt;/i18n&gt; <br>
         * </code>
         * <br/>
         * If just one i18n-code is passed, then the translation element is skipped.
         * <code>
         *     &lt;i18n&gt; <br>translation&lt;/i18n&gt;<br>
         * </code>
         * Additionally, if the singular i18n-code is followed by a ":"-separated list of values,
         * the translation result is interpreted to be in Java MessageFormat and will be formatted with those values.
         * E.g.
         * <code>i18n:module.dptbase.common.results.nResults:15</code> (<code>{0} objects found</code>)
         *  -> "<code>15 objects found</code>"
         * @param href
         *            URI in the syntax above
         * @param base
         *            not used
         * @return the element with result format above
         * @see javax.xml.transform.URIResolver
         */
        @Override
        public Source resolve(String href, String base) {
            String target = href.substring(href.indexOf(":") + 1);

            final Element i18nElement = new Element("i18n");
            if (!target.contains("*") && !target.contains(",")) {
                String translation;
                if (target.contains(":")) {
                    final int i = target.indexOf(":");
                    translation = MCRTranslation.translate(target.substring(0, i),
                        (Object[]) target.substring(i + 1).split(":"));
                } else {
                    translation = MCRTranslation.translate(target);
                }
                i18nElement.addContent(translation);
                return new JDOMSource(i18nElement);
            }

            final String[] translationKeys = target.split(",");

            // Combine translations to prevent duplicates
            HashMap<String, String> translations = new HashMap<>();
            for (String translationKey : translationKeys) {
                if (translationKey.endsWith("*")) {
                    final String prefix = translationKey.substring(0, translationKey.length() - 1);
                    translations.putAll(MCRTranslation.translatePrefix(prefix));
                } else {
                    translations.put(translationKey,
                        MCRTranslation.translate(translationKey));
                }
            }

            translations.forEach((key, value) -> {
                final Element translation = new Element("translation");
                translation.setAttribute("key", key);
                translation.setText(value);
                i18nElement.addContent(translation);
            });

            return new JDOMSource(i18nElement);
        }
    }

    private static class MCRCheckPermissionChainResolver implements URIResolver {
        /**
         * Checks the permission and if granted resolve the uri
         * <p>
         * Syntax: <code>checkPermissionChain:{?id}:{permission}:{$uri}</code>
         *
         * @param href
         *            URI in the syntax above
         * @param base
         *            not used
         * @return if you have the permission then the resolved uri otherwise an Exception
         * @see javax.xml.transform.URIResolver
         */
        @Override
        public Source resolve(String href, String base) throws TransformerException {
            final String[] split = href.split(":", 4);

            if (split.length != 4) {
                throw new MCRException(
                    "Syntax needs to be checkPermissionChain:{?id}:{permission}:{uri} but was " + href);
            }

            final String permission = split[2];

            final String uri = split[3];
            final boolean hasAccess;

            if (!split[1].isBlank()) {
                hasAccess = MCRAccessManager.checkPermission(split[1], permission);
            } else {
                hasAccess = MCRAccessManager.checkPermission(permission);
            }

            if (!hasAccess) {
                throw new TransformerException("No Access to " + uri + " (" + href + " )");
            }

            return instance().resolve(uri, base);
        }
    }

    private static class MCRCheckPermissionResolver implements URIResolver {
        /**
         * returns the boolean value for the given ACL permission.
         * <p>
         * Syntax: <code>checkPermission:{id}:{permission}</code> or <code>checkPermission:{permission}</code>
         *
         * @param href
         *            URI in the syntax above
         * @param base
         *            not used
         * @return the root element "boolean" of the XML document with content string true of false
         * @see javax.xml.transform.URIResolver
         */
        @Override
        public Source resolve(String href, String base) {
            final String[] split = href.split(":");
            boolean permission = switch (split.length) {
                case 2 -> MCRAccessManager.checkPermission(split[1]);
                case 3 -> MCRAccessManager.checkPermission(split[1], split[2]);
                default -> throw new IllegalArgumentException(
                    "Invalid format of uri for retrieval of checkPermission: " + href);
            };
            Element root = new Element("boolean");
            root.setText(Boolean.toString(permission));
            return new JDOMSource(root);
        }
    }

    /**
     * @author Frank Lützenkirchen
     */
    private static class MCRCachingResolver implements URIResolver {

        private final static Logger LOGGER = LogManager.getLogger();

        private final static String CONFIG_PREFIX = "MCR.URIResolver.CachingResolver";

        private final long maxAge;

        private final MCRCache<String, Element> cache;

        MCRCachingResolver() {
            int capacity = MCRConfiguration2.getOrThrow(CONFIG_PREFIX + ".Capacity", Integer::parseInt);
            maxAge = MCRConfiguration2.getOrThrow(CONFIG_PREFIX + ".MaxAge", Long::parseLong);
            cache = new MCRCache<>(capacity, MCRCachingResolver.class.getName());
        }

        /**
         * Resolves XML content from a given URI and caches it for re-use.
         *
         * If the URI was already resolved within the last
         * MCR.URIResolver.CachingResolver.MaxAge milliseconds, the cached version is returned.
         * The default max age is one hour.
         *
         * The cache capacity is configured via MCR.URIResolver.CachingResolver.Capacity
         * The default capacity is 100.
         */
        @Override
        public Source resolve(String href, String base) {
            String hrefToCache = href.substring(href.indexOf(":") + 1);
            LOGGER.debug("resolving: " + hrefToCache);

            long maxDateCached = System.currentTimeMillis() - maxAge;
            Element resolvedXML = cache.getIfUpToDate(hrefToCache, maxDateCached);

            if (resolvedXML == null) {
                LOGGER.debug(hrefToCache + " not in cache, must resolve");
                resolvedXML = instance().resolve(hrefToCache);
                cache.put(hrefToCache, resolvedXML);
            } else {
                LOGGER.debug(hrefToCache + " already in cache");
            }

            return new JDOMSource(resolvedXML);
        }
    }

}
