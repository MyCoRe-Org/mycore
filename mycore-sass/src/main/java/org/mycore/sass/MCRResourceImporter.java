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

package org.mycore.sass;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRDeveloperTools;
import org.mycore.common.config.MCRConfigurationDir;

import com.google.protobuf.ByteString;

import de.larsgrefer.sass.embedded.importer.CustomImporter;
import de.larsgrefer.sass.embedded.importer.CustomUrlImporter;
import de.larsgrefer.sass.embedded.util.SyntaxUtil;
import jakarta.servlet.ServletContext;
import sass.embedded_protocol.EmbeddedSass.InboundMessage.ImportResponse.ImportSuccess;

/**
 * The MCRResourceImporter class is responsible for importing resources from URLs that start with the "sass:/" prefix.
 */
public class MCRResourceImporter extends CustomImporter {
    private static final Logger LOGGER = LogManager.getLogger();
    static final String SASS_URL_PREFIX = "sass:/";
    private final ServletContext servletContext;

    private static final UrlHelper URL_HELPER = new UrlHelper();

    /**
     * Initializes a new instance of the MCRResourceImporter class.
     *
     * @param servletContext The ServletContext used for importing resources.
     */
    public MCRResourceImporter(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Canonicalizes the given URL of the import and determines if it is from an `@import` rule.
     * <br>
     * This method takes a URL as input and checks if it starts with the "sass:/" prefix. If it does,
     * it removes the prefix and attempts to locate the resource URL using the getWebResourceUrl() method. If the
     * resource URL is found and is a file, it returns the original URL; otherwise, it returns null
     *
     * @param url The URL of the import to be canonicalized. This may be either absolute or relative.
     * @param fromImport Whether this request comes from an `@import` rule.
     * @return The canonicalized URL if it starts with "sass:/" and the resource exists, otherwise null.
     * @throws Exception If an error occurs during the canonicalization process.
     */
    @Override
    public String canonicalize(String url, boolean fromImport) throws Exception {
        LOGGER.debug(() -> "handle canonicalize: " + url);
        URL sassResourceURL = toURL(url);
        if (sassResourceURL == null || sassResourceURL.getFile().endsWith("/") || !URL_HELPER.isFile(sassResourceURL)) {
            //with jetty the directory does not end with /, so we need to REAL check
            return null;
        }
        LOGGER.debug("Resolved {} to {}", url, sassResourceURL);
        return sassResourceURL == null ? null : url;
    }

    /**
     * Handles the import of a URL and returns the success of the import along with its content.
     * <br>
     * This method takes a URL as input and checks if it starts with the "sass:/" prefix. If it does,
     * it attempts to locate the resource URL. If the resource URL is found,
     * it reads its content returns the an ImportSuccess object.
     *
     * @param url The URL of the resource to import.
     * @return An ImportSuccess object containing the content and syntax of the imported resource.
     * @throws IOException If there is an error in handling the import.
     */
    @Override
    public ImportSuccess handleImport(String url) throws IOException {
        LOGGER.debug(() -> "handle import: " + url);
        URL sassResourceURL = toURL(url);
        if (sassResourceURL == null) {
            return null;
        }
        LOGGER.debug(() -> "resource url: " + sassResourceURL);
        ImportSuccess.Builder result = ImportSuccess.newBuilder();
        URLConnection urlConnection = sassResourceURL.openConnection();
        try (InputStream in = urlConnection.getInputStream()) {
            ByteString content = ByteString.readFrom(in);
            result.setContentsBytes(content);
            result.setSyntax(SyntaxUtil.guessSyntax(urlConnection));
        }
        return result.build();
    }

    private URL toURL(String url) {
        if (!url.startsWith(SASS_URL_PREFIX)) {
            return null;
        }
        String resourcePath = url.substring(SASS_URL_PREFIX.length());
        URL configResource = getWebResourceUrl(resourcePath);
        return configResource;
    }

    /**
     * Retrieves the URL of a web resource given its path.
     *
     * @param resourcePath The path of the web resource to retrieve.
     * @return The URL of the web resource.
     */
    private URL getWebResourceUrl(String resourcePath) {
        return MCRDeveloperTools.getOverriddenFilePath(resourcePath, true)
            .map(p -> {
                try {
                    return p.toUri().toURL();
                } catch (MalformedURLException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .or(() -> Optional.ofNullable(MCRConfigurationDir.getConfigResource("META-INF/resources/" + resourcePath)))
            .orElseGet(() -> {
                try {
                    return servletContext.getResource("/" + resourcePath);
                } catch (MalformedURLException e) {
                    throw new UncheckedIOException(e);
                }
            });
    }

    /**
     * Helper class for working with URLs.
     *
     * Exposes {@link CustomUrlImporter#isFile(URL)} as a utility method for internal use.
     */
    private static class UrlHelper extends CustomUrlImporter {

        @Override
        public URL canonicalizeUrl(String url) throws Exception {
            return null;
        }

        @Override
        public boolean isFile(URL url) throws IOException {
            return super.isFile(url);
        }
    }
}
