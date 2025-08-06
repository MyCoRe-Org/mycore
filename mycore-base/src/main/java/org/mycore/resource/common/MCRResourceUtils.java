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

package org.mycore.resource.common;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import org.mycore.common.MCRException;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.MCRResourceResolver;

/**
 * A utility class with helper methods  for the process of resolving a resource in {@link MCRResourceResolver}.
 */
public final class MCRResourceUtils {

    private MCRResourceUtils() {
    }

    public static URL toFileUrl(String path) {
        return toFileUrl(Path.of(path));
    }

    public static URL toFileUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new MCRException("Failed to convert path to file URL: " + path, e);
        }
    }

    public static URL toJarFileUrl(String jarPath, String path) {
        return toJarFileUrl(Path.of(jarPath), path);
    }

    public static URL toJarFileUrl(Path jarPath, String path) {
        try {
            String absoluteResourcePath = MCRResourcePath.ofPathOrThrow(path).asAbsolutePath();
            String urlString = "jar:" + toFileUrl(jarPath) + "!" + absoluteResourcePath;
            return URI.create(urlString).toURL();
        } catch (MalformedURLException e) {
            throw new MCRException("Failed to convert paths to JAR file URL: " + jarPath + " / " + path, e);
        }
    }

}
