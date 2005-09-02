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
package org.mycore.backend.lucene;

import java.io.InputStream;
import java.util.List;

import org.jdom.input.SAXBuilder;
import org.mycore.backend.query.MCRQuerySearcher;
import org.mycore.backend.query.MCRResults;
import org.mycore.common.MCRConfigurationException;

/**
 * 
 * @author Harald Richter
 *
 */
public class MCRLuceneSearcher extends MCRQuerySearcher{


    public void runQuery(){
        try{
            System.out.println("read document for Lucene Query");
            SAXBuilder builder = new SAXBuilder();
            InputStream in = this.getClass().getResourceAsStream("/query1.xml");

            if (in == null) {
                String msg = "Could not find configuration file";
                throw new MCRConfigurationException(msg);
            }
            
            MCRLuceneQuery query = new MCRLuceneQuery(builder.build(in));
            in.close();
/*
            Session session = MCRHIBConnection.instance().getSession();
            Transaction tx = session.beginTransaction();

            List l = session.createQuery(query.getHIBQuery()).list();

            for(int i=0; i<l.size(); i++){
                MCRHIBQuery res = new MCRHIBQuery(l.get(i));
                System.out.println("ID: " + res.getValue("getmcrid"));
            }
*/            
//            tx.commit();
//            session.close();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public MCRResults runQuery(String query) {
        // TODO Auto-generated method stub
        return null;
    }
}
