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

package org.mycore.backend.cm7;

import java.util.*;
import java.text.*;

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.query.*;
import org.mycore.common.xml.MCRXMLContainer;

/**
 * This is the tranformer implementation for CM 7 from XQuery language
 * to the CM Text Search Engine.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRCM7TransformXQueryToText extends MCRCM7TransformToText
implements MCRQueryInterface 
{
// common data
protected static String NL =
  new String((System.getProperties()).getProperty("line.separator"));
private MCRConfiguration conf =  MCRConfiguration.instance();
private String ccsid = null;
private String lang = null;
private String langid = null;
private Logger logger = null;

// 32 Bit
protected static int MAX_DATE_STRING_LENGTH = 1024 * 1024 * 1024 * 2;
protected static int MAX_NUMBER_STRING_LENGTH = 1024 * 1024 * 1024 * 2;
protected static String ROOT_TAG_OBJECT = "/MYCOREOBJECT";
protected static String ROOT_TAG_DERIVATE = "/MYCOREDERIVATE";

/**
 * The constructor.
 **/
public MCRCM7TransformXQueryToText()
  {
  conf =  MCRConfiguration.instance();
  ccsid = conf.getString("MCR.persistence_cm7_textsearch_ccsid","850");
  lang = conf.getString("MCR.persistence_cm7_textsearch_lang","DEU");
  langid = conf.getString("MCR.persistence_cm7_textsearch_langid","4841");
  logger = MCRCM7ConnectionPool.getLogger();
  }

/**
 * This method parse the XQuery string and return the result as
 * MCRXMLContainer. If the type is null or empty or maxresults
 * is lower 1 an empty list was returned.
 *
 * @param query	                the XQuery string
 * @param maxresults            the maximum of results
 * @param type                  the MCRObject type
 * @return			a result list as MCRXMLContainer
 **/
