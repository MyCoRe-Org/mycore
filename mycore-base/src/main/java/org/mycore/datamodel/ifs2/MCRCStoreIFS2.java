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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;

import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRFileReader;

/**
 * Implements the MCRContentStore interface to store the content of
 * IFS1 MCRFile objects in a structure of the new, but currently incomplete
 * IFS2 store.
 * 
 * <code>
 * </code>
 * 
 * @author Frank Lützenkirchen
 **/
public class MCRCStoreIFS2 extends MCRContentStore {

    private MCRFileStore store;

    @Override
    public void init(String storeID) {
        super.init(storeID);

        store = MCRStoreManager.getStore(storeID, MCRFileStore.class);
        if (store == null)
            try {
                store = MCRStoreManager.createStore(storeID, MCRFileStore.class);
            } catch (Exception ex) {
                String msg = "Could not create IFS2 file store with ID " + storeID;
                throw new MCRConfigurationException(msg, ex);
            }
    }

    @Override
    protected boolean exists(MCRFileReader fr) {
        int slotID = getSlotID(fr);

        try {
            MCRFileCollection slot = store.retrieve(slotID);
            if (slot == null)
                return false;

            String path = fr.getPath();
            MCRNode node = slot.getNodeByPath(path);
            return (node != null);
        } catch (IOException ex) {
            String msg = "Exception checking existence of file " + fr.getPath();
            throw new MCRPersistenceException(msg, ex);
        }
    }

    @Override
    protected String doStoreContent(MCRFileReader fr, MCRContentInputStream source) throws Exception {

        int id = getSlotID(fr);
        MCRFileCollection slot = store.retrieve(id);
        if (slot == null)
            slot = store.create(id);

        String path = fr.getPath();
        MCRDirectory dir = slot;
        StringTokenizer steps = new StringTokenizer(path, "/");
        while (steps.hasMoreTokens()) {
            String step = steps.nextToken();
            if (steps.hasMoreTokens()) {
                MCRNode child = dir.getChild(step);
                if (child == null)
                    dir = dir.createDir(step);
                else
                    dir = (MCRDirectory) child;
            } else {
                MCRFile file = dir.createFile(step);
                file.setContent(new MCRStreamContent(source));
            }
        }

        return id + "/" + path;
    }

    @Override
    protected void doDeleteContent(String storageID) throws Exception {
        MCRFile file = getFile(storageID);
        MCRDirectory parent = (MCRDirectory) (file.getParent());
        file.delete();
        deleteEmptyParents(parent);
    }

    private void deleteEmptyParents(MCRDirectory dir) throws IOException {
        if ((dir == null) || dir.hasChildren())
            return;
        MCRDirectory parent = (MCRDirectory) (dir.getParent());
        dir.delete();
        deleteEmptyParents(parent);
    }

    @Override
    protected void doRetrieveContent(MCRFileReader fr, OutputStream target) throws Exception {
        doRetrieveMCRContent(fr).sendTo(target);
    }

    @Override
    protected InputStream doRetrieveContent(MCRFileReader fr) throws IOException {
        return doRetrieveMCRContent(fr).getInputStream();
    }

    @Override
    protected MCRContent doRetrieveMCRContent(MCRFileReader fr) throws IOException {
        String storageID = fr.getStorageID();
        MCRFile file = getFile(storageID);
        return file.getContent();
    }

    private int getSlotID(MCRFileReader fr) {
        String ownerID = fr.getOwnerID();
        int pos = ownerID.lastIndexOf("_") + 1;
        return Integer.parseInt(ownerID.substring(pos));
    }

    private MCRFileCollection getSlot(String storageID) throws IOException {
        int slotID = Integer.parseInt(storageID.split("/")[0]);
        return store.retrieve(slotID);
    }

    private MCRFile getFile(String storageID) throws IOException {
        MCRFileCollection slot = getSlot(storageID);
        int pos = storageID.indexOf("/") + 1;
        String path = storageID.substring(pos);
        return (MCRFile) (slot.getNodeByPath(path));
    }
}
