/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.Enumeration;
import java.util.Objects;

import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRStreamUtils;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.function.MCRThrowFunction;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;
import org.xml.sax.ext.EntityResolver2;

/**
 * MCREntityResolver uses {@link CatalogResolver} for resolving entities or - for compatibility reasons - looks in
 * classpath to resolve XSD and DTD files.
 * 
 * @author Thomas Scheffler (yagee)
 * @since 2013.10
 */
public class MCREntityResolver implements EntityResolver2, LSResourceResolver {

    public static final Logger LOGGER = LogManager.getLogger(MCREntityResolver.class);

    private static final String CONFIG_PREFIX = "MCR.URIResolver.";

    CatalogResolver catalogResolver;

    private MCRCache<String, InputSourceProvider> bytesCache;

    private MCREntityResolver() {
        Enumeration<URL> systemResources;
        try {
            systemResources = MCRClassTools.getClassLoader().getResources("catalog.xml");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        URI[] catalogURIs = MCRStreamUtils.asStream(systemResources)
            .map(URL::toString)
            .peek(c -> LOGGER.info("Using XML catalog: {}", c))
            .map(URI::create)
            .toArray(URI[]::new);
        catalogResolver = CatalogManager.catalogResolver(CatalogFeatures.defaults(), catalogURIs);
        int cacheSize = MCRConfiguration2.getInt(CONFIG_PREFIX + "StaticFiles.CacheSize").orElse(100);
        bytesCache = new MCRCache<>(cacheSize, "EntityResolver Resources");
    }

    public static MCREntityResolver instance() {
        return MCREntityResolverHolder.instance;
    }

    private static boolean isAbsoluteURL(String url) {
        try {
            URL baseHttp = new URL("http://www.mycore.org");
            URL baseFile = new URL("file:///");
            URL relativeHttp = new URL(baseHttp, url);
            URL relativeFile = new URL(baseFile, url);
            return relativeFile.equals(relativeHttp);
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private InputSource resolveEntity(String publicId, String systemId,
        MCRThrowFunction<CatalogEntityIdentifier, InputSource, IOException> alternative) throws IOException {
        InputSource entity = null;
        try {
            entity = catalogResolver.resolveEntity(publicId, systemId);
            if (entity != null) {
                return resolvedEntity(entity);
            }
        } catch (CatalogException e) {
            LOGGER.debug(e.getMessage());
        }
        entity = alternative.apply(new CatalogEntityIdentifier(publicId, systemId));
        if (entity == null) {
            MCRURIResolver uriResolver = MCRURIResolver.instance();
            String scheme = uriResolver.getScheme(systemId, null);
            try {
                if (scheme != null && uriResolver.getResolver(scheme) != null) {
                    Source s = MCRURIResolver.instance().resolve(systemId, null, false);
                    return new MCRSourceContent(s).getInputSource();
                }
            } catch (MCRUsageException | TransformerException e) {
                return null;
            }
        }
        return entity;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
     */
    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws IOException {
        LOGGER.debug("Resolving: \npublicId: {}\nsystemId: {}", publicId, systemId);
        return resolveEntity(publicId, systemId, id -> resolveClassRessource(id.publicId, id.systemId));
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ext.EntityResolver2#getExternalSubset(java.lang.String, java.lang.String)
     */
    @Override
    public InputSource getExternalSubset(String name, String baseURI) {
        LOGGER.debug("External Subset: \nname: {}\nbaseURI: {}", name, baseURI);
        return null;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ext.EntityResolver2#resolveEntity(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId)
        throws IOException {
        LOGGER.debug("Resolving: \nname: {}\npublicId: {}\nbaseURI: {}\nsystemId: {}", name, publicId, baseURI,
            systemId);
        return resolveEntity(publicId, systemId, id -> resolveRelativeEntity(baseURI, id));
    }

    private InputSource resolveRelativeEntity(String baseURI, CatalogEntityIdentifier id)
        throws IOException {
        if (id.systemId == null) {
            return null; // Use default resolver
        }

        if (id.systemId.length() == 0) {
            // if you overwrite SYSTEM by empty String in XSL
            return new InputSource(new StringReader(""));
        }

        //resolve against base:
        URI absoluteSystemId = resolveRelativeURI(baseURI, id.systemId);
        if (absoluteSystemId.isAbsolute()) {
            if (uriExists(absoluteSystemId)) {
                InputSource inputSource = new InputSource(absoluteSystemId.toString());
                inputSource.setPublicId(id.publicId);
                return resolvedEntity(inputSource);
            }
            //resolve absolute URI against catalog first
            return resolveEntity(id.publicId, absoluteSystemId.toString(),
                id2 -> resolveClassRessource(id.publicId, id.systemId));
        }
        return resolveClassRessource(id.publicId, id.systemId);
    }

    private InputSource resolveClassRessource(String publicId, String systemId) throws IOException {
        if (MCRUtils.filterTrimmedNotEmpty(systemId).isEmpty()) {
            return null;
        }
        //required for XSD files that are usually classpath resources
        InputSource is = getCachedResource("/" + systemId);
        if (is == null) {
            return null;
        }
        is.setPublicId(publicId);
        return resolvedEntity(is);
    }

    private boolean uriExists(URI absoluteSystemId) {
        if (absoluteSystemId.getScheme().startsWith("http")) {
            return false; //default resolver handles http anyway
        }
        if (absoluteSystemId.getScheme().equals("jar")) {
            //multithread issues, when using ZIP filesystem with second check
            try {
                URL jarURL = absoluteSystemId.toURL();
                try (InputStream is = jarURL.openStream()) {
                    return is != null;
                }
            } catch (IOException e) {
                LOGGER.error("Error while checking (URL) URI: {}", absoluteSystemId, e);
            }
        }
        try {
            if (isFileSystemAvailable(absoluteSystemId.getScheme())) {
                Path pathTest = Paths.get(absoluteSystemId);
                LOGGER.debug("Checking: {}", pathTest);
                return Files.exists(pathTest);
            }
        } catch (Exception e) {
            LOGGER.error("Error while checking (Path) URI: {}", absoluteSystemId, e);
        }
        return false;
    }

    private boolean isFileSystemAvailable(String scheme) {
        return FileSystemProvider
            .installedProviders()
            .stream()
            .map(FileSystemProvider::getScheme)
            .anyMatch(Objects.requireNonNull(scheme)::equals);
    }

    private URI resolveRelativeURI(String baseURI, String systemId) {
        if (baseURI == null || isAbsoluteURL(systemId)) {
            return URI.create(systemId);
        }
        return URI.create(baseURI).resolve(systemId);
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        LOGGER.debug("Resolving resource: \ntype: {}\nnamespaceURI: {}\npublicId: {}\nsystemId: {}\nbaseURI: {}",
            type, namespaceURI, publicId, systemId, baseURI);
        return catalogResolver.resolveResource(type, namespaceURI, publicId, systemId, baseURI);
    }

    private InputSource resolvedEntity(InputSource entity) {
        String msg = "Resolved to: " + entity.getSystemId() + ".";
        LOGGER.debug(msg);
        return entity;
    }

    private InputSource getCachedResource(String classResource) throws IOException {
        URL resourceURL = this.getClass().getResource(classResource);
        if (resourceURL == null) {
            LOGGER.debug("{} not found", classResource);
            return null;
        }
        InputSourceProvider is = bytesCache.get(classResource);
        if (is == null) {
            LOGGER.debug("Resolving resource {}", classResource);
            final byte[] bytes;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream in = resourceURL.openStream()) {
                IOUtils.copy(in, baos);
                bytes = baos.toByteArray();
            }
            is = new InputSourceProvider(bytes, resourceURL);
            bytesCache.put(classResource, is);
        }
        return is.newInputSource();
    }

    private static class MCREntityResolverHolder {
        public static MCREntityResolver instance = new MCREntityResolver();
    }

    private static class InputSourceProvider {
        byte[] bytes;

        URL url;

        InputSourceProvider(byte[] bytes, URL url) {
            this.bytes = bytes;
            this.url = url;
        }

        public InputSource newInputSource() {
            InputSource is = new InputSource(url.toString());
            is.setByteStream(new ByteArrayInputStream(bytes));
            return is;
        }
    }

    private static class CatalogEntityIdentifier {
        private String publicId;

        private String systemId;

        private CatalogEntityIdentifier(String publicId, String systemId) {
            this.publicId = publicId;
            this.systemId = systemId;
        }
    }
}
