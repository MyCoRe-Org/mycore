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

package org.mycore.backend.sql;

import java.io.InputStream;

import org.apache.log4j.Logger;
import org.jdom.input.SAXBuilder;
import org.mycore.backend.query.MCRQuerySearcherInterface;
import org.mycore.common.MCRConfigurationException;

public class MCRSQLSearcher implements MCRQuerySearcherInterface{
    
    /** The logger */
    public static Logger LOGGER = Logger.getLogger(MCRSQLSearcher.class.getName());
    
    public MCRSQLSearcher(){

    }
    
    public void runQuery(int no){
        try{
            System.out.println("read document");
            SAXBuilder builder = new SAXBuilder();
            InputStream in = this.getClass().getResourceAsStream("/query1.xml");

            if (in == null) {
                String msg = "Could not find configuration file";
                throw new MCRConfigurationException(msg);
            }
            
            MCRSQLQuery query = new MCRSQLQuery(builder.build(in));
            in.close();


            MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
            
            try {
                MCRSQLRowReader reader;
                reader = c.doQuery(query.getSQLQuery());
                while (reader.next()){
                    System.out.println("ID: " + reader.getString("MCRID"));
                }
               
           }catch(Exception e){
               LOGGER.error(e);
           }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
