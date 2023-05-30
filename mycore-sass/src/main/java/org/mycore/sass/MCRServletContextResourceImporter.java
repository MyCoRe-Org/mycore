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

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRDeveloperTools;

import de.larsgrefer.sass.embedded.importer.Servlet5ContextImporter;
import jakarta.servlet.ServletContext;

/**
 * Imports scss files using {@link ServletContext}.
 */
public class MCRServletContextResourceImporter extends Servlet5ContextImporter {

    private static final Logger LOGGER = LogManager.getLogger();

    private final URI baseURL;

    private final Path webappPath;

    /**
     * Constructs this importer with required properties
     * @param servletContext to resolve web resources
     * @param baseURL like "foo/layout.scss", used to resolve relative path against
     * @see MCRSassCompilerManager#getRealFileName(String)
     */
    public MCRServletContextResourceImporter(ServletContext servletContext, String baseURL) {
        super(servletContext);
        this.baseURL = URI.create(baseURL);
        this.webappPath = Paths.get(servletContext.getRealPath("/"));
    }

    @Override
    public String canonicalize(String url, boolean fromImport) throws Exception {
        String modifiedUrl = getRelativePart(url);
        URI resolveURI;
        if (modifiedUrl.equals(url)) {
            resolveURI = modifiedUrl.equals(baseURL.toString()) ? baseURL : baseURL.resolve(modifiedUrl);
        } else {
            resolveURI = URI.create(modifiedUrl);
        }
        var filePath = MCRDeveloperTools.getOverriddenFilePath(resolveURI.toString(), true)
            .filter(Files::isRegularFile); //needs to be a file not a directory
        URL resource;
        if (filePath.isPresent()) {
            resource = filePath.get().toUri().toURL();
        } else {
            resource = super.canonicalizeUrl(resolveURI.toString());
            if (resource != null && resource.getFile().endsWith("/")) {
                //needs to be a file not a directory
                resource = null;
            }
        }
        LOGGER.debug("Resolved {} to {}", url, resource);
        return resource == null ? null : resource.toString();
    }

    private String getRelativePart(String modifiedURL) {
        if (modifiedURL.startsWith("jar:")) {
            final String webResourcePrefix = "!/META-INF/resources/";
            return modifiedURL.substring(modifiedURL.indexOf(webResourcePrefix) + webResourcePrefix.length());
        }
        if (modifiedURL.startsWith("file:")) {
            final Path localFile = Paths.get(URI.create(modifiedURL));
            return Stream
                .concat(MCRDeveloperTools.getOverridePaths().map(p -> p.resolve("META-INF").resolve("resources")),
                    Stream.of(webappPath))
                .filter(p -> localFile.startsWith(p))
                .findAny()
                .map(basePath -> basePath.relativize(localFile))
                .map(Path::toString)
                .orElseThrow();
        }
        if (!modifiedURL.contains(":")) {
            LOGGER.debug("Not absolute: {}", modifiedURL);
            return modifiedURL;
        }
        LOGGER.warn("How do we handle: {}?", modifiedURL);
        return modifiedURL;
    }

}
