/*
 * 
 * $Revision$ $Date$
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

package org.mycore.backend.hibernate;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import org.mycore.backend.hibernate.tables.MCRCSTORE;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRFileReader;

/**
 * This class implements the MCRContentStore interface.
 */
public class MCRHIBCStore extends MCRContentStore {
    // logger
    static Logger logger = Logger.getLogger(MCRHIBCStore.class.getName());

    public void init(String storeID) {
        super.init(storeID);

        // System.out.println("### INIT " + storeID );
        // MCRConfiguration config = MCRConfiguration.instance();
    }

    private synchronized int getNextFreeID() throws Exception {
        Session session = MCRHIBConnection.instance().getSession();
        List l = session.createQuery("select max(storageid) from MCRCSTORE").list();

        if (l.size() > 0) {
            int max = ((Integer) l.get(0)).intValue();
            return max + 1;
        }
        return 1;
    }

    protected synchronized String doStoreContent(MCRFileReader file, MCRContentInputStream source) throws Exception {
        int ID = getNextFreeID();
        String storageID = String.valueOf(ID);

        Session session = MCRHIBConnection.instance().getSession();
        byte[] b = new byte[source.available()];
        source.read(b);

        MCRCSTORE c = new MCRCSTORE(ID, b);
        session.saveOrUpdate(c);

        return storageID;
    }

    protected synchronized void doDeleteContent(String ID) throws Exception {
        int storageID = Integer.valueOf(ID).intValue();
        Session session = MCRHIBConnection.instance().getSession();
        List l = session.createQuery("from MCRCSTORE where storageid=" + storageID).list();

        for (int t = 0; t < l.size(); t++) {
            session.delete(l.get(t));
        }
    }

    protected void doRetrieveContent(MCRFileReader file, OutputStream target) throws Exception {
        int storageID = Integer.valueOf(file.getStorageID()).intValue();
        Session session = MCRHIBConnection.instance().getSession();
        List l = session.createQuery("from MCRCSTORE where storageid=" + storageID).list();

        if (l.size() < 1) {
            throw new MCRException("No such content: " + storageID);
        }

        MCRCSTORE st = (MCRCSTORE) l.get(0);
        byte[] c = st.getContentBytes();
        target.write(c);
    }

    protected InputStream doRetrieveContent(MCRFileReader file) throws Exception {
        int storageID = Integer.valueOf(file.getStorageID()).intValue();
        Session session = MCRHIBConnection.instance().getSession();
        MCRCSTORE st = (MCRCSTORE) session.createQuery("from MCRCSTORE where storageid=" + storageID).uniqueResult();
        return st.getInputStream();
    }

}
