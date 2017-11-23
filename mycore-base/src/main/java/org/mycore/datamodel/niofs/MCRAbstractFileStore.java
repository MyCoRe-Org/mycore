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

package org.mycore.datamodel.niofs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Path;

/**
 * @author Thomas Scheffler
 *
 */
public abstract class MCRAbstractFileStore extends FileStore {

    /**
     * Returns base directory of this filestore.
     */
    public abstract Path getBaseDirectory() throws IOException;

    /**
     * Translates the given path into an absolute path of the physical filesystem.
     * 
     * To retrieve a relative path use {@link Path#relativize(Path)} on {@link #getBaseDirectory()}.
     * The returned path may not exist or may be null.
     */
    public abstract Path getPhysicalPath(MCRPath path);

}
