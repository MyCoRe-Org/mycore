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

package mycore.cm7;

import java.util.*;
import java.text.*;
import mycore.common.MCRConfiguration;
import mycore.common.MCRException;
import mycore.common.MCRPersistenceException;
import mycore.common.MCRUtils;
import mycore.datamodel.MCRQueryInterface;
import mycore.datamodel.MCRObjectID;

/**
 * This is the tranformer implementation for CM 7 from  Miless Query language
 * to the CM Text Search Engine.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRCM7TransformMilessToText extends MCRCM7TransformToText 
implements MCRQueryInterface
{
// common data
protected static String NL =
  new String((System.getProperties()).getProperty("line.separator"));
// 31 Bit
protected static int MAX_DATE_STRING_LENGTH = 1024 * 1024 * 1024 * 2;

/**
 * The constructor.
 **/
public MCRCM7TransformMilessToText()
  {}

/**
 * This method parse the Miless query string and return a vector of 
 * MCRObjectId's as strings. If the type is null or empty or maxresults
 * is lower 1 a MCRException was throwed.
 *
 * @param query	                the Miless query string
 * @param maxresults            the maximum of results
 * @param type                  the MCRObject type
 * @exception MCRException      general Exception of MyCoRe
 * @return			list of MCRObject's as a vector of XML strings
 **/
public final Vector getResultList(String query, String type, int maxresults) 
  throws MCRPersistenceException
  {
  // check the parameter
  if ((type == null) || ((type = type.trim()).length() ==0)) {
    throw new MCRPersistenceException("The type is empty."); }
  if (maxresults < 1) {
    throw new MCRPersistenceException("The maxresults is lower then one."); }
  Vector result = new Vector(maxresults);
  // transform the search string
  StringBuffer cond = new StringBuffer("");
  System.out.println("================================");
  System.out.println("MCRCM7TransformMilessToText : "+query);
  System.out.println("================================");
  String rawtext = query.toUpperCase();
  rawtext.replace('\n',' ');
  rawtext.replace('\r',' ');
  int startpos = 0;
  int stoppos = rawtext.length();
  int operpos = -1;
  String onecond = "";
  String oper = "";
  cond.append('(');
  while (startpos<stoppos) {
    onecond = getNextCondition(startpos,stoppos,rawtext);
    System.out.println("Next cond :"+onecond);
    startpos += onecond.length();
    if (onecond.indexOf("CONTAINS") != -1) {
      cond.append(setContainsCondition(onecond)); }
    if (onecond.indexOf("DD.MM.YYYY") != -1) {
      cond.append(setDateCondition(onecond)); }
    if (onecond.indexOf("LINKLABEL") != -1) {
      cond.append(setLinkLabelCondition(onecond)); }
    if (onecond.indexOf("LINKID") != -1) {
      cond.append(setLinkIdCondition(onecond)); }
    if (startpos<stoppos) {
      operpos = rawtext.indexOf("(",startpos);
      if (operpos != -1) {
        oper = rawtext.substring(startpos,operpos-1).trim();
        cond.append(' ').append(oper).append(' ');
        startpos = operpos;
        }
      }
    }
  if (cond.length()==1) { cond.append("(XXXOBJECTXXXIDXXX)"); }
  cond.append(')');
  System.out.println("MCRCM7TransformMilessToText : "+cond.toString());
  System.out.println("================================");
  // search
  MCRCM7SearchTS ts = new MCRCM7SearchTS();
  ts.setSearchLang(MCRConfiguration.instance()
    .getString("MCR.persistence_cm7_textsearch_lang"));
  String conf = "MCR.persistence_cm7_"+type.toLowerCase();
  ts.setIndexClass(MCRConfiguration.instance().getString(conf));
  conf = conf + "_ts";
  ts.setIndexTS(MCRConfiguration.instance().getString(conf));
  ts.setMaxResults(maxresults);
  try {
    ts.search(cond.toString()); result = ts.getResultVector(); }
  catch (Exception e) {
    System.out.println(e.getMessage());
    e.printStackTrace();
    throw new MCRPersistenceException("The text search error.");
    }
  System.out.println("================================");
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
    if (query.charAt(i)=='(') { numopen++; }
    if (query.charAt(i)==')') { numclose++; }
    if (numopen==numclose) { break; }
    }
  return sb.toString();
  }

/**
 * This private method set the part of the CM7 search string for the
 * 'CONTAINS' condition.
 *
 * @param condition   a single condition
 * @return the CM7 query substring
 **/
