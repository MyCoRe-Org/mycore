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

package org.mycore.solr.index.cs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.mycore.datamodel.niofs.MCRContentTypes;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrPathContentStream extends MCRSolrAbstractContentStream<Path> {

    private BasicFileAttributes attrs;

    public MCRSolrPathContentStream(Path path, BasicFileAttributes attrs) {
        super(path);
        this.attrs = attrs;
    }

    @Override
    protected void setup() throws IOException {
        Path file = getSource();
        this.setName(file.toString());
        this.setSourceInfo(file.getClass().getSimpleName());
        this.setContentType(MCRContentTypes.probeContentType(file));
        this.setSize(attrs.size());
        this.setInputStream(Files.newInputStream(file));
    }

    public BasicFileAttributes getAttrs() {
        return attrs;
    }

}
