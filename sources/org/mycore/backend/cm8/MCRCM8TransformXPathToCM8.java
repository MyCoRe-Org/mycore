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

package org.mycore.backend.cm8;

import java.util.*;

import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;
import org.mycore.services.query.MCRQueryInterface;
import org.mycore.services.query.MCRQueryBase;

/**
 * This is the tranformer implementation for CM 8 from XPath language
 * to the CM Search Engine language (this is XPath like).
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRCM8TransformXPathToCM8 extends MCRQueryBase implements
  DKConstantICM 
{
// private data
public static final String DEFAULT_QUERY = "";

/**
 * The constructor.
 **/
public MCRCM8TransformXPathToCM8()
  { super(); }

/**
 * This method start the Query over one object type and return the
 * result as MCRXMLContainer.
 *
 * @param type                  the MCRObject type
 * @return                      a result list as MCRXMLContainer
 **/
public final MCRXMLContainer startQuery(String type) 
  {
  MCRXMLContainer result = new MCRXMLContainer();
  // read prefix from configuration
  String sb = new String("MCR.persistence_cm8_"+type.toLowerCase());
  String itemtypename = config.getString(sb); 
  String itemtypeprefix = config.getString(sb+"_prefix");

  // Search in the text document
  String tsquery = "";
  for (int i=0;i<subqueries.size();i++) {
    if (((String)subqueries.get(i)).indexOf(XPATH_ATTRIBUTE_DOCTEXT) != -1) {
      tsquery = (String)subqueries.get(i);
      flags.set(i,Boolean.TRUE);
      }
    }

  // Search in the metadata
  // Select the query strings
  StringBuffer cond = new StringBuffer(1024);
  for (int i=0;i<subqueries.size();i++) {
    if (((Boolean)flags.get(i)).booleanValue()) continue;
      cond.append(' ').append(traceOneCondition((String)subqueries.get(i),
        itemtypeprefix)).append(' ').append((String)andor.get(i));
      flags.set(i,Boolean.TRUE);
    }
  logger.debug("Codition transformation "+cond.toString());
  // build the query
  StringBuffer query = new StringBuffer(1024);
  query.append('/').append(itemtypename); 
  if (cond.toString().trim().length() > 0) {
    query.append('[').append(cond.toString()).append(']'); }
  logger.debug("Transformed query "+query);
  // Start the search
  DKDatastoreICM connection = null;
  try {
    connection = MCRCM8ConnectionPool.instance().getConnection();
    DKNVPair parms [] = new DKNVPair[3];
    parms[0] = new DKNVPair(DK_CM_PARM_MAX_RESULTS,
      new Integer(maxresults).toString());
    parms[1] = new DKNVPair(DK_CM_PARM_RETRIEVE,
      //new Integer(DK_CM_CONTENT_ATTRONLY | DK_CM_CONTENT_LINKS_OUTBOUND));
      new Integer(DK_CM_CONTENT_YES));
    parms[2] = new DKNVPair(DK_CM_PARM_END,null);
    DKResults rsc = (DKResults)connection.evaluate(query.toString(),
      DK_CM_XQPE_QL_TYPE,parms);
    dkIterator iter = rsc.createIterator();
    logger.debug("Results :"+rsc.cardinality());
    MCRXMLTableManager xmltable = MCRXMLTableManager.instance();
    String id = "";
    int rank = 0;
    byte [] xml = null;
    short dataId = 0;
    while (iter.more()) {
      DKDDO resitem = (DKDDO)iter.next();
      resitem.retrieve();
      dataId = resitem.dataId(DK_CM_NAMESPACE_ATTR,itemtypeprefix+"ID");     
      id = (String) resitem.getData(dataId);
      logger.debug(itemtypeprefix+"ID :"+id);
      try {
        xml = xmltable.retrieve(type,new MCRObjectID(id));
        result.add( "local", id, 0, xml);
        }
      catch (Exception e) {
        logger.warn(">>>>>>>>>>>>>>> OLD VERSION <<<<<<<<<<<<<<<<<<<");
        try {
          dataId = resitem.dataId(DK_CM_NAMESPACE_ATTR,itemtypeprefix+"xml");
          xml = (byte []) resitem.getData(dataId);
          result.add("local",id,rank,xml);
          }
        catch (Exception ex) {
          logger.warn("Can not retieve the XML data from the old CM8 store."); }
        }
      }
    }
  catch (Exception e) {
    throw new MCRPersistenceException("CM8 Search error."+e.getMessage()); }
  finally {
    MCRCM8ConnectionPool.instance().releaseConnection(connection); }
  return result;
  }