private final String setContainsCondition(String condition)
  {
  StringBuffer sb = new StringBuffer(128);
  int i = condition.indexOf("CONTAINS");
  if (i==-1) { return ""; }
  String tag = condition.substring(1,i).trim();
  int start = condition.indexOf("\"",i);
  if (start==-1) { return ""; }
  int stop = condition.indexOf("\"",start+1);
  if (stop==-1) { return ""; }
  String value = condition.substring(start+1,stop);
  int l = stop;
  value = value.replace('"',' ').trim();
  sb.append("($PARA$ {"); 
  int j = 0;
  String word = "";
  while (j<value.length()) {
    int k = value.indexOf(" ",j);
    if (k==-1) { k= value.length(); }
    word = value.substring(j,k); 
    if (word.indexOf("*")!=-1) {
      sb.append("$MC=*$ ").append(word).append(' '); }
    else {
      sb.append(word).append(' '); }
    j = k+1;
    }
  while(true) {
    i = condition.indexOf("WITH",l);
    if (i==-1) { break; }
    start = condition.indexOf("\"",i+4);
    if (start==-1) { return ""; }
    stop = condition.indexOf("\"",start+1);
    if (stop==-1) { return ""; }
    int k = condition.indexOf("=",start+1);
    sb.append("XXX").append(condition.substring(start+1,k)).append("XXX")
      .append(condition.substring(k+1,stop)).append("XXX ");
    l = stop;
    }
  i = tag.indexOf(".");
  if (i==-1) {
    sb.append("$MC=*$ *XXX").append(tag).append("XXX* }"); }
  else {
    i = -1;
    sb.append("$MC=*$ *XXX");
    j = tag.indexOf(".",i+1);
    while (j!=-1) {
      sb.append(tag.substring(i+1,j)).append("XXX");
      i = j;
      j = tag.indexOf(".",i+1);
      }
    sb.append(tag.substring(i+1,tag.length())).append("XXX* }");
    }
  sb.append(')');
  return sb.toString();
  }

/**
 * This private method set the part of the CM7 search string for the
 * 'LINKLABEL' condition.
 *
 * @param condition   a single condition
 * @return the CM7 query substring
 **/
private final String setLinkLabelCondition(String condition)
  {
  StringBuffer sb = new StringBuffer(128);
  int i = condition.indexOf("LINKLABEL");
  if (i==-1) { return ""; }
  String tag = condition.substring(1,i).trim();
  int start = condition.indexOf("\"",i);
  if (start==-1) { return ""; }
  int stop = condition.indexOf("\"",start+1);
  if (stop==-1) { return ""; }
  String value = condition.substring(start+1,stop);
  int l = stop;
  value = value.replace('"',' ').trim();
  sb.append("($PARA$ {"); 
  int j = 0;
  String word = "";
  while (j<value.length()) {
    int k = value.indexOf(" ",j);
    if (k==-1) { k= value.length(); }
    word = value.substring(j,k); 
    if (word.indexOf("*")!=-1) {
      sb.append("$MC=*$ ").append(word).append(' '); }
    else {
      sb.append(word).append(' '); }
    j = k+1;
    }
  while(true) {
    i = condition.indexOf("WITH",l);
    if (i==-1) { break; }
    start = condition.indexOf("\"",i+4);
    if (start==-1) { return ""; }
    stop = condition.indexOf("\"",start+1);
    if (stop==-1) { return ""; }
    int k = condition.indexOf("=",start+1);
    sb.append("XXX").append(condition.substring(start+1,k)).append("XXX")
      .append(condition.substring(k+1,stop)).append("XXX ");
    l = stop;
    }
  i = tag.indexOf(".");
  if (i==-1) {
    sb.append("$MC=*$ *XXX").append(tag).append("XXX* }"); }
  else {
    i = -1;
    sb.append("$MC=*$ *XXX");
    j = tag.indexOf(".",i+1);
    while (j!=-1) {
      sb.append(tag.substring(i+1,j)).append("XXX");
      i = j;
      j = tag.indexOf(".",i+1);
      }
    sb.append(tag.substring(i+1,tag.length())).append("XXX* }");
    }
  sb.append(')');
  return sb.toString();
  }

/**
 * This private method set the part of the CM7 search string for the
 * 'LINKID' condition.
 *
 * @param condition   a single condition
 * @return the CM7 query substring
 **/
private final String setLinkIdCondition(String condition)
  {
  StringBuffer sb = new StringBuffer(128);
  int i = condition.indexOf("LINKID");
  if (i==-1) { return ""; }
  String tag = condition.substring(1,i).trim();
  int start = condition.indexOf("\"",i);
  if (start==-1) { return ""; }
  int stop = condition.indexOf("\"",start+1);
  if (stop==-1) { return ""; }
  String value = condition.substring(start+1,stop);
  int l = stop;
  value = value.replace('"',' ').replace('_','X').trim();
  sb.append("($PARA$ {"); 
  int j = 0;
  String word = "";
  while (j<value.length()) {
    int k = value.indexOf(" ",j);
    if (k==-1) { k= value.length(); }
    word = value.substring(j,k); 
    if (word.indexOf("*")!=-1) {
      sb.append("$MC=*$ ").append(word).append(' '); }
    else {
      sb.append(word).append(' '); }
    j = k+1;
    }
  while(true) {
    i = condition.indexOf("WITH",l);
    if (i==-1) { break; }
    start = condition.indexOf("\"",i+4);
    if (start==-1) { return ""; }
    stop = condition.indexOf("\"",start+1);
    if (stop==-1) { return ""; }
    int k = condition.indexOf("=",start+1);
    sb.append("XXX").append(condition.substring(start+1,k)).append("XXX")
      .append(condition.substring(k+1,stop)).append("XXX ");
    l = stop;
    }
  i = tag.indexOf(".");
  if (i==-1) {
    sb.append("$MC=*$ *XXX").append(tag).append("XXX* }"); }
  else {
    i = -1;
    sb.append("$MC=*$ *XXX");
    j = tag.indexOf(".",i+1);
    while (j!=-1) {
      sb.append(tag.substring(i+1,j)).append("XXX");
      i = j;
      j = tag.indexOf(".",i+1);
      }
    sb.append(tag.substring(i+1,tag.length())).append("XXX* }");
    }
  sb.append(')');
  return sb.toString();
  }

