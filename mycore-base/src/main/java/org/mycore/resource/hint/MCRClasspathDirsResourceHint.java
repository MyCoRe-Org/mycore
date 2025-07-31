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

package org.mycore.resource.hint;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.mycore.common.hint.MCRHint;
import org.mycore.common.hint.MCRHintKey;

public final class MCRClasspathDirsResourceHint implements MCRHint<List<Path>> {

    private final List<Path> classpathDirs;

    public MCRClasspathDirsResourceHint() {
        this.classpathDirs = getClassPathDirs();
    }

    private static List<Path> getClassPathDirs() {

        List<Path> classPathDirs = new LinkedList<>();

        String classPath = System.getProperty("java.class.path");
        String[] classPathEntries = classPath.split(Pattern.quote(File.pathSeparator));
        for (String classPathEntry : classPathEntries) {
            Path path = Path.of(classPathEntry);
            if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                classPathDirs.add(path);
            }
        }

        return List.copyOf(classPathDirs);

    }

    @Override
    public MCRHintKey<List<Path>> key() {
        return MCRResourceHintKeys.CLASSPATH_DIRS;
    }

    @Override
    public Optional<List<Path>> value() {
        return Optional.of(classpathDirs);
    }

}