public final MCRXMLContainer getResultList(String query, String type, 
  int maxresults) 
  {
  // check the parameter
  MCRXMLContainer result = new MCRXMLContainer();
  if ((type == null) || ((type = type.trim()).length() ==0)) {
    return result; }
  if (maxresults < 1) {
    return result; }
  // transform the search string
  StringBuffer cond = new StringBuffer("");
  logger.debug("Incomming query = "+query);
  String rawtext = query.toUpperCase();
  rawtext.replace('\n',' ');
  rawtext.replace('\r',' ');
  logger.debug("Raw text query = "+rawtext);
  int startpos = 0;
  int stoppos = rawtext.length();
  int operpos = -1;
  String onecond = "";
  String oper = "";
  cond.append('(');
  while (startpos<stoppos) {
    onecond = getNextCondition(startpos,stoppos,rawtext);
    logger.debug("Next cond = "+onecond);
    startpos += onecond.length();
    if (onecond.length()>1) { cond.append('('); }
    cond.append(traceOneCondition(onecond.trim()));
    if (onecond.length()>1) { cond.append(')'); }
    if (startpos<stoppos) {
      operpos = rawtext.indexOf(" AND ",startpos);
      if (operpos != -1) {
        startpos = operpos+5;
        oper = rawtext.substring(operpos,startpos);
        cond.append(' ').append(oper).append(' ');
        continue;
        }
      operpos = rawtext.indexOf(" OR ",startpos);
      if (operpos != -1) {
        startpos = operpos+4;
        oper = rawtext.substring(operpos,startpos);
        cond.append(' ').append(oper).append(' ');
        continue;
        }
      }
    }
  logger.debug("First transformation = "+cond.toString());
  if ((cond.length()==3)||(cond.length()==1)) { 
    cond = new StringBuffer("(( $MC=*$ XXXMYCOREOBJECTXXX* )"); }
  cond.append(')');
  logger.debug("Transformed query = "+cond.toString());
  // search
  MCRCM7SearchTS ts = new MCRCM7SearchTS();
  ts.setSearchLang(conf.getString("MCR.persistence_cm7_textsearch_lang"));
  String confp = "MCR.persistence_cm7_"+type.toLowerCase();
  ts.setIndexClass(conf.getString(confp));
  confp = confp + "_ts";
  ts.setIndexTS(conf.getString(confp));
  ts.setMaxResults(maxresults);
  try {
    ts.search(cond.toString()); result = ts.getResult(); }
  catch (Exception e) {
    throw new MCRPersistenceException("The text search error.",e);
    }
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
 * @ return the transfromed query for CM7.
 **/
private final String traceOneCondition(String cond)
  {
  int i, j, k,l ,m, n;
  StringBuffer sb = new StringBuffer(128);
  // search [..]
  int klammerauf = cond.indexOf("[");
  int klammerzu = cond.indexOf("]",klammerauf+1);
  if ((klammerauf==-1)||(klammerzu==-1)) { return ""; }
  // create pretag as value of the path to the data
  if ((klammerauf!=ROOT_TAG_OBJECT.length()) &&
      (klammerauf!=ROOT_TAG_DERIVATE.length())) { return ""; }
  if ((!cond.startsWith(ROOT_TAG_OBJECT)) && 
      (!cond.startsWith(ROOT_TAG_DERIVATE))) { return ""; } 
  String pretag = "XXXMYCOREOBJECT";
  logger.debug("PRETAG="+pretag);
  // search operations
  String tag[] = new String[10];
  String op[] = new String[10];
  String value[] = new String[10];
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
      opstart = cond.indexOf("CONTAINS(",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = "contains";
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) {
      opstart = cond.indexOf("LIKE",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = "contains";
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
    if (tippelzu+5<cond.length()) {
      tagstart = cond.indexOf(" AND ",tippelzu+1);
      if (tagstart==-1) {
        tagstart = cond.indexOf(" OR ",tippelzu+1);
        if (tagstart==-1) { return ""; }
        tagstart +=4; 
        }
      else { tagstart+=5; }
      }
    counter++;
    }

  // prepare values they are MCRObjectId's
  for (i=0;i<counter;i++) {
    try {
      MCRObjectID mid = new MCRObjectID(value[i]);
      if (mid.isValid()) {
        value[i] = mid.getId().toUpperCase().replace('_','X'); }
      }
    catch (MCRException e) { }
    }
  // prepare categid
  for (i=0;i<counter;i++) {
    if (tag[i].endsWith("@CATEGID")) { value[i] = value[i]+"*"; }
    }
  
  for (i=0;i<counter;i++) {
    logger.debug("TAG="+tag[i]);
    logger.debug("OPER="+op[i]);
    logger.debug("VALUE="+value[i]);
    logger.debug("");
    }

  // search and prepare all attributes
  String attr[] = new String[10];
  boolean isattr[] = new boolean[10];
  for (i=0;i<counter;i++) {
    if (tag[i].indexOf("@") != -1) { 
      isattr[i] = true;
      StringBuffer sbtag = new StringBuffer(128);
      sbtag.append(" $CCSID=").append(ccsid).append(",LANG=").append(langid)
        .append(",MC=*$ ").append(pretag);
      j = 0;
      k = tag[i].length();
      while (j < k) {
        l = tag[i].indexOf("/",j);
        if (l == -1) { break; }
        sbtag.append("XXX").append(tag[i].substring(j,l));
        j = l+1;
        }
      sbtag.append("*XXX").append(tag[i].substring(j+1,tag[i].length()))
        .append("XXX");
      if (op[i].equals("contains")) { sbtag.append('*'); }
      for (int ii=0;ii<value[i].length();ii++) {
        if ((value[i].charAt(ii)>='A')&&(value[i].charAt(ii)<='Z')) {
          sbtag.append(value[i].charAt(ii)); continue; }
        if ((value[i].charAt(ii)>='0')&&(value[i].charAt(ii)<='9')) {
          sbtag.append(value[i].charAt(ii)); continue; }
        if (value[i].charAt(ii)=='*') {
          sbtag.append(value[i].charAt(ii)); continue; }
        sbtag.append('X');
        }
      if (op[i].equals("contains")) { sbtag.append('*'); }
      sbtag.append("XXX* "); 
      attr[i] = sbtag.toString();
      }
    else {
      isattr[i] = false;
      StringBuffer sbtag = new StringBuffer(128);
      sbtag.append(pretag);
      j = 0;
      k = tag[i].length();
      while (j < k) {
        l = tag[i].indexOf("/",j);
        if (l == -1) { break; }
        sbtag.append("XXX").append(tag[i].substring(j,l));
        j = l+1;
        }
      sbtag.append("XXX").append(tag[i].substring(j,tag[i].length()))
        .append("XXX*");
      attr[i] = sbtag.toString();
      }
    }

  StringBuffer sbout = new StringBuffer(1024);
  if (isattr[0]) {
    if (counter > 1) {
      sbout.append("( $PARA$ {");
      for (i=0;i<counter;i++) { sbout.append(attr[i]); }
      sbout.append("} )");
      }
    else {
      sbout.append('(').append(attr[0]).append(')'); }
    return sbout.toString();
    }

  // Check for value as date or number 
  GregorianCalendar date = new GregorianCalendar();
  boolean isdate = false;
  boolean isnumber = false;
  long number = 0;
  // is value[0] a date
  date = MCRUtils.covertDateToGregorianCalendar(value[0]);
  if (date != null) {
    isdate = true;
    if (date.get(Calendar.ERA) == GregorianCalendar.AD) {
      number = (4000+date.get(Calendar.YEAR))*10000 +
                     date.get(Calendar.MONTH)*100 +
                     date.get(Calendar.DAY_OF_MONTH); }
    else {
      number = (4000-date.get(Calendar.YEAR))*10000 +
                     date.get(Calendar.MONTH)*100 +
                     date.get(Calendar.DAY_OF_MONTH); }
    }
  else { isdate = false; }
  // is value[0] a number
  try {
    String test = value[0].replace(',','.');
    double dnumber = (new Double(test)).doubleValue();
    // non numbers after decimal point
    //number = (Math.round(dnumber*10.))/10;
    // 3 numbers after decimal point
    number = (Math.round(dnumber*10000.))/10;
    if (number<0.) { number *= -1.; }
    isnumber = true;
    }
  catch (NumberFormatException e) { isnumber = false; }
    
  // number search
  if ((isnumber)||(isdate)) {
    String stag = "( ";
    String atag = "";
    String etag = " )";
    // prepare attributes
    if (counter > 1) {
      StringBuffer sbatag = new StringBuffer(128);
      for (i=1;i<counter;i++) {
        j = tag[i].indexOf("@");
        if (j == -1) { continue; }
        sbatag.append("XXX").append(tag[i].substring(j+1,tag[i].length()))
          .append("XXX").append(value[i]).append("XXX*");
        }
      atag = sbatag.toString();
      }
    // select operator
    int ioper = 0;
    if (op[0].indexOf("<=")>=0) { ioper = 6; }
    else if (op[0].indexOf(">=")>=0) { ioper = 5; }
      else if (op[0].indexOf(">")>=0) { ioper = 4; }
        else if (op[0].indexOf("<")>=0) { ioper = 3; }
          else if (op[0].indexOf("!=")>=0) { ioper = 2; }
            else if (op[0].indexOf("=")>=0) { ioper = 1; }
              else { return ""; }
    String binstr = Long.toBinaryString(number);
    String binstrmax = Integer.toBinaryString(MAX_NUMBER_STRING_LENGTH);
    int lenstr = binstr.length();
    int lenstrmax = binstrmax.length();
    // do for equal
    if ((ioper==1)||(ioper==5)||(ioper==6)) {
      sbout.append(stag).append("$MC=*$ ").append(attr[0]).append(atag)
        .append("YYY");
      for (i=0;i<(lenstrmax-lenstr);i++) { sbout.append('0'); }
        sbout.append(binstr);
      sbout.append(etag); }
    if ((ioper==5)||(ioper==6)) { sbout.append(" OR "); }
    // do for not equal
    if (ioper==2) {
      for (j=0;j<(lenstrmax-lenstr);j++) {
        sbout.append(stag).append("$SC=?,MC=*$ ").append(attr[0])
          .append(atag).append("YYY");
        for (k=0;k<j;k++) { sbout.append('?'); }
        sbout.append('1');
        for (k=j+1;k<lenstrmax;k++) { sbout.append('?'); }
        sbout.append(" ").append(etag);
        sbout.append(" OR ");
        }
      for (j=0;j<lenstr;j++) {
        sbout.append(stag).append("$SC=?,MC=*$ ").append(attr[0])
          .append(atag).append("YYY");
        for (i=0;i<(lenstrmax-lenstr);i++) { sbout.append('?'); }
        for (k=0;k<lenstr;k++) {
          if (k==j) {
            if (binstr.charAt(k)=='0') {
              sbout.append('1'); }
            else {
              sbout.append('0'); }
            }
          else {
            sbout.append('?'); }
          }
        sbout.append(" ").append(etag);
        if (j!=lenstr-1) { sbout.append(" OR "); }
        }
      }
    // do for lower
    if ((ioper==4)||(ioper==5)) {
      StringBuffer sbetag = new StringBuffer(32);
      for (k=0;k<lenstr;k++) { sbetag.append('?'); }
      for (i=0;i<(lenstrmax-lenstr);i++) {
        sbout.append(stag).append("$SC=?,MC=*$ ").append(attr[0])
          .append(atag).append("YYY");
        for (j=0;j<(lenstrmax-lenstr);j++) {
          if (i==j) { sbout.append('1'); }
          else { sbout.append('?'); }
          }
        sbout.append(sbetag.toString()).append(etag);
        if (i!=lenstrmax-lenstr-1) { sbout.append(" OR "); }
        }
      sbetag = new StringBuffer(32);
      for (i=0;i<(lenstrmax-lenstr);i++) { sbetag.append('?'); }
      if (binstr.indexOf("0")!=-1) { sbout.append(" OR "); }
      for (j=0;j<lenstr;j++) {
        if (binstr.charAt(j)=='0') {
          sbout.append(stag).append("$SC=?,MC=*$ ").append(attr[0])
            .append(atag).append("YYY").append(sbetag.toString());
          for (k=0;k<lenstr;k++) {
            if (k<j) {
              sbout.append(binstr.charAt(k)); }
            else {
              if (k==j) { sbout.append('1'); }
              else { sbout.append('?'); }
              }
            }
          sbout.append(etag);
          if (binstr.indexOf("0",j+1)!=-1) { sbout.append(" OR "); }
          }
        }
      }
    // do for greater
    if (((ioper==3)||(ioper==6))&&(number!=0.)) {
      StringBuffer sbetag = new StringBuffer(32);
      for (k=0;k<(lenstrmax-lenstr);k++) { sbetag.append('0'); }
      sbout.append(stag).append("$SC=?,MC=*$ ").append(attr[0])
        .append(atag).append("YYY").append(sbetag.toString()).append('0');
      for (k=0;k<lenstr-1;k++) { sbout.append('?'); }
      sbout.append(etag);
      if (number > 1) {
        sbout.append(" OR ");
        for (j=1;j<lenstr;j++) {
          if (binstr.charAt(j)=='0') { continue; }
          sbout.append(stag).append("$SC=?,MC=*$ ").append(attr[0])
            .append(atag).append("YYY").append(sbetag.toString()).append('1');
          for (k=1;k<lenstr;k++) {
            if (k<j) {
              sbout.append(binstr.charAt(k)); }
            else {
              if (k==j) { sbout.append('0'); }
              else { sbout.append('?'); }
              }
            }
          sbout.append(etag);
          if (binstr.indexOf("1",j+1)!=-1) { sbout.append(" OR "); }
          }
        }
      }
    return sbout.toString();
    }

  // Text values
  ArrayList list = new ArrayList();
  j = 0;
  k = value[0].length();
  while (j < k) {
    l = value[0].indexOf(" ",j);
    if (l == -1) { list.add(value[0].substring(j,k)); break; }
    list.add(value[0].substring(j,l));
    j = l+1;
    }

  sbout.append("( $PARA$ { $MC=*$ ").append(attr[0]);
  for (i=0;i<list.size();i++) {
    sbout.append(" $CCSID=").append(ccsid).append(",LANG=").append(langid);
    if ((op[0].equals("contains")) || 
      (((String)list.get(i)).indexOf('*') != -1)) { 
      sbout.append(",MC=*$ *").append((String)list.get(i)).append("* "); }
    else {
      sbout.append("$ ").append((String)list.get(i)); }
    }
  for (i=1;i<counter;i++) { sbout.append(attr[i]); }
  sbout.append(" } )");

  return sbout.toString();
  }

}

