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

package org.mycore.datamodel.ifs2;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Comparator;

class MCRPathComparator implements Comparator<Path>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(Path o1, Path o2) {
        String path1 = o1.getFileName().toString();
        String path2 = o2.getFileName().toString();
        return path1.compareTo(path2);
    }

}
