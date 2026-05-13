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
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRFactory;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.xml.MCREntityResolver;
import org.mycore.common.xsl.MCRLazyStreamSource;
import org.mycore.common.xsl.MCRXSLResourceHelper;
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

    @MCRFactory
    public static MCRURIResolver obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    public static MCRURIResolver createInstance() {
        return new MCRURIResolver();
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
        String baseUri = MCRXSLResourceHelper.getXSLDirectory(base);

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

        Source oldResolveMethodResult =
            supportedSchemes.get("resource").resolve(MCRXSLResourceHelper.getXSLResourceURI(href), base);
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
