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

import java.util.Iterator;

import org.apache.commons.vfs.FileObject;
import org.mycore.common.MCRException;

/**
 * Stores file collections containing files and directories.
 * 
 * For each store, properties must be defined, for example
 * 
 * MCR.IFS2.Store.ID.Class=org.mycore.datamodel.ifs2.MCRFileStore
 * MCR.IFS2.Store.ID.BaseDir=/foo/bar MCR.IFS2.Store.ID.SlotLayout=4-2-2
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFileStore extends MCRStore {

    /**
     * Returns the store with the given ID
     * 
     * @param ID
     *            the ID of the store
     * @return the store with that ID
     */
    public static MCRFileStore getStore(String type) {
        return (MCRFileStore) (MCRStore.getStore(type));
    }

    /**
     * Creates and stores a new, empty file collection using the next free ID in
     * the store.
     * 
     * @return a newly created file collection
     */
    public MCRFileCollection create() throws Exception {
        int id = getNextFreeID();
        return create(id);
    }

    /**
     * Creates and stores a new, empty file collection with the given ID
     * 
     * @param id
     *            the ID of the file collection
     * @return a newly created file collection
     * @throws Exception
     *             when a file collection with the given ID already exists
     */
    public MCRFileCollection create(int id) throws Exception {
        FileObject fo = getSlot(id);
        if (fo.exists()) {
            String msg = "FileCollection with ID " + id + " already exists";
            throw new MCRException(msg);
        }
        return new MCRFileCollection(this, id);
    }

    /**
     * Returns the file collection stored under the given ID, or null when no
     * collection is stored for the given ID.
     * 
     * @param id
     *            the file collection's ID
     * @return the file collection with the given ID, or null
     */
    public MCRFileCollection retrieve(int id) throws Exception {
        FileObject fo = getSlot(id);
        if (!fo.exists())
            return null;
        else
            return new MCRFileCollection(this, id);
    }

    /**
     * Repairs metadata of all file collections stored here
     * 
     * @throws Exception
     */
    public void repairAllMetadata() throws Exception {
        for (Iterator<Integer> e = listIDs(MCRStore.ASCENDING); e.hasNext();)
            retrieve(e.next()).repairMetadata();
    }
}
