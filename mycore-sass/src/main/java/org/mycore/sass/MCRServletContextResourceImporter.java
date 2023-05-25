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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRDeveloperTools;

import de.larsgrefer.sass.embedded.importer.CanonicalizationHelper;
import de.larsgrefer.sass.embedded.importer.CustomUrlImporter;
import jakarta.servlet.ServletContext;

/**
 * Imports scss files using {@link ServletContext}.
 */
public class MCRServletContextResourceImporter extends CustomUrlImporter {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ServletContext context;
    private final Path webappPath;

    private final Deque<ResolvedUri> previousUris = new ArrayDeque<>();

    private long resolvingTime = 0;
    private int resolveCount = 0;

    /**
     * Initialize MCRServletContextResourceImporter
     * @param context - the servlet context
     */
    public MCRServletContextResourceImporter(ServletContext context) {
        this.context = context;
        this.webappPath = Paths.get(context.getRealPath("/"));

    }

    @Override
    public URL canonicalizeUrl(String url) {
        long start = System.currentTimeMillis();
        /*
         this mehod is called pre-order, so we can stack previous imports
         addToDeque is used to keep the previousUris size small.
         We assume that we do not need keep urls in it that were absulute URIs
         This will reduce the candidate size drastically
         */
        boolean addToDeque = true;
        try {
            String modifiedURL = url;
            Optional<URL> firstPossibleName;
            if (previousUris.isEmpty()) {
                //main sass file
                firstPossibleName = tryResolve(url);
            } else {
                //this is only a heuristical try, as we have no base URI to resolve against
                //it will work if `url` can only be resolved against ONE base URI of `previousUris` or
                //if the lowest matching import node is the right one
                modifiedURL = unResolve(modifiedURL); //strip prefix to web resource
                addToDeque = modifiedURL.equals(url); //not for absoluteUris
                ResolvedUri previous;
                do {
                    previous = previousUris.pollLast();
                    LOGGER.debug("\nurl:{}\nmodified:{}\nprevious:{}", url, modifiedURL, previous);
                    final String candidate = previous.modified.resolve(modifiedURL).toString();
                    firstPossibleName = tryResolve(candidate);
                    if (firstPossibleName.isPresent()) {
                        modifiedURL = candidate;
                    }
                } while (firstPossibleName.isEmpty() && !previousUris.isEmpty());
                //main sass up to the previous match should be kept for the next search
                previousUris.add(previous);
            }

            if (firstPossibleName.isEmpty()) {
                return null;
            }
            URL resource = firstPossibleName.get();

            LOGGER.debug("Resolved {} to {}", url, resource);
            if (addToDeque) {
                previousUris.add(new ResolvedUri(url, URI.create(modifiedURL), resource));
            }
            return resource;
        } finally {
            resolvingTime += System.currentTimeMillis() - start;
            resolveCount++;
            LOGGER.debug("Accumulated resolving time: {} ms, deque size: {}, i: {}", resolvingTime, previousUris.size(),
                resolveCount);
        }
    }

    private String unResolve(String modifiedURL) {
        //unresolved urls must return String starting with "/"
        if (modifiedURL.startsWith("jar:")) {
            final String webResourcePrefix = "!/META-INF/resources/";
            return modifiedURL.substring(modifiedURL.indexOf(webResourcePrefix) + webResourcePrefix.length() - 1);
        }
        if (modifiedURL.startsWith("file:")) {
            final Path localFile = Paths.get(URI.create(modifiedURL));
            return Stream.concat(MCRDeveloperTools.getOverridePaths(), Stream.of(webappPath))
                .filter(p -> localFile.startsWith(p))
                .findAny()
                .map(basePath -> basePath.relativize(localFile))
                .map(p -> "/" + p.toString())
                .orElseThrow();
        }
        if (!modifiedURL.contains(":")) {
            LOGGER.debug("Not absolute: {}", modifiedURL);
            return modifiedURL;
        }
        LOGGER.warn("How do we handle: {}?", modifiedURL);
        return modifiedURL;
    }

    private Optional<URL> tryResolve(String url) {
        String myUrl = url.startsWith("/") ? url.substring(1) : url;
        List<String> possiblePaths = CanonicalizationHelper.resolvePossiblePaths(myUrl);

        Optional<URL> firstPossibleName = possiblePaths.stream()
            .map(possiblePath -> {
                try {
                    if (MCRDeveloperTools.overrideActive()) {
                        final Optional<Path> overriddenFilePath
                            = MCRDeveloperTools.getOverriddenFilePath(possiblePath, true);

                        if (overriddenFilePath.isPresent()) {
                            return overriddenFilePath.get().toUri().toURL();
                        }
                    }
                    return context.getResource("/" + possiblePath);
                } catch (MalformedURLException e) {
                    // ignore exception because it seems to be a not valid name form
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .findFirst();
        return firstPossibleName;
    }

    private record ResolvedUri(String original, URI modified, URL resolved) {
    }
}
