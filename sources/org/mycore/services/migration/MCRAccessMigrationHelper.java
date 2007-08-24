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

package org.mycore.services.migration;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.hibernate.Session;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
class MCRAccessMigrationHelper {

    static File getExportFile() {
        Properties props = (Properties) (MCRConfiguration.instance().getProperties());
        String workingDir = props.getProperty("MCR.BaseDirectory", null);
        if (workingDir == null) {
            workingDir = System.getProperty("java.io.tmpdir");
        }

        File dir = new File(workingDir, "access-migration");
        dir.mkdirs();
        return new File(dir, "mcraccess-data.xml");
    }

    static void dropTable() throws SQLException {
        Connection con = MCRHIBConnection.instance().getSession().connection();
        Statement stmt = con.createStatement();
        stmt.executeUpdate("DROP TABLE MCRACCESS");
        stmt.close();
        con.commit();
        MCRHIBConnection.instance().flushSession();
        Session session = MCRHIBConnection.instance().getSession();
        session.clear();
    }

    static void deleteExportFile() {
        final File exportFile = getExportFile();
        exportFile.delete();
        exportFile.getParentFile().delete();
    }

}
