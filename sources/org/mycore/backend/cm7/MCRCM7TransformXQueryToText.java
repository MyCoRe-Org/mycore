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
// 31 Bit
protected static int MAX_DATE_STRING_LENGTH = 1024 * 1024 * 1024 * 2;

/**
 * The constructor.
 **/
public MCRCM7TransformXQueryToText()
  {}

/**
 * This method parse the XQuery string and return a vector of 
 * MCRObjectId's as strings. If the type is null or empty or maxresults
 * is lower 1 a MCRException was throwed.
 *
 * @param query	                the XQuery string
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
  System.out.println("MCRCM7TransformXQueryToText : "+query);
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
//    System.out.println("Next cond :"+onecond);
    startpos += onecond.length();
    cond.append(traceOneCondition(onecond));
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
  System.out.println("MCRCM7TransformXQueryToText : "+cond.toString());
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
 * This is a private routine they trace one condition.
 *
 * @param onecond  one single condition
 * @ return the transfromed query for CM7.
 **/
private final String traceOneCondition(String cond)
  {
  StringBuffer sb = new StringBuffer(128);
//  System.out.println("ONECOND="+cond);
  int klammerauf = cond.indexOf("[");
  int klammerzu = cond.indexOf("]",klammerauf+1);
  if ((klammerauf==-1)||(klammerzu==-1)) { return ""; }
  int i = 1;
  int j = 0;
  StringBuffer pt = new StringBuffer(128);
  while (j!=-1) {
    j=cond.indexOf("/",i);
    if (j!=-1) { 
      pt.append("XXX").append(cond.substring(i,j)); 
      i = j+1;
      }
    }
  pt.append("XXX").append(cond.substring(i,klammerauf));
  String pretag = pt.toString();
  String tag[] = new String[10];
  String op[] = new String[10];
  String value[] = new String[10];
  int counter = 0;
  int tippelauf = 0;
  int tippelzu = 0;
  int tagstart = klammerauf+1;
  int tagstop = 0;
  int opstart = 0;
  int opstop = 0;
  while ((tippelauf!=-1)&&(tippelzu!=-1)) {
    tippelauf = cond.indexOf("\"",tippelzu+1);
    if (tippelauf==-1) { break; }
    tippelzu = cond.indexOf("\"",tippelauf+1);
    if (tippelzu==-1) { break; }
    value[counter] = new String(cond.substring(tippelauf+1,tippelzu).trim());
    boolean opset = false;
    if (!opset) {
      opstart = cond.indexOf("!=",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = cond.substring(opstart,opstart+2);
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) {
      opstart = cond.indexOf(">=",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = cond.substring(opstart,opstart+2);
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) {
      opstart = cond.indexOf("<=",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = cond.substring(opstart,opstart+2);
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) {
      opstart = cond.indexOf("=",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = cond.substring(opstart,opstart+1);
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) {
      opstart = cond.indexOf("<",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = cond.substring(opstart,opstart+1);
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) {
      opstart = cond.indexOf(">",tagstart);
      if ((opstart != -1)&&(opstart<tippelauf)) {
        op[counter] = cond.substring(opstart,opstart+1);
        tag[counter] = cond.substring(tagstart,opstart).trim();
        opset = true; }
      }
    if (!opset) { return ""; }
    counter++;
    if (tippelzu+5<cond.length()) {
      tagstart = cond.indexOf(" AND ",tippelzu+1);
      if (tagstart==-1) {
        tagstart = cond.indexOf(" OR ",tippelzu+1);
        if (tagstart==-1) { return ""; }
        tagstart +=4;
        }
      else { tagstart+=5; }
      }
    }
/*
  System.out.println("PRETAG="+pretag);
  for (i=0;i<counter;i++) {
    System.out.println("VALUE="+value[i]);
    System.out.println("OPER="+op[i]);
    System.out.println("TAG="+tag[i]);
    }
*/
  // Check for value as date
  GregorianCalendar date = new GregorianCalendar();
  boolean isdate = false;
  try {
    DateFormat df = MCRUtils.getDateFormat("de");
    date.setTime(df.parse(value[0]));
    isdate = true;
    }
  catch (ParseException e) {
    isdate = false; }
  // date search
  if (isdate) {
    int idate = date.get(Calendar.YEAR)*10000 +
                date.get(Calendar.MONTH)*100 +
                date.get(Calendar.DAY_OF_MONTH);
    int ioper = 0;
    if (op[0].indexOf("<=")>=0) { idate += 1; ioper = 3; }
    else if (op[0].indexOf(">=")>=0) { ioper = 2; }
      else if (op[0].indexOf(">")>=0) { idate += 1; ioper = 2; }
        else if (op[0].indexOf("<")>=0) { ioper = 3; }
          else if (op[0].indexOf("!=")>=0) { ioper = 4; }
            else if (op[0].indexOf("=")>=0) { ioper = 1; }
              else { return ""; }
    String binstr = Integer.toBinaryString(idate);
    String binstrmax = Integer.toBinaryString(MAX_DATE_STRING_LENGTH);
    int lenstr = binstr.length();
    int lenstrmax = binstrmax.length();
    StringBuffer sbdate = new StringBuffer(32);
    for (int k=0;k<(lenstrmax-lenstr);k++) { sbdate.append('2'); }
    sbdate.append(binstr);
    String stdate = sbdate.toString();
    StringBuffer sbstag = new StringBuffer(64);
    sbstag.append(pretag).append("XXX").append(tag[0]).append("XXX")
       .append(tag[1].substring(1,tag[1].length())).append("XXX")
       .append(value[1]).append("XXX");
    String stag = sbstag.toString();
    if ((ioper==1) || (ioper==4)) {
      stdate = stdate.replace('2','0');
      if (ioper==4) { 
        sb.append("(NOT "); }
      else {
        sb.append("("); }
      sb.append(stag).append(stdate).append(")").append(NL);
      return sb.toString(); 
      }
    String standor = "";
    String stnot = "(";
    if (ioper==3) { stnot = "(NOT "; }
    stdate = stdate.replace('2','?');
    int k=stdate.indexOf("0");
    while(k<stdate.length())
      {
      sb.append(standor).append(stnot).append("$SC=?$ ").append(stag);
      StringBuffer sbtemp = new StringBuffer(32);
      sbtemp.append(stdate.substring(0,k)).append('1');
      for (int l=k+1;l<stdate.length();l++) { sbtemp.append('?'); }
      sb.append(sbtemp.toString()).append(")");
      if (ioper==2) { standor = " OR "; } else { standor = " AND "; }
      k=stdate.indexOf("0",k+1);
      if (k==-1) break;
      }
    return sb.toString();
    }
  // check value of MCRObjectID
  MCRObjectID mid = new MCRObjectID(value[0]);
  if (mid.isValid()) {
    value[0] = mid.getId().replace('_','X'); }
  // text search
  sb.append("($PARA$ {");
  for (i=0;i<counter;i++) {
    if (tag[i].charAt(0)=='@') {
      sb.append(" XXX").append(tag[i].substring(1,tag[i].length()))
        .append("XXX").append(value[i]).append("XXX");
      continue;
      }
    int valuestart = 0;
    int valuestop = 0;
    String word = "";
    while (valuestop!=-1) {
      valuestop = value[i].indexOf(" ",valuestart+1);
      if (valuestop!=-1) {
        word = value[i].substring(valuestart,valuestop); }
      else {
        word = value[i].substring(valuestart,value[i].length()); }
      if (word.indexOf("*")==-1) {
        sb.append(word).append(' '); }
      else {
        sb.append(" $MC=*$ ").append(word).append(' '); }
      valuestart = valuestop+1;
      }
    sb.append(" $MC=*$ *").append(pretag).append("XXX");
    if (tag[i].charAt(0)!='.') { 
      sb.append(tag[i]).append("XXX*"); }
    else {
      sb.append('*'); }
    }
  sb.append(" })"); 
  return sb.toString();
  }

}

