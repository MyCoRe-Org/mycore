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

package org.mycore.common.content;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * Reads MCRContent from a local file.
 * 
 * @author Thomas Scheffler
 */
public class MCRFileContent extends MCRPathContent {

    private File file;

    public MCRFileContent(File file) {
        super(file.toPath());
        this.file = file;
    }

    public MCRFileContent(String file) {
        this(new File(file));
    }

    @Override
    public Source getSource() throws IOException {
        return new StreamSource(file);
    }

}
