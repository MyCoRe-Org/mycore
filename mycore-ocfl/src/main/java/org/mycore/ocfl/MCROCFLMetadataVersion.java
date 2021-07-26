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

package org.mycore.ocfl;

import java.io.IOException;
import java.util.Date;

import org.jdom2.JDOMException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRAbstractMetadataVersion;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Provides information about a stored version of metadata and allows to
 * retrieve that version from SVN
 * 
 * @author Frank Lützenkirchen
 */
@XmlRootElement(name = "revision")
@XmlAccessorType(XmlAccessType.FIELD)
public class MCROCFLMetadataVersion extends MCRAbstractMetadataVersion<MCRContent> {

    public MCROCFLMetadataVersion(MCRContent vm, String revision, String user, Date date, char type) {
        super(vm, revision, user, date, type);
    }

    /**
     * Retrieves this version of the metadata
     * 
     * @return the metadata document as it was in this version
     * @throws MCRUsageException
     *             if this is a deleted version, which can not be retrieved
     */
    @Override
    public MCRContent retrieve() throws IOException {
        return vm;
    }

    /**
     * Replaces the current version of the metadata object with this version,
     * which means that a new version is created that is identical to this old
     * version. The stored metadata document is updated to this old version of
     * the metadata.
     */
    @Override
    public void restore() throws IOException, JDOMException {
        throw new IOException("Can not restore a version!");
    }
}
