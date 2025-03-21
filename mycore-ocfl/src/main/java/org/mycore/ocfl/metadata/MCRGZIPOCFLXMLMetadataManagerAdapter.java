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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRObjectID;

import io.ocfl.api.model.OcflObjectVersion;
import io.ocfl.api.model.VersionNum;

public class MCRGZIPOCFLXMLMetadataManagerAdapter extends MCROCFLXMLMetadataManagerAdapter {

    @Override
    protected InputStream getContentStream(MCRContent xml) throws IOException {
        InputStream contentStream = super.getContentStream(xml);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(baos)) {
            contentStream.transferTo(out);
        }
        byte[] byteArray = baos.toByteArray();
        return new ByteArrayInputStream(byteArray);
    }

    @Override
    protected InputStream getStoredContentStream(MCRObjectID mcrid, OcflObjectVersion storeObject) throws IOException {
        InputStream storedContentStream = super.getStoredContentStream(mcrid, storeObject);
        return new GZIPInputStream(storedContentStream);
    }

    @Override
    protected MCROCFLContent getContent(MCRObjectID id, String ocflObjectID, VersionNum key) {
        return new MCRGZIPOCFLContent(getRepository(), ocflObjectID, buildFilePath(id),
            key.toString());
    }

    @Override
    protected String buildFilePath(MCRObjectID mcrid) {
        return super.buildFilePath(mcrid) + ".gz";
    }
}
