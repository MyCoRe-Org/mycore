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
protected static String ROOT_TAG = "/mycoreobject";

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
  // read prefix from configuration
  String sb = new String("MCR.persistence_cm8_"+type.toLowerCase());
  String itemtypename = conf.getString(sb); 
  String itemtypeprefix = conf.getString(sb+"_prefix");
  // prepare query
System.out.println("================================");
System.out.println("MCRCM8TransformXQueryToCM8 : "+query);
System.out.println("================================");
  int startpos = 0;
  int stoppos = query.length();
  int operpos = -1;
  String onecond = "";
  String oper = "";
  StringBuffer cond = new StringBuffer("/mycoreobject[");
  while (startpos<stoppos) {
    onecond = getNextCondition(startpos,stoppos,query);
    startpos += onecond.length();
    int klammerauf = onecond.indexOf('[');
    int klammerzu = onecond.indexOf(']');
    if ((klammerauf == -1) || (klammerzu == -1)) { break; }
    cond.append(onecond.substring(klammerauf+1,klammerzu));
    if (startpos<stoppos) {
      operpos = query.toLowerCase().indexOf(" and ",startpos);
      if (operpos != -1) {
        startpos = operpos+5;
        cond.append(' ').append(query.substring(operpos,startpos)).append(' ');
        continue;
        }
      operpos = query.toLowerCase().indexOf(" or ",startpos);
      if (operpos != -1) {
        startpos = operpos+4;
        cond.append(' ').append(query.substring(operpos,startpos)).append(' ');
        continue;
        }
      }
    }
  cond.append(']');
System.out.println("MCRCM8TransformXQueryToCM8 : "+cond.toString());
System.out.println("================================");
  if (cond.toString().equals("/mycoreobject[]")) { 
    query = ""; }
  else {
    query = traceOneCondition(cond.toString(),itemtypename,itemtypeprefix); }
  if (query.length()==0) { query = "/"+itemtypename; }
System.out.println("MCRCM8TransformXQueryToCM8 : "+query);
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
      dataId = resitem.dataId(DK_CM_NAMESPACE_ATTR,itemtypeprefix+"ID");     
      id = (String) resitem.getData(dataId);
System.out.println(itemtypeprefix+"ID :"+id);
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
 * This private method get the next condition of the query string.
 *
 * @param startpos   the first character position
 * @param stoppos    the last character position
 * @param query      the query string
 * @return the separated condition
 **/
private final String getNextCondition(int startpos,int stoppos,String query)
  {
  int numopen = 0;
  int numclose = 0;
  StringBuffer sb = new StringBuffer(128);
  for (int i=startpos;i<stoppos;i++) {
    sb.append(query.charAt(i));
    if (query.charAt(i)=='[') { numopen++; }
    if (query.charAt(i)==']') { numclose++; }
    if ((numopen==1)&&(numclose==1)) { break; }
    }
  return sb.toString();
  }

/**
 * This is a private routine they trace one condition.
 *
 * @param onecond  one single condition
 * @param itemtypename the name of the itme type
 * @param itemtypeprefix the prefix of the itme type
 * @ return the transfromed query for CM7.
 **/
private final String traceOneCondition(String cond, String itemtypename,
  String itemtypeprefix)
  {
  int i, j, k,l ,m, n;
  StringBuffer sb = new StringBuffer(128);
  // search [..]
  int klammerauf = cond.indexOf("[");
  int klammerzu = cond.indexOf("]",klammerauf+1);
  if ((klammerauf==-1)||(klammerzu==-1)) { return ""; }
  // create pretag as value of the path to the data
  if (klammerauf!=ROOT_TAG.length()) { return ""; }
  if (!cond.startsWith(ROOT_TAG)) { return ""; }
  String pretag = "/"+itemtypename;
//System.out.println("PRETAG="+pretag);
  // search operations
  String tag[] = new String[10];
  String op[] = new String[10];
  String value[] = new String[10];
  String bool[] = new String[10];
  int counter = 0;
  int tippelauf = 0;
  int tippelzu = 0;
  int tippelauf1 = 0;
  int tippelauf2 = 0;
  int tagstart = klammerauf+1;
  int tagstop = 0;
  int opstart = 0;
  int opstop = 0;
  while ((tippelauf!=-1)&&(tippelzu!=-1)) {
    tippelauf1 = cond.indexOf("\"",tippelzu+1);
    tippelauf2 = cond.indexOf("'",tippelzu+1);
    if (tippelauf1!=-1) {
      tippelauf = tippelauf1;
      tippelzu = cond.indexOf("\"",tippelauf+1);
      if (tippelzu==-1) { break; }
      }
    else {
      if (tippelauf2!=-1) {
        tippelauf = tippelauf2;
        tippelzu = cond.indexOf("'",tippelauf+1);
        if (tippelzu==-1) { break; }
        }
      else { break; }
      }
    value[counter] = new String(cond.substring(tippelauf+1,tippelzu).trim());
    boolean opset = false;
    if (!opset) {
      opstart = cond.toUpperCase().indexOf("CONTAINS(",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = "contains";
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) {
      opstart = cond.toUpperCase().indexOf("LIKE",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = "like";
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) {
      opstart = cond.indexOf("!=",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = "!=";
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) {
      opstart = cond.indexOf(">=",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = ">=";
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) {
      opstart = cond.indexOf("<=",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = "<=";
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) {
      opstart = cond.indexOf("=",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = "=";
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) {
      opstart = cond.indexOf("<",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = "<";
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) {
      opstart = cond.indexOf(">",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = ">";
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) { return ""; }
    bool[counter] = "";
    if (tippelzu+5<cond.length()) {
      tagstart = cond.toLowerCase().indexOf(" and ",tippelzu+1);
      if (tagstart==-1) {
        tagstart = cond.toLowerCase().indexOf(" or ",tippelzu+1);
        if (tagstart==-1) { return ""; }
        tagstart +=4; bool[counter] = " or ";
        }
      else { tagstart+=5; bool[counter] = " and "; }
      }
    counter++;
    }

/*
  for (i=0;i<counter;i++) {
    System.out.println("TAG="+tag[i]);
    System.out.println("OPER="+op[i]);
    System.out.println("VALUE="+value[i]);
    System.out.println("BOOLEAN="+bool[i]);
    System.out.println();
    }
*/

  StringBuffer sbout = new StringBuffer();
  sbout.append(pretag).append('[');
  for (i=0;i<counter;i++) {
    j = 0;
    k = tag[i].length();
    m = 0;
    n = 0;
    while( j < k) {
      l = tag[i].indexOf("/",j);
      if (l == -1) { 
        if (tag[i].charAt(j) == '@') { 
          sbout.append('@').append(itemtypeprefix)
            .append(tag[i].substring(j+1,k));  break; }
        else {
          sbout.append(itemtypeprefix).append(tag[i].substring(j,k))
            .append('/').append('@').append(itemtypeprefix)
            .append(tag[i].substring(j,k)); break;
          }
        }
      sbout.append(itemtypeprefix).append(tag[i].substring(j,l)).append('/');
      m = j; n = l;
      j = l+1; 
      }
    if (op[i].equals("contains")) {
      return ""; }
    else {
      if (op[i].equals("like")) { 
        // replace * with %
        value[i] = value[i].replace('*','%'); 
        } 
      sbout.append(' ').append(op[i]).append(" \"").append(value[i])
        .append("\"").append(bool[i]); }
    }
  sbout.append(']');
  //return sbout.toString().replace('\'','\"');
  return sbout.toString();
  }

}