/**
 * This is a private routine they trace one condition.
 *
 * @param onecond  one single condition
 * @param itemtypeprefix the prefix of the itme type
 * @ return the transfromed query for CM7.
 **/
private final String traceOneCondition(String condstr, String itemtypeprefix)
  {
  // search operations
  int maxcount = 10;
  String pathin[] = new String[maxcount];
  String pathout[] = new String[maxcount];
  String tag[] = new String[maxcount];
  String op[] = new String[maxcount];
  String value[] = new String[maxcount];
  String bool[] = new String[maxcount];
  int counter = 0;
  boolean klammer = false;
  // search for []
  String cond = "";
  int i = condstr.indexOf("[");
  if (i != -1) {
    int j = condstr.indexOf("]");
    if (j == -1) { throwQueryEx(); }
    klammer = true;
    cond = condstr.substring(i+1,j);
    String p = condstr.substring(0,i);
    for (int k=0;k<maxcount;k++) { pathout[k] = p; pathin[k] = ""; }
    }
  else { 
    for (int k=0;k<maxcount;k++) { pathin[k] = ""; pathout[k] = ""; }
    cond = condstr;
    }
  logger.debug("Incomming condition : "+cond);
  // analyze cond
  int tippelauf = 0;
  int tippelzu = 0;
  int tippelauf1 = 0;
  int tippelauf2 = 0;
  int tagstart = 0;
  int opstart = 0;
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
    // has the tag a path (if true split them)
    StringBuffer sbpath = new StringBuffer("");
    int j = 0, l, lastl = 0;
    int k = tag[counter].length();
    while( j < k) {
      l = tag[counter].indexOf("/",j);
      if (l == -1) { 
        String nt = "";
        if (tag[counter].charAt(j) == '@') {
          nt = tag[counter].substring(j,tag[counter].length()); }
        if (tag[counter].charAt(j) == '*') { nt = "*"; }
        if (tag[counter].indexOf("ts()",j) != -1) { nt = "*"; }
        if (tag[counter].indexOf("text()",j) != -1) { nt = "text()"; }
        if (nt.length() == 0) { 
          nt = "text()"; 
          if (lastl != 0) { sbpath.append('/'); }
          sbpath.append(tag[counter].substring(j,tag[counter].length()));
          }
        if (sbpath.length() != 0) { pathin[counter] = sbpath.toString(); }
        else { pathin[counter] = ""; }
        tag[counter] = nt;
        break; }
      if (lastl != 0) { sbpath.append('/'); }
      sbpath.append(tag[counter].substring(j,l));
      lastl = l;
      j = l+1;
      }
    // add the itemtypeprefix to the pathout 
    sbpath = new StringBuffer("");
    j = 0;
    k = pathout[counter].length();
    while( j < k) {
      l = pathout[counter].indexOf("/",j);
      if (l == -1) {
        sbpath.append(itemtypeprefix).append(pathout[counter].substring(j,k));
        pathout[counter] = sbpath.toString();
        break; }
      sbpath.append(itemtypeprefix).append(pathout[counter].substring(j,l+1));
      j = l+1;
      }
    // add the itemtypeprefix to the pathin 
    sbpath = new StringBuffer("");
    j = 0;
    k = pathin[counter].length();
    while( j < k) {
      l = pathin[counter].indexOf("/",j);
      if (l == -1) {
        sbpath.append(itemtypeprefix).append(pathin[counter].substring(j,k));
        pathin[counter] = sbpath.toString();
        break; }
      sbpath.append(itemtypeprefix).append(pathin[counter].substring(j,l+1));
      j = l+1;
      }
    // replace the tag if it is text()    
    if (tag[counter].equals("text()")) {
      j = 0;
      if (pathin[counter].length() != 0) {
        k = pathin[counter].length();
        while( j < k) {
          l = pathin[counter].indexOf("/",j);
          if (l == -1) {
            tag[counter] = "@"+pathin[counter].substring(j+2,k); break; }
          j = l+1;
          }
        }
      else {
        if (pathout[counter].length() == 0) { tag[counter] = "*"; }
        k = pathout[counter].length();
        while( j < k) {
          l = pathout[counter].indexOf("/",j);
          if (l == -1) {
            tag[counter] = "@"+pathout[counter].substring(j+2,k); break; }
          j = l+1;
          }
        }
      }
    // increment the counter
    counter++;
    }

  // debug
