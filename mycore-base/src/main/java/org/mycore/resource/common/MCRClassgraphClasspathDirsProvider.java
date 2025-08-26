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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.mycore.common.hint.MCRHints;
import org.mycore.resource.hint.MCRResourceHintKeys;

/**
 * A {@link MCRClassgraphClasspathDirsProvider} is a {@link MCRClasspathDirsProvider} that returns the list of
 * filesystem directories obtained via {@link MCRClassgraphUtil#scanClasspath(ClassLoader)}.
 * <p>
 * It uses the {@link ClassLoader} hinted at by {@link MCRResourceHintKeys#CLASS_LOADER}, if present.
 * <p>
 * No configuration options are available.
 * <p>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.common.MCRClassgraphClasspathDirsProvider
 * </code></pre>
 */
public final class MCRClassgraphClasspathDirsProvider implements MCRClasspathDirsProvider {

    @Override
    public List<Path> getClasspathDirs(MCRHints hints) {
        return hints.get(MCRResourceHintKeys.CLASS_LOADER).map(this::scanClasspath).orElseGet(List::of);
    }

    private List<Path> scanClasspath(ClassLoader classLoader) {

        List<Path> classpathDirs = new LinkedList<>();

        LinkOption[] linkOptions = MCRResourceUtils.linkOptions();
        List<URI> classpathEntries = MCRClassgraphUtil.scanClasspath(classLoader);
        for (URI classpathEntry : classpathEntries) {
            if (classpathEntry.getScheme().equals("file")) {
                Path classpathDirOrJarFile = Path.of(classpathEntry);
                if (Files.isDirectory(classpathDirOrJarFile, linkOptions)) {
                    classpathDirs.add(classpathDirOrJarFile);
                }
            }
        }

        return List.copyOf(classpathDirs);

    }

}
