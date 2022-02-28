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

import java.io.IOException;
import java.util.Date;

import org.jdom2.JDOMException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.streams.MCRByteArrayOutputStream;
import org.mycore.datamodel.common.MCRAbstractMetadataVersion;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

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
public class MCRMetadataVersion extends MCRAbstractMetadataVersion<MCRVersionedMetadata> {

    public MCRMetadataVersion(MCRVersionedMetadata vm, String revision, String user, Date date, char type) {
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
        if (getType() == DELETED) {
            String msg = "You can not retrieve a deleted version, retrieve a previous version instead";
            throw new MCRUsageException(msg);
        }
        try {
            SVNRepository repository = vm.getStore().getRepository();
            MCRByteArrayOutputStream baos = new MCRByteArrayOutputStream();
            repository.getFile(vm.getStore().getSlotPath(vm.getID()), Long.parseLong(getRevision()), null, baos);
            baos.close();
            return new MCRByteContent(baos.getBuffer(), 0, baos.size(), getDate().getTime());
        } catch (SVNException e) {
            throw new IOException(e);
        }
    }

    /**
     * Replaces the current version of the metadata object with this version,
     * which means that a new version is created that is identical to this old
     * version. The stored metadata document is updated to this old version of
     * the metadata.
     */
    @Override
    public void restore() throws IOException, JDOMException {
        vm.update(retrieve());
    }
}