/*
  for (i=0;i<counter;i++) {
    logger.debug("PATHOUT="+pathout[i]);
    logger.debug("PATHIN="+pathin[i]);
    logger.debug("TAG="+tag[i]);
    logger.debug("OPER="+op[i]);
    logger.debug("VALUE="+value[i]);
    logger.debug("BOOLEAN="+bool[i]);
    logger.debug("");
    }
*/

  StringBuffer sbout = new StringBuffer();
  // if we have a common path
  if (klammer) { sbout.append(pathout[0]).append('['); }
  for (i=0;i<counter;i++) {
    // if we have a xml namespace
    int x = tag[i].indexOf("@xml:");
    if (x != -1) { 
      tag[i] = "@"+tag[i].substring(x+5,tag[i].length()); }
    // if we have a xlink namespace
    x = tag[i].indexOf("@xlink:");
    if (x != -1) { 
      tag[i] = "@xlink"+tag[i].substring(x+7,tag[i].length()); }
    // expand the attributes with itemtypeprefix
    if (!tag[i].equals("*")) {
      tag[i] = "@" + itemtypeprefix + tag[i].substring(1,tag[i].length()); }
    // create for CONTAINS
    if (op[i].equals("contains")) {
      value[i] = value[i].replace('*','%');  
      StringTokenizer st = new StringTokenizer(value[i]);
      int stcount = st.countTokens();
      while (st.hasMoreTokens()) {
System.out.println(tag[i]);
        if (tag[i].equals("*")) {
          sbout.append(" contains-text(@").append(itemtypeprefix)
            .append("ts,\"\'").append(st.nextToken()).append("\'\")=1");
          }
        else {
          sbout.append(" contains-text(");
          if (pathin[i].length() != 0) { sbout.append(pathin[i]).append('/'); }
          sbout.append(tag[i])
            .append(",\"\'").append(st.nextToken()).append("\'\")=1");
          }
        if ((stcount > 1) && (st.countTokens()>0)) { sbout.append(" and "); }
        }
      sbout.append(bool[i]).append(' ');
      continue;
      }
    if (pathin[i].length() != 0) { sbout.append(pathin[i]).append('/'); }
    sbout.append(tag[i]);
    if (op[i].equals("like")) { 
      // replace * with %
      value[i] = "%"+value[i].replace('*','%')+"%"; } 
    // is value[0] a date
    try {
      GregorianCalendar date = MCRUtils.covertDateToGregorianCalendar(value[i]); 
      long number = 0;
      if (date.get(Calendar.ERA) == GregorianCalendar.AD) {
        number = (4000+date.get(Calendar.YEAR))*10000 +
                     date.get(Calendar.MONTH)*100 +
                     date.get(Calendar.DAY_OF_MONTH); }
      else {
        number = (4000-date.get(Calendar.YEAR))*10000 +
                     date.get(Calendar.MONTH)*100 +
                     date.get(Calendar.DAY_OF_MONTH); }
      logger.debug("Date "+value[i]+" as number = "+Long.toString(number));
      value[i] = Long.toString(number);
      sbout.append(' ').append(op[i]).append(' ').append(value[i])
        .append(bool[i]); 
      continue;
      }
    catch (Exception e) { }
    // is value[0] a number
    try {
      double numb = Double.parseDouble(value[i]);
      sbout.append(' ').append(op[i]).append(' ').append(value[i])
        .append(' ').append(bool[i]); 
      continue;
      }
    catch (Exception e) { }
    sbout.append(' ').append(op[i]).append(" \"").append(value[i])
      .append("\"").append(bool[i]); 
    }
  if (klammer) { sbout.append(']'); }
  return sbout.toString();
  }

}

