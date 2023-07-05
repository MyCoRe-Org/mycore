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

package org.mycore.common;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;

/**
 * @author Sebastian Hofmann
 */
public class MCRDeveloperTools {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * @return true if any override is defined
     */
    public static boolean overrideActive() {
        return MCRConfiguration2.getString("MCR.Developer.Resource.Override").isPresent();
    }

    public static Stream<Path> getOverridePaths() {
        if (!overrideActive()) {
            return Stream.empty();
        }
        return MCRConfiguration2
            .getOrThrow("MCR.Developer.Resource.Override", MCRConfiguration2::splitValue)
            .map(Paths::get);
    }

    /**
     * Reads the property <code>MCR.Developer.Resource.Override</code> and checks if any of the containing paths
     * contains the path parameter.
     * @param path the resource to override
     * @param webResource if true META-INF/resources will be appended to the paths in the property
     * @return the path to new file
     */
    public static Optional<Path> getOverriddenFilePath(String path, boolean webResource) {
        if (overrideActive()) {
            final String[] pathParts = path.split("/");

            return MCRConfiguration2
                .getOrThrow("MCR.Developer.Resource.Override", MCRConfiguration2::splitValue)
                .map(Paths::get)
                .map(p -> webResource ? p.resolve("META-INF").resolve("resources") : p)
                .map(p -> {
                    try {
                        return MCRUtils.safeResolve(p, pathParts);
                    } catch (MCRException | InvalidPathException e) {
                        LOGGER.debug("Exception in safeResolve", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(Files::exists)
                .peek(p -> LOGGER.debug("Found overridden Resource: {}", p.toAbsolutePath().toString()))
                .findFirst();
        }
        return Optional.empty();
    }
}
