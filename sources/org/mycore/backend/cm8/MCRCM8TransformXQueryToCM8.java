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

package mycore.cm8;

import java.util.*;
import java.text.*;
import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;
import mycore.common.MCRConfiguration;
import mycore.common.MCRException;
import mycore.common.MCRPersistenceException;
import mycore.datamodel.MCRQueryInterface;
import mycore.datamodel.MCRObjectID;
import mycore.datamodel.MCRQueryResultArray;

/**
 * This is the tranformer implementation for CM 8 from XQuery language
 * to the CM Search Engine language (this is XQuery like).
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRCM8TransformXQueryToCM8 implements MCRQueryInterface,
  DKConstantICM 
{
// common data
protected static String NL =
  new String((System.getProperties()).getProperty("line.separator"));
// defaults
private final int MAX_RESULTS = 1000;
// private data
private MCRConfiguration conf = null;
private int maxres = 0;

/**
 * The constructor.
 **/
public MCRCM8TransformXQueryToCM8()
  { 
  conf = MCRConfiguration.instance();
  maxres = conf.getInt("MCR.query_max_results",MAX_RESULTS);
  }

/**
 * This method parse the XQuery string and return the result as
 * MCRQueryResultArray. If the type is null or empty or maxresults
 * is lower 1 an empty list was returned.
 *
 * @param query                 the XQuery string
 * @param maxresults            the maximum of results
 * @param type                  the MCRObject type
 * @return                      a result list as MCRQueryResultArray
 **/
public final MCRQueryResultArray getResultList(String query, String type, 
  int maxresults) 
  {
  // check the parameter
  MCRQueryResultArray result = new MCRQueryResultArray();
  if ((type == null) || ((type = type.trim()).length() ==0)) {
    return result; }
  if ((maxresults < 1) || (maxresults > maxres)) {
    return result; }
  // prepare query
  if (query.equals("\'\'")) { query = ""; }
System.out.println("================================");
System.out.println("MCRCM7TransformXQueryToCM8 : "+query);
System.out.println("================================");
  query = prepare(query,type);
System.out.println("MCRCM7TransformXQueryToCM8 : >>>"+query+"<<<");
System.out.println("================================");
  // Start the search
  DKDatastoreICM connection = null;
  try {
    connection = MCRCM8ConnectionPool.getConnection();
    DKNVPair parms [] = new DKNVPair[3];
    parms[0] = new DKNVPair(DK_CM_PARM_MAX_RESULTS,
      "0");
      //new Integer(maxresults).toString());
    parms[1] = new DKNVPair(DK_CM_PARM_RETRIEVE,
      new Integer(DK_CM_CONTENT_YES));
    parms[2] = new DKNVPair(DK_CM_PARM_END,null);
    DKResults rsc = (DKResults)connection.evaluate(query,
      DK_CM_XQPE_QL_TYPE,parms);
    dkIterator iter = rsc.createIterator();
System.out.println("Results :"+rsc.cardinality());
    String id = "";
    int rank = 0;
    byte [] xml = null;
    short dataId = 0;
    while (iter.more()) {
      DKDDO resitem = (DKDDO)iter.next();
      resitem.retrieve();
      dataId = resitem.dataId(DK_CM_NAMESPACE_ATTR,"ID");     
      id = (String) resitem.getData(dataId);
System.out.println("ID :"+id);
      dataId = resitem.dataId(DK_CM_NAMESPACE_ATTR,"xml");     
      xml = (byte []) resitem.getData(dataId);
      result.add("local",id,rank,xml);
      }
    }
  catch (Exception e) {
    throw new MCRPersistenceException("CM8 Search error."+e.getMessage()); }
  finally {
    MCRCM8ConnectionPool.releaseConnection(connection); }
  return result;
  }

  /**
   * This private method prepare the input query string.
   *
   * @param query the input query string
   * @return the output query string
   **/
  private final String prepare(String query, String type)
    {
    // Read the item type name from the configuration
    String sb = new String("MCR.persistence_cm8_"+type.toLowerCase());
    String itemtypename = conf.getString(sb); 
    // Empty query
    if (query.length()==0) {
      StringBuffer qs = new StringBuffer(128);
      qs.append('/').append(itemtypename);
      return qs.toString(); }
    // replace mycoreobject with the ItemType name
    int i = 0;
    int j = query.length();
    int k = 0;
    StringBuffer qs = new StringBuffer(128);
    String mo = "mycoreobject";
    while (i<j) {
      k = query.indexOf(mo,i);
      if (k==-1) { qs.append(query.substring(i,j)); i = j; continue; }
      qs.append(query.substring(i,k)).append(itemtypename);
      i = i + k + mo.length();
      }
    String nquery = qs.toString().replace('\'','\"');
    return nquery;
    }

}

