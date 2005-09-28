/*
 * $RCSfile$
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

package org.mycore.backend.sql;

import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRFileReader;

/**
 * This class implements the MCRContentStore interface to store the content of
 * MCRFile objects in a sql database.
 * 
 * @author Harald Richter
 * @version $Revision$ $Date$
 */
public class MCRCStoreSQLDB extends MCRContentStore {
    public void init(String storeID) {
        super.init(storeID);

        // System.out.println("### INIT " + storeID );
        // MCRConfiguration config = MCRConfiguration.instance();
    }

    private synchronized int getNextFreeID(String tableName) throws Exception {
        String sql = "SELECT MAX( ID ) FROM " + tableName;
        String id = MCRSQLConnection.justGetSingleValue(sql);

        int result = ((id == null) ? 0 : Integer.parseInt(id));

        return result + 1;
    }

    protected String doStoreContent(MCRFileReader file, MCRContentInputStream source) throws Exception {
        String tableName = file.getContentTypeID();
        int ID = getNextFreeID(tableName);
        String storageID = tableName + ":" + ID;

        MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

        try {
            connection.getJDBCConnection().setAutoCommit(false);

            String insert = "INSERT INTO " + tableName + " VALUES (?, ?)";
            PreparedStatement statement = connection.getJDBCConnection().prepareStatement(insert);
            statement.setInt(1, ID);
            statement.setBinaryStream(2, source, source.available());

            // System.out.println("##### SIZE " + source.available() );
            statement.execute();
            statement.close();

            connection.getJDBCConnection().commit();
            connection.getJDBCConnection().setAutoCommit(true);
        } catch (Exception ex) {
            try {
                connection.getJDBCConnection().rollback();
            } catch (SQLException ignored) {
            }

            throw ex;
        } finally {
            connection.release();
        }

        return storageID;
    }

    protected void doDeleteContent(String storageID) throws Exception {
        int i = storageID.indexOf(':');
        String tableName = storageID.substring(0, i);
        String ID = storageID.substring(i + 1);
        String sql = "DELETE FROM " + tableName + " WHERE ID = " + ID;

        // System.out.println("###### DELETE " + sql );
        MCRSQLConnection.justDoUpdate(sql);
    }

    protected void doRetrieveContent(MCRFileReader file, OutputStream target) throws Exception {
        MCRSQLConnection connection = MCRSQLConnectionPool.instance().getConnection();

        try {
            String storageID = file.getStorageID();
            int i = storageID.indexOf(':');
            String tableName = storageID.substring(0, i);
            String ID = storageID.substring(i + 1);
            String select = "SELECT XML FROM " + tableName + " WHERE ID = " + ID;
            Statement statement = connection.getJDBCConnection().createStatement();
            ResultSet rs = statement.executeQuery(select);

            if (!rs.next()) {
                String msg = ID + " is not in table  " + tableName;
                throw new MCRUsageException(msg);
            }

            MCRUtils.copyStream(rs.getBinaryStream(1), target);
            rs.close();
        } finally {
            connection.release();
        }
    }
}
