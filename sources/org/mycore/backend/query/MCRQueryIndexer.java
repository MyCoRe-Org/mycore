/**
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
package org.mycore.backend.query;


import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;

public abstract class MCRQueryIndexer {

    public abstract void initialLoad();
    public abstract void updateObject(MCRBase object);
    public abstract void deleteObject(MCRObjectID objectid);   
    public abstract void insertInQuery(String mcrid, List values);
    public abstract void updateConfiguration();
    
    public static Logger logger = Logger.getLogger(MCRQueryIndexer.class.getName());

    protected static String SQLQueryTable = MCRConfiguration.instance().getString("MCR.QueryTableName", "MCRQuery");
    protected static String querytypes = MCRConfiguration.instance().getString("MCR.QueryTypes", "document,author");
    
    public static MCRQueryManager queryManager = MCRQueryManager.getInstance();
    
    static private MCRQueryIndexer implementation;
    public static MCRQueryIndexer getInstance() 
    {
        try{
            if(implementation == null) {
                implementation = (MCRQueryIndexer)MCRConfiguration.instance().getSingleInstanceOf("MCR.QueryIndexer_class_name", "org.mycore.backend.sql.MCRSQLIndexer");
            }
        }catch(Exception e){
            logger.error(e);
        }
        return implementation;
    }
}
