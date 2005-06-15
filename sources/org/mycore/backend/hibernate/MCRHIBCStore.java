/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.backend.hibernate;

import org.hibernate.*;

import org.mycore.common.*;
import org.mycore.datamodel.ifs.*;
import org.mycore.backend.hibernate.tables.MCRCSTORE;

import java.util.*;
import java.io.*;

/**
 * This class implements the MCRContentStore interface.
 */
public class MCRHIBCStore extends MCRContentStore {
    public void init(String storeID) {
        super.init(storeID);

        //    System.out.println("### INIT " + storeID );
        //    MCRConfiguration config = MCRConfiguration.instance();
    }

    private synchronized int getNextFreeID(String tableName) throws Exception {
        return (int)MCRHIBConnection.instance().getID();
    }

    protected String doStoreContent(MCRFileReader file, MCRContentInputStream source) throws Exception {
        String tableName = file.getContentTypeID();
        int ID = getNextFreeID(tableName);
        String storageID = tableName + ":" + ID;

        Session session = MCRHIBConnection.instance().getSession();
        byte[] b = new byte[source.available()];
        source.read(b);
	MCRCSTORE c = new MCRCSTORE(storageID, b);
	Transaction tx = session.beginTransaction();
	session.saveOrUpdate(c);
	tx.commit();
	session.close();

	return storageID;
    }

    protected void doDeleteContent(String storageID) throws Exception {
        Session session = MCRHIBConnection.instance().getSession();
	Transaction tx = session.beginTransaction();
	List l = session.createQuery("from MCRCSTORE where storageid='"+storageID+"'").list();
	int t;
	for(t=0;t<l.size();t++) {
	    session.delete(l.get(t));
	}
	tx.commit();
	session.close();
    }

    protected void doRetrieveContent(MCRFileReader file, OutputStream target) throws Exception {
        String storageID = file.getStorageID();
        Session session = MCRHIBConnection.instance().getSession();
        List l = session.createQuery("from MCRCSTORE where storageid='"+storageID+"'").list();
        if(l.size() < 1)
            throw new MCRException("No such content: "+ storageID);
        MCRCSTORE st = (MCRCSTORE)l.get(0);
        byte[] c = st.getContentBytes();
        target.write(c);
    }
}

