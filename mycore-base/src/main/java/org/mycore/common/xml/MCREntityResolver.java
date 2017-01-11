/*
 * $Id$
 * $Revision: 5697 $ $Date: Oct 9, 2013 $
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
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.util.XMLCatalogResolver;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.mycore.common.MCRCache;
import org.mycore.common.config.MCRConfiguration;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

/**
 * MCREntityResolver uses {@link XMLCatalogResolver} for resolving entities or - for compatibility reasons - looks in
 * classpath to resolve XSD and DTD files.
 * 
 * @author Thomas Scheffler (yagee)
 * @since 2013.10
 */
public class MCREntityResolver implements EntityResolver2, LSResourceResolver, XMLEntityResolver {

    public static final Logger LOGGER = LogManager.getLogger(MCREntityResolver.class);

    private static final String CONFIG_PREFIX = "MCR.URIResolver.";

    private MCRCache<String, InputSourceProvider> bytesCache;

    XMLCatalogResolver catalogResolver;

    private static class MCREntityResolverHolder {
        public static MCREntityResolver instance = new MCREntityResolver();
    }

    public static MCREntityResolver instance() {
        return MCREntityResolverHolder.instance;
    }

    private MCREntityResolver() {
        Enumeration<URL> systemResources;
        try {
            systemResources = this.getClass().getClassLoader().getResources("catalog.xml");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        Vector<String> catalogURIs = new Vector<>();
        while (systemResources.hasMoreElements()) {
            URL catalogURL = systemResources.nextElement();
            LOGGER.info("Using XML catalog: " + catalogURL);
            catalogURIs.add(catalogURL.toString());
        }
        String[] catalogs = catalogURIs.toArray(new String[catalogURIs.size()]);
        catalogResolver = new XMLCatalogResolver(catalogs);
        int cacheSize = MCRConfiguration.instance().getInt(CONFIG_PREFIX + "StaticFiles.CacheSize", 100);
        bytesCache = new MCRCache<String, InputSourceProvider>(cacheSize, "EntityResolver Resources");
    }

    /* (non-Javadoc)
     * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
     */
    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MessageFormat.format("Resolving: \npublicId: {0}\nsystemId: {1}", publicId, systemId));
        }
        InputSource entity = catalogResolver.resolveEntity(publicId, systemId);
        if (entity != null) {
            return resolvedEntity(entity);
        }
        return resolveEntity(null, publicId, null, systemId);
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ext.EntityResolver2#getExternalSubset(java.lang.String, java.lang.String)
     */
    @Override
    public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MessageFormat.format("External Subset: \nname: {0}\nbaseURI: {1}", name, baseURI));
        }
        InputSource externalSubset = catalogResolver.getExternalSubset(name, baseURI);
        if (externalSubset != null) {
            return resolvedEntity(externalSubset);
        }
        return resolveEntity(name, null, baseURI, null);
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ext.EntityResolver2#resolveEntity(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId)
        throws SAXException, IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MessageFormat.format("Resolving: \nname: {0}\npublicId: {1}\nbaseURI: {2}\nsystemId: {3}",
                name, publicId, baseURI, systemId));
        }
        InputSource entity = catalogResolver.resolveEntity(name, publicId, baseURI, systemId);
        if (entity != null) {
            return resolvedEntity(entity);
        }
        if (systemId == null) {
            return null; // Use default resolver
        }

        if (systemId.length() == 0) {
            // if you overwrite SYSTEM by empty String in XSL
            return new InputSource(new StringReader(""));
        }

        //resolve against base:
        URI absoluteSystemId = resolveRelativeURI(baseURI, systemId);
        if (absoluteSystemId.isAbsolute() && uriExists(absoluteSystemId)) {
            InputSource inputSource = new InputSource(absoluteSystemId.toString());
            inputSource.setPublicId(publicId);
            return resolvedEntity(inputSource);
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
                LOGGER.error("Error while checking (URL) URI: " + absoluteSystemId, e);
            }
        }
        try {
            if (isFileSystemAvailable(absoluteSystemId.getScheme())) {
                Path pathTest = Paths.get(absoluteSystemId);
                LOGGER.debug("Checking: " + pathTest);
                return Files.exists(pathTest);
            }
        } catch (Exception e) {
            LOGGER.error("Error while checking (Path) URI: " + absoluteSystemId, e);
        }
        return false;
    }

    private boolean isFileSystemAvailable(String scheme) {
        return FileSystemProvider
            .installedProviders()
            .stream()
            .map(FileSystemProvider::getScheme)
            .filter(Objects.requireNonNull(scheme)::equals)
            .findAny()
            .isPresent();
    }

    private URI resolveRelativeURI(String baseURI, String systemId) {
        if (baseURI == null || isAbsoluteURL(systemId)) {
            return URI.create(systemId);
        }
        URI resolved = URI.create(baseURI).resolve(systemId);
        return resolved;
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MessageFormat.format(
                "Resolving resource: \ntype: {0}\nnamespaceURI: {1}\npublicId: {2}\nsystemId: {3}\nbaseURI: {4}", type,
                namespaceURI, publicId, systemId, baseURI));
        }
        return catalogResolver.resolveResource(type, namespaceURI, publicId, systemId, baseURI);
    }

    private InputSource resolvedEntity(InputSource entity) {
        String msg = "Resolved  to: " + entity.getSystemId() + ".";
        LOGGER.info(msg);
        return entity;
    }

    private InputSource getCachedResource(String classResource) throws IOException {
        URL resourceURL = this.getClass().getResource(classResource);
        if (resourceURL == null) {
            LOGGER.debug(classResource + " not found");
            return null;
        }
        InputSourceProvider is = bytesCache.get(classResource);
        if (is == null) {
            LOGGER.debug("Resolving resource " + classResource);
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

    @Override
    public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier) throws XNIException, IOException {
        XMLInputSource entity = catalogResolver.resolveEntity(resourceIdentifier);
        if (entity == null) {
            LOGGER.info("Could not resolve entity: " + resourceIdentifier.getBaseSystemId());
            LOGGER.info("Identifer: " + catalogResolver.resolveIdentifier(resourceIdentifier));
            return null;
        }
        LOGGER.info("Resolve entity: " + resourceIdentifier.getBaseSystemId() + " --> " + entity.getBaseSystemId());
        return entity;
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

    private static class InputSourceProvider {
        byte[] bytes;

        URL url;

        public InputSourceProvider(byte[] bytes, URL url) {
            this.bytes = bytes;
            this.url = url;
        }

        public InputSource newInputSource() {
            InputSource is = new InputSource(url.toString());
            is.setByteStream(new ByteArrayInputStream(bytes));
            return is;
        }
    }
}
