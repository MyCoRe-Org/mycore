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

import org.hibernate.cfg.Configuration;
import org.hibernate.type.BigIntegerType;
import org.hibernate.type.BlobType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
import org.mycore.common.MCRConfiguration;

/**
 * Creater class for mapping files 
 * This class generates the xml mapping for the hibernate configuration and 
 * adds each file to the current configuration
 * 
 * @author Arne Seifert
 */
public class MCRHIBMapping {

    private StringType dbString = new StringType();
    private IntegerType dbInt = new IntegerType();
    private BigIntegerType dbBigInt = new BigIntegerType();
    private BlobType dbBlob = new BlobType();
    private TimestampType dbTimestamp = new TimestampType();
    
    MCRConfiguration config = MCRConfiguration.instance();
    
    public void generateTables(Configuration cfg){
        try{
            MCRTableGenerator map;

		    // derivate            
		    map = new MCRTableGenerator( "MCRXMLTYPE", "org.mycore.backend.hibernate.tables.MCRXMLType", "", 2);
            map.addIDColumn("mcrid","MCRID", dbString, 64, "assigned");
			map.addIDColumn("mcrversion", "MCRVERSION", dbInt, 0, "assigned");
			map.addColumn("mcrtype", "MCRTYPE", dbString, 64, false, false);
			map.addColumn("mcrxml", "MCRXML", dbBlob, 0, false, false);
		    cfg.addXML(map.getTableXML());

		    // Category
		    map = new MCRTableGenerator(config.getString("MCR.classifications_store_sql_table_categ"), "org.mycore.backend.hibernate.tables.MCRCATEG","", 2);
		    map.addIDColumn("id","ID",dbString, 128, "assigned");
		    map.addIDColumn("clid","CLID",dbString, 64, "assigned");
		    map.addColumn("pid","PID",dbString, 128, true,false);
		    map.addColumn("url","URL",dbString, 254, true,false);
		    cfg.addXML(map.getTableXML());
	   
		    // Categorylabel
		    map = new MCRTableGenerator(config.getString("MCR.classifications_store_sql_table_categlabel"), "org.mycore.backend.hibernate.tables.MCRCATEGLABEL", "", 3);
		    map.addIDColumn("id", "ID", dbString, 128, "assigned");
		    map.addIDColumn("clid", "CLID", dbString, 64, "assigned");
		    map.addIDColumn("lang", "LANG", dbString, 8, "assigned");
		    map.addColumn("text", "TEXT", dbString, 254, true, false);
		    map.addColumn("mcrdesc", "MCRDESC", dbString, 254, true, false);
		    cfg.addXML(map.getTableXML());
		    
		    // Classification
		    map = new MCRTableGenerator(config.getString("MCR.classifications_store_sql_table_class"), "org.mycore.backend.hibernate.tables.MCRCLASS", "", 1);
		    map.addIDColumn("id","ID", dbString, 64, "assigned");
		    cfg.addXML(map.getTableXML());
		    
		    // Classification Label
		    map = new MCRTableGenerator(config.getString("MCR.classifications_store_sql_table_classlabel"), "org.mycore.backend.hibernate.tables.MCRCLASSLABEL", "", 2);
		    map.addIDColumn("id","ID", dbString, 64, "assigned");
		    map.addIDColumn("lang","LANG", dbString, 8, "assigned");
		    map.addColumn("text", "TEXT",dbString,254, false, false);
		    map.addColumn("mcrdesc", "MCRDESC", dbString, 254, false, false);
		    cfg.addXML(map.getTableXML());
		    
		    // FS Nodes
		    map = new MCRTableGenerator(config.getString("MCR.IFS.FileMetadataStore.SQL.TableName"), "org.mycore.backend.hibernate.tables.MCRFSNODES", "", 1);
		    map.addIDColumn("id", "ID", dbString, 16, "assigned");
		    map.addColumn("pid", "PID", dbString, 16, false, false);
		    map.addColumn("type", "TYPE", dbString, 1, true, false);
		    map.addColumn("owner", "OWNER", dbString, 64, false, false);
		    map.addColumn("name", "NAME", dbString, 250, true, false);
		    map.addColumn("label", "LABEL", dbString, 250, false, true);
		    map.addColumn("size", "SIZE", dbBigInt, 0, false, false);
		    map.addColumn("date", "DATE", dbTimestamp, 0, false, false);
		    map.addColumn("storeid", "STOREID", dbString, 32, true, false);
		    map.addColumn("storageid", "STORAGEID", dbString, 250, true, false);
		    map.addColumn("fctid", "FCTID", dbString, 32, true, false);
		    map.addColumn("md5", "MD5", dbString, 32, true, false);
		    map.addColumn("numchdd", "NUMCHDD", dbInt, 0, true, false);
		    map.addColumn("numchdf","NUMCHDF", dbInt, 0, true, false);
		    map.addColumn("numchtd", "NUMCHTD", dbInt, 0, true, false);
		    map.addColumn("numchtf", "NUMCHTF", dbInt, 0, true, false);
		    cfg.addXML(map.getTableXML());
		    
		    // Group Admins
		    map = new MCRTableGenerator(config.getString("MCR.users_store_sql_table_group_admins"), "org.mycore.backend.hibernate.tables.MCRGROUPADMINS", "", 1);
		    map.addIDColumn("id", "ID", dbInt, 0, "assigned");
		    map.addColumn("gid", "GID", dbString, 20, true, false);
		    map.addColumn("userid", "USERID", dbString, 20, true, false);
		    map.addColumn("groupid", "GROUPID", dbString, 20, true, false);
		    cfg.addXML(map.getTableXML());
		   
		    // Group Members
		    map = new MCRTableGenerator(config.getString("MCR.users_store_sql_table_group_members"), "org.mycore.backend.hibernate.tables.MCRGROUPMEMBERS", "", 1);
		    map.addIDColumn("id", "ID", dbInt, 0, "assigned");
		    map.addColumn("gid", "GID", dbString, 20, true, false);
		    map.addColumn("userid", "USERID", dbString, 20, true, false);
		    map.addColumn("groupid", "GROUPID", dbString, 20, true, false);
		    cfg.addXML(map.getTableXML());
		    
		    // Group
		    map = new MCRTableGenerator(config.getString("MCR.users_store_sql_table_groups"), "org.mycore.backend.hibernate.tables.MCRGROUPS", "", 1);
		    map.addIDColumn("gid", "GID", dbString, 20, "assigned");
		    map.addColumn("creator", "CREATOR", dbString, 20, true, false);
		    map.addColumn("creationdate", "CREATIONDATE", dbTimestamp, 0, true, false);
		    map.addColumn("modifieddate", "MODIFIEDDATE", dbTimestamp, 0, true, false);
		    map.addColumn("description", "DESCRIPTION", dbString, 200, true, false);
		    cfg.addXML(map.getTableXML());
		    
		    // Link Class
		    map = new MCRTableGenerator(config.getString("MCR.linktable_store_sql_table_class"), "org.mycore.backend.hibernate.tables.MCRLINKCLASS", "", 2);
		    map.addIDColumn("mcrfrom", "MCRFROM", dbString, 64, "assigned");
		    map.addIDColumn("mcrto", "MCRTO", dbString, 194, "assigned");
		    cfg.addXML(map.getTableXML());
		    
		    // Link Class
		    map = new MCRTableGenerator(config.getString("MCR.linktable_store_sql_table_href"), "org.mycore.backend.hibernate.tables.MCRLINKHREF", "", 2);
		    map.addIDColumn("mcrfrom", "MCRFROM", dbString, 64, "assigned");
		    map.addIDColumn("mcrto", "MCRTO", dbString, 194, "assigned");
		    cfg.addXML(map.getTableXML());
		    
		    // Privileges
		    map = new MCRTableGenerator(config.getString("MCR.users_store_sql_table_privileges"), "org.mycore.backend.hibernate.tables.MCRPRIVSM", "", 1);
		    map.addIDColumn("name", "NAME", dbString, 100, "assigned");
		    map.addColumn("description", "DESCRIPTION", dbString, 200, false, false);
		    cfg.addXML(map.getTableXML());
		    
		    // Privilegeslookup
		    map = new MCRTableGenerator(config.getString("MCR.users_store_sql_table_privs_lookup"), "org.mycore.backend.hibernate.tables.MCRPRIVSLOOKUP", "", 1);
		    map.addIDColumn("id", "ID", dbInt, 0, "assigned");
		    map.addColumn("gid", "GID", dbString, 20, false, false);
		    map.addColumn("name", "NAME", dbString, 200, false, false);
		    cfg.addXML(map.getTableXML());
		    
		    // User
		    map = new MCRTableGenerator(config.getString("MCR.users_store_sql_table_users"), "org.mycore.backend.hibernate.tables.MCRUSERS", "", 2);
		    //map = new MCRTableGenerator(config.getString("MCR.users_store_sql_table_users"), "org.mycore.backend.hibernate.MCRUserExt", "", 2);
		    map.addIDColumn("numid", "NUMID", dbInt, 0, "assigned");
		    map.addIDColumn("uid", "UID", dbString, 20, "assigned");
		    map.addColumn("creator", "CREATOR", dbString, 20, true, false);
		    map.addColumn("creationdate", "CREATIONDATE", dbTimestamp, 0, false, false);
		    map.addColumn("modifieddate", "MODIFIEDDATE", dbTimestamp, 0, false, false);
		    map.addColumn("description", "DESCRIPTION", dbString, 200, false, false);
		    map.addColumn("passwd", "PASSWD", dbString, 128, true, false);
		    map.addColumn("enabled", "ENABLED", dbString, 8, true, false);
		    map.addColumn("upd", "UPD", dbString, 8, true, false);
		    map.addColumn("salutation", "SALUTATION", dbString, 24, false, false);
		    map.addColumn("firstname", "FIRSTNAME", dbString, 64, false, false);
		    map.addColumn("lastname", "LASTNAME", dbString, 32, false, false);
		    map.addColumn("street", "STREET", dbString, 64, false, false);
		    map.addColumn("city", "CITY", dbString, 32, false, false);
		    map.addColumn("postalcode", "POSTALCODE", dbString, 32, false, false);
		    map.addColumn("country", "COUNTRY", dbString, 32, false, false);
		    map.addColumn("state", "STATE", dbString, 32, false, false);
		    map.addColumn("institution", "INSTITUTION", dbString, 64, false, false);
		    map.addColumn("faculty", "FACULTY", dbString, 64, false, false);
		    map.addColumn("department", "DEPARTMENT", dbString, 64, false, false);
		    map.addColumn("institute", "INSTITUTE", dbString, 64, false, false);
		    map.addColumn("telephone", "TELEPHONE", dbString, 32, false, false);
		    map.addColumn("fax", "FAX", dbString, 32, false, false);
		    map.addColumn("email", "EMAIL", dbString, 64, false, false);
		    map.addColumn("cellphone", "CELLPHONE", dbString, 32, false, false);
		    map.addColumn("primgroup", "PRIMGROUP", dbString, 20, true, false);
		    cfg.addXML(map.getTableXML());
		    
		    map = new MCRTableGenerator(config.getString("MCRID"), "org.mycore.backend.hibernate.tables.MCRID", "", 2);
		    map.addIDColumn("id", "ID", dbInt, 0, "native");
		    cfg.addXML(map.getTableXML());
		    
		    cfg.createMappings();
		}catch(Exception e){
			System.out.println(e.toString());
		}
    }    
}
