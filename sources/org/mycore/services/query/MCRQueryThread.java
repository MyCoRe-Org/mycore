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

package org.mycore.services.query;

import org.mycore.datamodel.metadata.*;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRQueryResultArray;
import org.mycore.datamodel.classifications.MCRClassification;
import java.net.*;
import java.io.*;

/**
 * This class queries remote or local in a thread.
 *
 * @author Mathias Zarick
 * @version $Revision$ $Date$
 */
public class MCRQueryThread extends Thread {
    private int vec_max_length;
    private MCRConfiguration config;
    private MCRQueryInterface mcr_queryint;
    private MCRQueryResultArray mcr_result;
    private String mcr_type;
    private String mcr_query;
    private String hostAlias;

// constructor
    public MCRQueryThread(ThreadGroup threadGroup, String hostAlias,
                          String mcr_query, String mcr_type,
                          MCRQueryResultArray mcr_result) {
	super(threadGroup,hostAlias);
        this.hostAlias = hostAlias;
        this.mcr_query = mcr_query;
        this.mcr_type = mcr_type;
        this.mcr_result = mcr_result;
        config = MCRConfiguration.instance();
        vec_max_length = config.getInt("MCR.query_max_results",10);
    }

// lets the thread run
public void run() 
  {
  if (hostAlias.equalsIgnoreCase("local")) {
    try {
      if (mcr_type.equalsIgnoreCase("class")) {
        MCRClassification cl = new MCRClassification();
        org.jdom.Document jdom = cl.search(mcr_query);
        if (jdom != null) {
          org.jdom.Element el = jdom.getRootElement();
          String id = el.getAttributeValue("ID");
          MCRQueryResultArray res = new MCRQueryResultArray();
          res.add("local",id,1,el);
          mcr_result.importElements(res);
          }
        }
      else {
        String persist_type = config.getString("MCR.persistence_type","cm7");
        String proppers = "MCR.persistence_"+persist_type.toLowerCase()+
          "_query_name";
        mcr_queryint = (MCRQueryInterface)config.getInstanceOf(proppers);
        mcr_result.importElements(mcr_queryint.getResultList(mcr_query,mcr_type,
          vec_max_length));
        }
    }
    catch (Exception e) {
       throw new MCRException(e.getMessage(),e); }
    }
  else {
    MCRCommunicationInterface comm = null;
    comm = (MCRCommunicationInterface)
      config.getInstanceOf("MCR.communication_"+hostAlias+"_query_class");
    mcr_result.importElements(comm.requestQuery(hostAlias,mcr_type,mcr_query));
    }
  }

}
