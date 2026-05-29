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
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRFactory;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.xml.MCREntityResolver;
import org.mycore.common.xsl.MCRLazyStreamSource;
import org.mycore.common.xsl.MCRXSLResourceHelper;
import org.xml.sax.InputSource;

/**
 * Resolves URIs of various schemes by delegating to registered {@link URIResolver} implementations.
 * <p>
 * Additional URI schemes can be registered via {@link MCRURIResolverProvider} by setting the
 * {@code MCR.URIResolver.ExternalResolver.Class} property, or via module resolvers configured
 * under {@code MCR.URIResolver.ModuleResolver.<scheme>.Class}.
 *
 * @see MCRURIResolverProvider
 */
public class MCRURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIG_PREFIX = "MCR.URIResolver.";

    private static final Marker UNIQUE_MARKER = MarkerManager.getMarker("tryResolveXML");

    private Map<String, URIResolver> supportedSchemes;

    private MCRURIResolverProvider extResolver;

    public MCRURIResolver() {
        reinitialize();
    }

    /**
     * Reinitializes this resolver by reloading the external resolver provider and rebuilding
     * the scheme-to-{@link URIResolver} mapping. Can be called to pick up configuration changes
     * at runtime without creating a new instance.
     */
    public void reinitialize() {
        try {
            extResolver = getExternalResolverProvider();
            supportedSchemes = Collections.unmodifiableMap(getResolverMapping());
        } catch (Exception exc) {
            LOGGER.error("Unable to initialize MCRURIResolver", exc);
        }
    }

    private static MCRURIResolverProvider getExternalResolverProvider() {
        return MCRConfiguration2
            .getInstanceOf(MCRURIResolverProvider.class, CONFIG_PREFIX + "ExternalResolver")
            .orElse(HashMap::new);
    }

    /**
     * Returns the shared singleton instance of {@link MCRURIResolver}.
     *
     * @return the shared {@link MCRURIResolver} instance
     */
    @MCRFactory
    public static MCRURIResolver obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    /**
     * Creates and returns a new, independent {@link MCRURIResolver} instance.
     *
     * @return a new {@link MCRURIResolver}
     */
    public static MCRURIResolver createInstance() {
        return new MCRURIResolver();
    }

    /**
     * Resolves a relative or absolute {@code href} against an optional {@code base} URI.
     *
     * @param href the URI to resolve
     * @param base the base URI to resolve against, or {@code null} to use {@code href} as-is
     * @return the resolved {@link URI}
     */
    public static URI resolveURI(String href, String base) {
        return Optional.ofNullable(base)
            .map(URI::create)
            .map(u -> u.resolve(href))
            .orElse(URI.create(href));
    }

    private Map<String, URIResolver> getResolverMapping() {
        final Map<String, URIResolver> extResolverMapping = extResolver.getURIResolverMapping();
        extResolverMapping.putAll(new MCRURIModuleResolverProvider().getURIResolverMapping());
        // set Map to final size with loadfactor: full
        Map<String, URIResolver> supportedSchemes = new HashMap<>(10 + extResolverMapping.size(), 1);
        supportedSchemes.putAll(extResolverMapping);
        return supportedSchemes;
    }

    /**
     * Resolves a URI by delegating to the registered {@link URIResolver} for its scheme.
     * <p>URI syntax:
     * <pre>
     *   &lt;scheme&gt;:{target}
     * </pre>
     * <p>Example requests:
     * <pre>
     *   resource:xsl/myStylesheet.xsl
     *   mcrobject:mcr_document_00000001
     * </pre>
     * <p>If no scheme is present and the href ends with {@code .xsl}, resolution is attempted
     * via the {@code resource} scheme relative to the base URI.
     * <p>If the scheme has no registered resolver, resolution falls back to
     * {@link MCREntityResolver} and finally to a plain {@link StreamSource}.
     *
     * @param href the URI to resolve, optionally prefixed with a scheme
     * @param base the base URI of the calling stylesheet, used for relative URI resolution
     *             and scheme fallback
     * @return a {@link Source} for the resolved content, or {@code null} if the URI cannot
     *         be resolved
     * @throws TransformerException if a relative XSL href points outside the base URI
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
     * Reads an XML document from the given URI and returns its root element.
     * <p>Example:
     * <pre>
     *   Element root = resolver.resolve("resource:xml/myConfig.xml");
     * </pre>
     *
     * @param uri the URI to read the XML from
     * @return the detached root element of the resolved XML document, or {@code null} if
     *         the URI resolves to no content
     * @throws MCRException if the URI cannot be resolved or the content is not valid XML
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
     * Extracts the scheme (protocol) from a URI.
     * <p>If the given {@code uri} contains no {@code :}, the scheme is taken from {@code base}
     * instead.
     * <p>Example:
     * <pre>
     *   getScheme("mcrobject:mcr_document_00000001", null) // returns "mcrobject"
     *   getScheme("myFile.xsl", "resource:/xsl/")          // returns "resource"
     * </pre>
     *
     * @param uri  the URI whose scheme to extract
     * @param base fallback URI used when {@code uri} contains no scheme
     * @return the scheme portion before the first {@code :}, or {@code null} if neither
     *         {@code uri} nor {@code base} contains one
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
    public interface MCRURIResolverProvider {
        /**
         * provides a Map of URIResolver mappings. Key is the scheme, e.g. <code>http</code>, where value is an
         * implementation of {@link URIResolver}.
         *
         * @see URIResolver
         * @return a Map of URIResolver mappings
         */
        Map<String, URIResolver> getURIResolverMapping();
    }

    private static class MCRURIModuleResolverProvider implements MCRURIResolverProvider {
        private final Map<String, URIResolver> resolverMap = new HashMap<>();

        MCRURIModuleResolverProvider() {
            String prefix = CONFIG_PREFIX + "ModuleResolver.";
            MCRConfiguration2.getInstantiatablePropertyKeys(prefix)
                .forEach(property -> {
                    String scheme = property.substring(prefix.length());
                    registerUriResolver(scheme, property);
                });
        }

        @Override
        public Map<String, URIResolver> getURIResolverMapping() {
            return resolverMap;
        }

        private void registerUriResolver(String scheme, String propertyName) {
            try {
                resolverMap.put(scheme, MCRConfiguration2.getInstanceOfOrThrow(URIResolver.class, propertyName));
            } catch (MCRConfigurationException re) {
                throw new MCRException("Cannot instantiate " + propertyName + " for URI scheme " + scheme, re);
            }
        }

    }

    private static final class LazyInstanceHolder {
        public static final MCRURIResolver SHARED_INSTANCE = createInstance();
    }

}
