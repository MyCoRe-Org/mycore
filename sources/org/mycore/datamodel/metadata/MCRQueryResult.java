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

package mycore.datamodel;

import java.util.*;
import mycore.common.MCRConfiguration;
import mycore.common.MCRConfigurationException;
import mycore.common.MCRException;
import mycore.datamodel.MCRQueryInterface;
import mycore.datamodel.MCRQueryResultArray;
import mycore.datamodel.MCRCommunicationInterface;

/**
 * This class is the result list of a XQuery question to the persistence
 * system. the result ist transparent over all searched instances of
 * a common instance collection or of a local instance.
 * The method use, depending of the persitence type property,
 * a interface to a query transformer form XQuery to the target system
 * query language.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRQueryResult
{
private int vec_max_length;
private MCRQueryResultArray mcr_result = null;
private MCRQueryInterface mcr_queryint = null;
private MCRConfiguration config = null;
private String mcr_type = null;
private String mcr_host = null;
private String mcr_query = null;

/**
 * This constructor create the MCRQueryResult class with an empty
 * MCRQueryResultArray.
 *
 * @param type the type of the MCRObjectId
 * @exception MCRConfigurationException     
 *                              an Exception of MyCoRe Configuration
 **/
public MCRQueryResult(String type) throws MCRConfigurationException
  {
  config = MCRConfiguration.instance();
  vec_max_length = config.getInt("MCR.query_max_results",10);
  mcr_result = new MCRQueryResultArray();
  if (type==null) { 
    throw new MCRException("The MCR object type is empty."); }
  mcr_type = type .toLowerCase();
  mcr_host = "";
  mcr_query = "";
  }

/**
 * This methode read the properties for the MCRObject type and call
 * the coresponding class for a query to the persistence layer. If
 * it was succesful, the MCRQueryResultArray is filled with answers.
 *
 * @param host                  the host as "local", "remote" or hostname
 * @param type 	                the MCRObject type
 * @param query	                the Query string
 * @exception MCRException      general Exception of MyCoRe
 * @exception MCRConfigurationException      
 *                              an Exception of MyCoRe Configuration
 **/
public final void setFromQuery(String host, String query) 
  throws MCRException, MCRConfigurationException
  {
  // precheck
  if (host==null) host = "local";
  host  = host .toLowerCase();
  if (query==null) query = "";
  query = query.toLowerCase();
  if ((host.equals(mcr_host))&&(query.equals(mcr_query))) { return; }
  mcr_host = host;
  mcr_query = query;
  boolean todo = false;
  // local
  if (mcr_host.equals("local")) {
    try {
      String proptype = "MCR.persistence_type_"+mcr_type.toLowerCase();
      String persist_type = config.getString(proptype);
      String proppers = "MCR.persistence_"+persist_type.toLowerCase()+
        "_query_name";
      mcr_queryint = (MCRQueryInterface)config.getInstanceOf(proppers);
      mcr_result = mcr_queryint.getResultList(mcr_query,mcr_type,
        vec_max_length);
      todo = true; }
    catch (Exception e) {
       throw new MCRException(e.getMessage(),e); }
    }
  // remote
  if(mcr_host.equals("remote")) {
    String hosts = config.getString("MCR.communication_hosts");
    int veclen = config.getInt("MCR.communication_max_hosts",3);
    Vector hostlist = new Vector(veclen);
    int i = 0;
    int j = hosts.length();
    int k = 0;
    while (k!=-1) {
      k = hosts.indexOf(",",i);
      if (k==-1) {
        hostlist.addElement(hosts.substring(i,j)); }
      else {
        hostlist.addElement(hosts.substring(i,k)); i = k+1; }
      }
    MCRCommunicationInterface comm = null;
    comm = (MCRCommunicationInterface)config
      .getInstanceOf("MCR.communication_class");
    comm.requestQuery(hostlist,mcr_type,mcr_query);
    mcr_result = comm.responseQuery();
    todo = true; }
  if(!todo) {
    Vector hostlist = new Vector(1);
    hostlist.addElement(mcr_host);
    MCRCommunicationInterface comm = null;
    comm = (MCRCommunicationInterface)config
      .getInstanceOf("MCR.communication_class");
    comm.requestQuery(hostlist,mcr_type,mcr_query);
    mcr_result = comm.responseQuery();
    }

  }

/**
 * This methode return the MCRQueryResultArray.
 *
 * @return the MCRQueryResultArray.
 **/
public final MCRQueryResultArray getResultArray()
  { return mcr_result; }

}

