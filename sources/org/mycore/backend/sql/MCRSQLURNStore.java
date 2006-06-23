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
package org.mycore.backend.sql;

import org.mycore.common.MCRConfiguration;
import org.mycore.services.urn.MCRURNStore;

/**
 * Stores URN data in a SQL table
 * 
 * @author Frank Lützenkirchen
 */
public class MCRSQLURNStore implements MCRURNStore {

    /** The name of the SQL table that stores the URN data */
    private String table;

    public MCRSQLURNStore() {
        table = MCRConfiguration.instance().getString("MCR.URN.SQLStore.TableName");
        if (!MCRSQLConnection.doesTableExist(table)) {
            MCRSQLStatement stmt = new MCRSQLStatement(table);
            stmt.addColumn("URN VARCHAR(250) NOT NULL PRIMARY KEY");
            stmt.addColumn("DOCID VARCHAR(64)");
            MCRSQLConnection.justDoUpdate(stmt.toCreateTableStatement());
        }
    }

    public boolean isAssigned(String urn) {
        MCRSQLConnection conn = MCRSQLConnectionPool.instance().getConnection();
        MCRSQLRowReader reader = null;

        try {
            MCRSQLStatement stmt = new MCRSQLStatement(table).setCondition("urn", urn);
            reader = conn.doQuery(stmt.toSelectStatement());
            String docID = null;
            if (reader.next())
                docID = reader.getString("DOCID");
            return ((docID != null) && (docID.length() > 0));
        } finally {
            if (reader != null)
                reader.close();
            conn.release();
        }
    }

    public void assignURN(String urn, String docID) {
        MCRSQLStatement stmt = new MCRSQLStatement(table);
        stmt.setValue("URN", urn);
        stmt.setValue("DOCID", docID);
        MCRSQLConnection.justDoUpdate(stmt.toInsertStatement());
    }

    public String getURNforDocument(String docID) {
        MCRSQLConnection conn = MCRSQLConnectionPool.instance().getConnection();
        MCRSQLRowReader reader = null;

        try {
            MCRSQLStatement stmt = new MCRSQLStatement(table).setCondition("DOCID", docID);
            reader = conn.doQuery(stmt.toSelectStatement());
            return (reader.next() ? reader.getString("URN") : null);
        } finally {
            if (reader != null)
                reader.close();
            conn.release();
        }
    }

    public String getDocumentIDforURN(String urn) {
        MCRSQLConnection conn = MCRSQLConnectionPool.instance().getConnection();
        MCRSQLRowReader reader = null;

        try {
            MCRSQLStatement stmt = new MCRSQLStatement(table).setCondition("URN", urn);
            reader = conn.doQuery(stmt.toSelectStatement());
            return (reader.next() ? reader.getString("DOCID") : null);
        } finally {
            if (reader != null)
                reader.close();
            conn.release();
        }
    }

    public void removeURN(String urn) {
        MCRSQLStatement stmt = new MCRSQLStatement(table);
        stmt.setCondition("URN", urn);
        MCRSQLConnection.justDoUpdate(stmt.toDeleteStatement());
    }
}
