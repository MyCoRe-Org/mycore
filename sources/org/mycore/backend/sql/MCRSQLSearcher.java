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

import java.util.List;

import org.jdom.Element;
import org.mycore.backend.query.MCRHit;
import org.mycore.backend.query.MCRQuerySearcher;
import org.mycore.backend.query.MCRResults;


public class MCRSQLSearcher extends MCRQuerySearcher{

    public MCRResults runQuery(String query) {
        this.query = query;
        MCRSQLConnection c = MCRSQLConnectionPool.instance().getConnection();
        MCRResults result = new MCRResults();
        try{
            MCRSQLRowReader reader;
            MCRSQLQuery sqlquery = new MCRSQLQuery(query);
            reader = c.doQuery(sqlquery.getSQLQuery());
            List order = sqlquery.getOrderFields();
            while (reader.next()){
                MCRHit hit = new MCRHit(reader.getString("MCRID"));
                
                // fill hit meta
                for (int j=0; j<order.size(); j++){
                    String key = ((Element) order.get(j)).getAttributeValue("field") +"_" +
                        ((Element) order.get(j)).getAttributeValue("order");
                    String value = (String) reader.getString(((Element) order.get(j)).getAttributeValue("field"));
                    hit.addMetaValue(key,value);
                }
                result.addHit(hit);
            }
            if (order.size()>0)
                result.setSorted(true);
        }catch(Exception e){
            logger.error(e);
        }finally{
            c.release();
        }
        return result;
    }

}
