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

package org.mycore.ocfl.metadata;

import io.ocfl.api.OcflRepository;
import org.mycore.common.MCRException;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class MCRCompressedOCFLContent extends MCROCFLContent {
    public MCRCompressedOCFLContent(OcflRepository repository, String objectid, String fileName, String version) {
        super(repository, objectid, fileName, version);
    }

    public MCRCompressedOCFLContent(OcflRepository repository, String objectid, String fileName) {
        super(repository, objectid, fileName);
    }

    @Override
    public InputStream getInputStream() {
        InputStream inputStream = super.getInputStream();
        try {
            return new GZIPInputStream(inputStream);
        } catch (IOException e) {
            throw new MCRException(e);
        }

    }
}
