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
 * A {@link MCRClasspathDirsProvider} implements a strategy to obtain the list of filesystem directories
 * that are part of the classpath.
 */
public interface MCRClasspathDirsProvider {

    List<Path> getClasspathDirs(MCRHints hints);

}
