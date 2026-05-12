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

package org.mycore.common.xsl.uriresolver;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRFactory;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.common.xml.MCREntityResolver;
import org.mycore.common.xml.MCRLayoutTransformerFactory;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.common.xsl.MCRLazyStreamSource;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.datamodel.metadata.MCRXMLConstants;
import org.mycore.resource.MCRResourceHelper;
import org.mycore.resource.MCRResourcePath;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

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

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIG_PREFIX = "MCR.URIResolver.";

    private static final Marker UNIQUE_MARKER = MarkerManager.getMarker("tryResolveXML");

    public static final String PROPERTY_XSL_FOLDER = "MCR.Layout.Transformer.Factory.XSLFolder";

    public static final String RESOURCE_PREFIX = "resource:";

    private static final String ELEMENT_NULL = "null";

    private Map<String, URIResolver> supportedSchemes;

    private MCRResolverProvider extResolver;

    public MCRURIResolver() {
        reinitialize();
    }

    public void reinitialize() {
        try {
            extResolver = getExternalResolverProvider();
            supportedSchemes = Collections.unmodifiableMap(getResolverMapping());
        } catch (Exception exc) {
            LOGGER.error("Unable to initialize MCRURIResolver", exc);
        }
    }

    private static MCRResolverProvider getExternalResolverProvider() {
        return MCRConfiguration2
            .getInstanceOf(MCRResolverProvider.class, CONFIG_PREFIX + "ExternalResolver.Class")
            .orElse(HashMap::new);
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

    @MCRFactory
    public static MCRURIResolver obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    public static MCRURIResolver createInstance() {
        return new MCRURIResolver();
    }

    public static Map<String, String> getParameterMap(String key) {
        String[] param;
        StringTokenizer tok = new StringTokenizer(key, "&");
        Map<String, String> params = new HashMap<>();

        while (tok.hasMoreTokens()) {
            param = tok.nextToken().split("=");
            params.put(URLDecoder.decode(param[0], StandardCharsets.UTF_8),
                param.length >= 2 ? URLDecoder.decode(param[1], StandardCharsets.UTF_8) : "");
        }
        return params;
    }

    /**
     * creates the default boolean response of a MyCoRe URIResolver.
     * This is an element with text body: &lt;boolean&gt;true|false&lt;boolean&gt;
     * @param value the boolean value that should be returned
     * @return a JDOMSource
     */
    public static Source createBooleanResponse(boolean value) {
        Element root = new Element("boolean");
        root.setText(Boolean.toString(value));
        return new JDOMSource(root);
    }

    /**
     * creates the default String response of a MyCoRe URIResolver.
     * This is an element with text body: &lt;string&gt;texte&lt;string&gt;
     * @param text the String text that should be returned
     * @return a JDOMSource
     */
    public static Source createStringResponse(String text) {
        Element root = new Element("string");
        root.setText(text);
        return new JDOMSource(root);
    }

    public static URI resolveURI(String href, String base) {
        return Optional.ofNullable(base)
            .map(URI::create)
            .map(u -> u.resolve(href))
            .orElse(URI.create(href));
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private Map<String, URIResolver> getResolverMapping() {
        final Map<String, URIResolver> extResolverMapping = extResolver.getURIResolverMapping();
        extResolverMapping.putAll(new MCRModuleResolverProvider().getURIResolverMapping());
        // set Map to final size with loadfactor: full
        Map<String, URIResolver> supportedSchemes = new HashMap<>(10 + extResolverMapping.size(), 1);
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
        supportedSchemes.put("checkDerivateDisplayEnabled", new MCRCheckDerivateDisplayEnabledResolver());
        MCRRESTResolver restResolver = new MCRRESTResolver();
        supportedSchemes.put("http", restResolver);
        supportedSchemes.put("https", restResolver);
        supportedSchemes.put("file", new MCRFileResolver());
        supportedSchemes.put("cache", new MCRCachingResolver());
        supportedSchemes.put("websiteWriteProtection", new MCRWebsiteWriteProtectionResolver());
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
            final String xslFolder = MCRConfiguration2.getStringOrThrow(PROPERTY_XSL_FOLDER);
            return RESOURCE_PREFIX + xslFolder + "/";
        } else {
            String resolvingBase = null;
            MCRResourcePath resourcePath = MCRResourceHelper.getResourcePath(base);
            if (resourcePath != null) {
                String path = resourcePath.asRelativePath();
                resolvingBase = RESOURCE_PREFIX + path.substring(0, path.lastIndexOf('/') + 1);
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

        URIResolver uriResolver = supportedSchemes.get(scheme);
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
                InputSource entity = MCREntityResolver.getInstance().resolveEntity(null, href);
                if (entity != null) {
                    LOGGER.debug("Resolved via EntityResolver: {}", entity::getSystemId);
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
        Source newResolveMethodResult = supportedSchemes.get("resource").resolve(finalUri, base);
        if (newResolveMethodResult != null) {
            return newResolveMethodResult;
        }

        // new relative include did not work, now fall back to old behaviour and print a warning if it works
        final String xslFolder = MCRConfiguration2.getStringOrThrow(PROPERTY_XSL_FOLDER);
        Source oldResolveMethodResult = supportedSchemes.get("resource")
            .resolve(RESOURCE_PREFIX + xslFolder + "/" + href, base);
        if (oldResolveMethodResult != null) {
            LOGGER.warn(UNIQUE_MARKER,
                () -> "The Stylesheet " + base + " has include " + href + " which only works with an old " +
                    "absolute include mechanism. Please change the include to relative!");
        }
        return oldResolveMethodResult;
    }

    private void addDebugInfo(String href, String base) {
        MCRURIResolverFilter.addUri(href + " from " + base);
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
            content = MCRSourceContent.createInstance(uri);
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
                resolverMap.put(scheme, MCRConfiguration2.instantiateClass(URIResolver.class, className));
            } catch (RuntimeException re) {
                throw new MCRException("Cannot instantiate " + className + " for URI scheme " + scheme, re);
            }
        }

    }

    /**
     * Reads XML from the CLASSPATH of the application. the location of the file in the format resource:path/to/file
     */
    private static final class MCRResourceResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            String path = href.substring(href.indexOf(':') + 1);
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
                    reader.setEntityResolver(MCREntityResolver.getInstance());
                    InputSource input = new InputSource(resource.toString());
                    SAXSource saxSource = new SAXSource(reader, input);
                    LOGGER.debug("include stylesheet: {}", saxSource::getSystemId);
                    return saxSource;
                } else {
                    return obtainInstance().resolve(resource.toString(), base);
                }
            }
            return null;
        }
    }

    private static final class MCRExceptionAsXMLResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) {
            String target = href.substring(href.indexOf(':') + 1);

            try {
                return obtainInstance().resolve(target, base);
            } catch (Exception ex) {
                LOGGER.debug("Caught {}. Put it into XML to process in XSL!", () -> ex.getClass().getName());
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
                    MCRException mcrException = new MCRException("Error while writing Exception to String!", e);
                    mcrException.addSuppressed(ex);
                    throw mcrException;
                }

                return new JDOMSource(exception);
            }
        }
    }

    /**
     * Ensures that the return of the given uri is never null. When the return is null, or the uri throws an exception,
     * this resolver will return an empty XML element instead. Usage: notnull:<anyMyCoReURI>
     */
    private static final class MCRNotNullResolver implements URIResolver {

        @Override
        public Source resolve(String href, String base) {
            String target = href.substring(href.indexOf(':') + 1);
            // fixes exceptions if suburi is empty like "mcrobject:"
            String subUri = target.substring(target.indexOf(':') + 1);
            if (subUri.isEmpty()) {
                return new JDOMSource(new Element(ELEMENT_NULL));
            }
            // end fix
            LOGGER.debug("Ensuring xml is not null: {}", target);
            try {
                Source result = obtainInstance().resolve(target, base);
                if (result != null) {
                    // perform actual construction of xml document, as in MCRURIResolver#resolve(String),
                    // by performing the same actions as MCRSourceContent#asXml(),
                    // but with a MCRXMLParser configured to be silent to suppress undesirable log messages
                    MCRContent content = new MCRSourceContent(result).getBaseContent();
                    Document document = MCRXMLParserFactory.getParser(false, true).parseXML(content);
                    return new JDOMSource(document.getRootElement().detach());
                } else {
                    LOGGER.debug("MCRNotNullResolver returning empty xml");
                    return new JDOMSource(new Element(ELEMENT_NULL));
                }
            } catch (Exception ex) {
                LOGGER.info("MCRNotNullResolver caught exception: {}", ex::getLocalizedMessage);
                LOGGER.debug(ex::getLocalizedMessage, ex);
                LOGGER.debug("MCRNotNullResolver returning empty xml");
                return new JDOMSource(new Element(ELEMENT_NULL));
            }
        }
    }

    /**
     * Transform result of other resolver with stylesheet. Usage: xslStyle:<stylesheet><,stylesheet><?param1=value1
     * <&param2=value2>><#flavor>:<anyMyCoReURI> To <stylesheet> is extension .xsl added.
     * File is searched in classpath.
     */
    private static final class MCRXslStyleResolver implements URIResolver {

        public static final String PREFIX = "MCR.URIResolver.XSLStyle.Flavor.";
        private static final String FLAVOR_PARAMETER = "xslStyleFlavor";

        private final Flavor defaultFlavor;

        private final Map<String, Flavor> flavors;

        private MCRXslStyleResolver() {

            Class<? extends TransformerFactory> defaultFactoryClass = MCRConfiguration2
                .<TransformerFactory>getClass("MCR.LayoutService.TransformerFactoryClass")
                .orElseGet(TransformerFactory.newInstance()::getClass);
            String defaultXslFolder = MCRConfiguration2
                .getStringOrThrow(PROPERTY_XSL_FOLDER);

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

            String help = href.substring(href.indexOf(':') + 1);

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

            Map<String, String> parameterMap = getParameterMap(parameters);
            String flavorParameter = parameterMap.remove(FLAVOR_PARAMETER);

            //  copy stylesheets from href
            String stylesheetPaths = help.substring(0, configurationEnd);

            // resolve target URI
            Source resolved = obtainInstance().resolve(targetUri, base);
            assert resolved != null;

            try {

                if (resolved.getSystemId() == null) {
                    resolved.setSystemId(targetUri);
                }

                if (flavorName.isEmpty() && flavorParameter != null && !flavorParameter.isBlank()) {
                    flavorName = flavorParameter;
                    flavor = flavors.get(flavorName);
                    if (flavor == null) {
                        throw new MCRUsageException("Unknown flavor " + flavorName + " in " + href);
                    }
                }

                // prepare transformer
                String[] stylesheets = augmentStylesheetsPaths(stylesheetPaths.split(","),
                    flavor.xslFolder);
                MCRXSLTransformer transformer = MCRXSLTransformer.obtainInstance(flavor.transformerFactory,
                    stylesheets);

                //prepare parameter collector
                MCRParameterCollector parameterCollector = MCRParameterCollector.ofCurrentSession();
                parameterCollector.setParameters(parameterMap);

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
            String help = href.substring(href.indexOf(':') + 1);
            String transformerId = new StringTokenizer(help, ":").nextToken();
            String target = help.substring(help.indexOf(':') + 1);

            String subUri = target.substring(target.indexOf(':') + 1);
            if (subUri.isEmpty()) {
                return new JDOMSource(new Element(ELEMENT_NULL));
            }

            Map<String, String> params;
            StringTokenizer tok = new StringTokenizer(transformerId, "?");
            transformerId = tok.nextToken();

            if (tok.hasMoreTokens()) {
                params = getParameterMap(tok.nextToken());
            } else {
                params = Collections.emptyMap();
            }
            Source resolved = obtainInstance().resolve(target, base);

            try {
                if (resolved != null) {
                    MCRSourceContent content = new MCRSourceContent(resolved);
                    MCRLayoutTransformerFactory factory = MCRConfiguration2.getInstanceOfOrThrow(
                        MCRLayoutTransformerFactory.class, TRANSFORMER_FACTORY_PROPERTY);
                    MCRContentTransformer transformer = factory.getTransformer(transformerId);
                    MCRContent result;
                    if (transformer instanceof MCRParameterizedTransformer parameterizedTransformer) {
                        MCRParameterCollector paramcollector = MCRParameterCollector.ofCurrentSession();
                        paramcollector.setParameters(params);
                        result = parameterizedTransformer.transform(content, paramcollector);
                    } else {
                        result = transformer.transform(content);
                    }
                    return result.getSource();
                } else {
                    LOGGER.debug("MCRLayoutStyleResolver returning empty xml");
                    return new JDOMSource(new Element(ELEMENT_NULL));
                }
            } catch (Exception e) {
                findAndThrowTransformerException(e);
                throw new TransformerException(e);
            }
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
     * <p>
     * Returns a xsl file with the import as href.
     */
    private static final class MCRXslImportResolver implements URIResolver {

        URIResolver fallback = new MCRResourceResolver();

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            final String baseURI = getParentDirectoryResourceURI(base);
            // set xslt folder
            final String xslFolder;
            if (Strings.CS.startsWith(baseURI, "resource:xsl/")) {
                xslFolder = "xsl";
            } else if (Strings.CS.startsWith(baseURI, "resource:xslt/")) {
                xslFolder = "xslt";
            } else {
                xslFolder = MCRConfiguration2.getStringOrThrow(PROPERTY_XSL_FOLDER);
            }

            String importXSL = MCRXMLFunctions.nextImportStep(href.substring(href.indexOf(':') + 1));
            if (importXSL.isEmpty()) {
                LOGGER.debug("End of import queue: {}", href);
                Namespace xslNamespace = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
                Element root = new Element("stylesheet", xslNamespace);
                root.setAttribute(MCRXMLConstants.VERSION, "1.0");
                return new JDOMSource(root);
            }
            LOGGER.debug("xslImport importing {}", importXSL);

            return fallback.resolve(RESOURCE_PREFIX + xslFolder + "/" + importXSL, base);
        }
    }

    /**
     * Redirect to different URIResolver that is defined via property. This resolver is meant to serve static content as
     * no variable substitution takes place Example: MCR.URIResolver.redirect.alias=webapp:path/to/alias.xml
     */
    private static final class MCRRedirectResolver implements URIResolver {

        private static final Logger LOGGER = LogManager.getLogger();

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            String configsuffix = href.substring(href.indexOf(':') + 1);

            // get the parameters from mycore.properties
            String propertyName = "MCR.URIResolver.redirect." + configsuffix;
            String propValue = MCRConfiguration2.getStringOrThrow(propertyName);
            LOGGER.info("Redirect {} to {}", href, propValue);
            return obtainInstance().resolve(propValue, base);
        }
    }

    private static final class MCRCheckPermissionChainResolver implements URIResolver {

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
         * @see URIResolver
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

            return obtainInstance().resolve(uri, base);
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
         * <p>
         * If the URI was already resolved within the last
         * MCR.URIResolver.CachingResolver.MaxAge milliseconds, the cached version is returned.
         * The default max age is one hour.
         * <p>
         * The cache capacity is configured via MCR.URIResolver.CachingResolver.Capacity
         * The default capacity is 100.
         */
        @Override
        public Source resolve(String href, String base) {
            String hrefToCache = href.substring(href.indexOf(':') + 1);
            LOGGER.debug(() -> "resolving: " + hrefToCache);

            long maxDateCached = System.currentTimeMillis() - maxAge;
            Element resolvedXML = cache.getIfUpToDate(hrefToCache, maxDateCached);

            if (resolvedXML == null) {
                LOGGER.debug(() -> hrefToCache + " not in cache, must resolve");
                resolvedXML = obtainInstance().resolve(hrefToCache);
                cache.put(hrefToCache, resolvedXML);
            } else {
                LOGGER.debug(() -> hrefToCache + " already in cache");
            }

            return new JDOMSource(resolvedXML);
        }
    }

    private static final class LazyInstanceHolder {
        public static final MCRURIResolver SHARED_INSTANCE = createInstance();
    }

}
