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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRFileReader;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Implements the MCRContentStore interface to store the content of
 * IFS1 MCRFile objects in a structure of the new, but currently incomplete
 * IFS2 store. Example configuration:
 * 
 * <code>
 * MCR.IFS.ContentStore.FS.Class=org.mycore.datamodel.ifs2.MCRCStoreIFS2
 * MCR.IFS.ContentStore.FS.BaseDir=/foo/bar
 * MCR.IFS.ContentStore.FS.SlotLayout=4-2-2
 * </code>
 * 
 * @author Frank L\u00FCtzenkirchen
 **/
public class MCRCStoreIFS2 extends MCRContentStore {

    private String slotLayout;

    private String baseDir;

    private boolean ignoreOwnerBase;

    private final static Logger LOGGER = Logger.getLogger(MCRCStoreIFS2.class);

    @Override
    public void init(String storeID) {
        super.init(storeID);

        MCRConfiguration config = MCRConfiguration.instance();
        baseDir = config.getString(storeConfigPrefix + "BaseDir");
        LOGGER.info("Base directory for store " + storeID + " is " + baseDir);

        String pattern = config.getString("MCR.Metadata.ObjectID.NumberPattern", "0000000000");
        slotLayout = pattern.length() - 4 + "-2-2";
        slotLayout = config.getString(storeConfigPrefix + "SlotLayout", slotLayout);
        LOGGER.info("Default slot layout for store " + storeID + " is " + slotLayout);

        ignoreOwnerBase = config.getBoolean(storeConfigPrefix + "IgnoreOwnerBase", false);
    }

    private MCRFileStore getStore(String base) {
        String sid = getID();
        String storeBaseDir = baseDir;
        String prefix = "";

        if (!ignoreOwnerBase) {
            sid += "_" + base;
            storeBaseDir += File.separatorChar + base.replace("_", File.separator);
            prefix = base + "_";
        }

        MCRFileStore store = MCRStoreManager.getStore(sid, MCRFileStore.class);
        if (store == null)
            store = createStore(sid, storeBaseDir);
        store.prefix = prefix;
        return store;
    }

    private synchronized MCRFileStore createStore(String sid, String storeBaseDir) {
        try {
            configureStore(sid, storeBaseDir);
            return MCRStoreManager.createStore(sid, MCRFileStore.class);
        } catch (Exception ex) {
            String msg = "Could not create IFS2 file store with ID " + sid;
            throw new MCRConfigurationException(msg, ex);
        }
    }

    private void configureStore(String sid, String storeBaseDir) {
        String storeConfigPrefix = "MCR.IFS2.Store." + sid + ".";
        configureIfNotSet(storeConfigPrefix + "Class", MCRFileStore.class.getName());
        configureIfNotSet(storeConfigPrefix + "BaseDir", storeBaseDir);
        configureIfNotSet(storeConfigPrefix + "SlotLayout", slotLayout);
    }

    private void configureIfNotSet(String property, String value) {
        value = MCRConfiguration.instance().getString(property, value);
        MCRConfiguration.instance().set(property, value);
        LOGGER.info("Configured " + property + "=" + value);
    }

    private int getSlotID(String ownerID) {
        int pos = ownerID.lastIndexOf("_") + 1;
        return Integer.parseInt(ownerID.substring(pos));
    }

    private String getBase(String ownerID) {
        int pos = ownerID.lastIndexOf("_");
        return ownerID.substring(0, pos);
    }

    @Override
    protected boolean exists(MCRFileReader fr) {
        int slotID = getSlotID(fr.getOwnerID());
        String base = getBase(fr.getOwnerID());
        MCRFileStore store = getStore(base);

        try {
            MCRFileCollection slot = store.retrieve(slotID);
            if (slot == null)
                return false;

            String path = fr.getAbsolutePath();
            MCRNode node = slot.getNodeByPath(path);
            return (node != null);
        } catch (IOException ex) {
            String msg = "Exception checking existence of file " + fr.getAbsolutePath();
            throw new MCRPersistenceException(msg, ex);
        }
    }

    /**
     * For the given derivateID, returns the underlying IFS2 file collection storing the files of the derivate 
     */
    public MCRFileCollection getIFS2FileCollection(MCRObjectID derivateID) throws IOException {
        String oid = derivateID.toString();
        String base = getBase(oid);
        MCRFileStore store = getStore(base);

        int slotID = getSlotID(oid);
        return store.retrieve(slotID);
    }

    @Override
    protected String doStoreContent(MCRFileReader fr, MCRContentInputStream source) throws Exception {
        int slotID = getSlotID(fr.getOwnerID());
        String base = getBase(fr.getOwnerID());
        MCRFileStore store = getStore(base);

        MCRFileCollection slot = store.retrieve(slotID);
        if (slot == null)
            slot = store.create(slotID);

        String path = fr.getAbsolutePath();
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

        return fr.getOwnerID() + path;
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

    @Override
    public File getLocalFile(String storageId) throws IOException {
        MCRFile file = getFile(storageId);
        return file.getLocalFile();
    }

    private MCRFileCollection getSlot(String storageID) throws IOException {
        int pos = storageID.indexOf("/");
        String first = storageID.substring(0, pos);
        pos = first.lastIndexOf("_");
        String base = first.substring(0, pos);
        int slotID = Integer.parseInt(first.substring(pos + 1));
        return getStore(base).retrieve(slotID);
    }

    private MCRFile getFile(String storageID) throws IOException {
        MCRFileCollection slot = getSlot(storageID);
        int pos = storageID.indexOf("/") + 1;
        String path = storageID.substring(pos);
        return (MCRFile) (slot.getNodeByPath(path));
    }

    @Override
    public File getBaseDir() throws IOException {
        return new File(baseDir);
    }
}
