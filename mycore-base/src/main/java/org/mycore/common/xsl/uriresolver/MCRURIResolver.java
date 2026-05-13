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
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRFactory;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.xml.MCREntityResolver;
import org.mycore.common.xsl.MCRLazyStreamSource;
import org.mycore.resource.MCRResourceHelper;
import org.mycore.resource.MCRResourcePath;
import org.xml.sax.InputSource;

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
public class MCRURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIG_PREFIX = "MCR.URIResolver.";

    private static final Marker UNIQUE_MARKER = MarkerManager.getMarker("tryResolveXML");

    public static final String PROPERTY_XSL_FOLDER = "MCR.Layout.Transformer.Factory.XSLFolder";

    public static final String RESOURCE_PREFIX = "resource:";

    public static final String ELEMENT_NULL = "null";

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

    public static void findAndThrowTransformerException(Exception e) throws TransformerException {
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

    private Map<String, URIResolver> getResolverMapping() {
        final Map<String, URIResolver> extResolverMapping = extResolver.getURIResolverMapping();
        extResolverMapping.putAll(new MCRModuleResolverProvider().getURIResolverMapping());
        // set Map to final size with loadfactor: full
        Map<String, URIResolver> supportedSchemes = new HashMap<>(10 + extResolverMapping.size(), 1);
        supportedSchemes.putAll(extResolverMapping);
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

    private static final class LazyInstanceHolder {
        public static final MCRURIResolver SHARED_INSTANCE = createInstance();
    }

}