/**
 * This private method set the part of the CM7 search string for the
 * 'DD.MM.YYYY' condition.
 *
 * @param condition   a single condition
 * @return the CM7 query substring
 **/
private final String setDateCondition(String condition)
  {
  StringBuffer sb = new StringBuffer(128);
  // split condition
  int i = condition.indexOf("DD.MM.YYYY");
  if (i==-1) { return ""; }
  String tag = condition.substring(1,i).trim();
  int start = condition.indexOf("\"",i);
  if (start==-1) { return ""; }
  String oper = condition.substring(i+10,start).trim();
  int stop = condition.indexOf("\"",start+1);
  if (stop==-1) { return ""; }
  String value = condition.substring(start+1,stop);
  value = value.replace('"',' ').trim();
  // create date string
  GregorianCalendar date = new GregorianCalendar();
  try {
    DateFormat df = MCRUtils.getDateFormat("de");
    date.setTime(df.parse(value)); }
  catch (ParseException e) {
    return ""; }
  int idate = date.get(Calendar.YEAR)*10000 +
              date.get(Calendar.MONTH)*100 +
              date.get(Calendar.DAY_OF_MONTH);
  int ioper = 0;
  if (oper.indexOf("<=")>=0) { idate += 1; ioper = 3; }
  else if (oper.indexOf(">=")>=0) { ioper = 2; }
    else if (oper.indexOf(">")>=0) { idate += 1; ioper = 2; }
      else if (oper.indexOf("<")>=0) { ioper = 3; }
        else if (oper.indexOf("!=")>=0) { ioper = 4; }
          else if (oper.indexOf("=")>=0) { ioper = 1; }
            else { return ""; }
  String binstr = Integer.toBinaryString(idate);
  String binstrmax = Integer.toBinaryString(MAX_DATE_STRING_LENGTH);
  int lenstr = binstr.length();
  int lenstrmax = binstrmax.length();
  StringBuffer sbdate = new StringBuffer(32);
  for (int k=0;k<(lenstrmax-lenstr);k++) { sbdate.append('2'); }
  sbdate.append(binstr);
  String stdate = sbdate.toString();
  // create the tag string
  StringBuffer sbtag = new StringBuffer(32);
  i = tag.indexOf(".");
  int j;
  if (i==-1) {
    sbtag.append("XXX").append(tag).append("XXX"); }
  else {
    i = -1;
    sbtag.append("XXX");
    j = tag.indexOf(".",i+1);
    while (j!=-1) {
      sbtag.append(tag.substring(i+1,j)).append("XXX");
      i = j;
      j = tag.indexOf(".",i+1);
      }
    sbtag.append(tag.substring(i+1,tag.length())).append("XXX");
    }
  int l = stop;
  while(true) {
    i = condition.indexOf("WITH",l);
    if (i==-1) { break; }
    start = condition.indexOf("\"",i+4);
    if (start==-1) { return ""; }
    stop = condition.indexOf("\"",start+1);
    if (stop==-1) { return ""; }
    int k = condition.indexOf("=",start+1);
    if (k==-1) { break; }
    sbtag.append(condition.substring(start+1,k)).append("XXX")
      .append(condition.substring(k+1,stop)).append("XXX");
    l = stop;
    }
  String sttag = sbtag.toString();
  // build the search string
  if ((ioper==1) || (ioper==4)) {
    stdate = stdate.replace('2','0');
    if (ioper==4) { 
      sb.append("(NOT "); }
    else {
      sb.append("("); }
    sb.append(sttag).append(stdate).append(")").append(NL);
    return sb.toString(); }
  String standor = "";
  String stnot = "(";
  if (ioper==3) { stnot = "(NOT "; }
  stdate = stdate.replace('2','?');
  int k=stdate.indexOf("0");
  while(k<stdate.length())
    {
    sb.append(standor).append(stnot).append("$SC=?$ ").append(sttag);
    StringBuffer sbtemp = new StringBuffer(32);
    sbtemp.append(stdate.substring(0,k)).append('1');
    for (l=k+1;l<stdate.length();l++) { sbtemp.append('?'); }
    sb.append(sbtemp.toString()).append(")");
    if (ioper==2) { standor = " OR "; } else { standor = " AND "; }
    k=stdate.indexOf("0",k+1);
    if (k==-1) break;
    }
  return sb.toString();
  }

}

