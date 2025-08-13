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

import java.nio.file.Path;
import java.util.List;

import org.mycore.common.hint.MCRHints;

/**
 * A {@link MCRNoOpClasspathDirsProvider} is a {@link MCRClasspathDirsProvider} that returns an empty list of
 * filesystem directories.
 * <p>
 * No configuration options are available.
 * <p>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.common.MCRNoOpClasspathDirsProvider
 * </code></pre>
 */
public final class MCRNoOpClasspathDirsProvider implements MCRClasspathDirsProvider {

    @Override
    public List<Path> getClasspathDirs(MCRHints hints) {
        return List.of();
    }

}
