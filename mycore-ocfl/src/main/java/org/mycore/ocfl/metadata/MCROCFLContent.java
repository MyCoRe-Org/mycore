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

package org.mycore.ocfl.metadata;

import java.io.IOException;
import java.io.InputStream;

import org.mycore.common.content.MCRContent;

import io.ocfl.api.OcflRepository;
import io.ocfl.api.model.ObjectVersionId;

public class MCROCFLContent extends MCRContent {

    private OcflRepository repository;

    private String ocflObjectID;

    private String fileName;

    private String version;

    public MCROCFLContent(OcflRepository repository, String ocflObjectID, String fileName, String version) {
        this.repository = repository;
        this.ocflObjectID = ocflObjectID;
        this.fileName = fileName;
        this.version = version;
    }

    public MCROCFLContent(OcflRepository repository, String ocflObjectID, String fileName) {
        this.repository = repository;
        this.ocflObjectID = ocflObjectID;
        this.fileName = fileName;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return repository
            .getObject(version == null ? ObjectVersionId.head(ocflObjectID) :
                    ObjectVersionId.version(ocflObjectID, version))
            .getFile(fileName).getStream();
    }
}
