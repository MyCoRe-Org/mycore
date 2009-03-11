/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.ifs2;

import java.util.Enumeration;

import org.apache.commons.vfs.FileObject;
import org.jdom.Document;

/**
 * Stores metadata both in a local filesystem structure and in a Subversion
 * repository. Metadata changes can be tracked and restored.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRVersioningMetadataStore extends MCRMetadataStore {

    MCRVersioningMetadataStore(String type, String baseDir, String slotLayout) {
        super(type, baseDir, slotLayout);
    }

    public MCRVersionedMetadata create(Document xml, int id) throws Exception {
        return (MCRVersionedMetadata) (super.create(xml, id));
    }

    public MCRVersionedMetadata create(Document xml) throws Exception {
        return (MCRVersionedMetadata) (super.create(xml));
    }

    public MCRVersionedMetadata retrieve(int id) throws Exception {
        return (MCRVersionedMetadata) (super.retrieve(id));
    }

    /**
     * Updates all stored metadata to the latest revision in SVN
     */
    public void updateAll() throws Exception {
        for (Enumeration<Integer> ids = listIDs(true); ids.hasMoreElements();)
            retrieve(ids.nextElement()).update();
    }

    public void delete(int id) throws Exception {
        MCRVersionedMetadata vm = retrieve(id);
        vm.delete();
    }

    protected MCRVersionedMetadata buildMetadataObject(FileObject fo, int id) {
        return new MCRVersionedMetadata(this, fo, id);
    }
}
