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
import mycore.datamodel.MCRQueryResultArray;

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

// 32 Bit
protected static int MAX_DATE_STRING_LENGTH = 1024 * 1024 * 1024 * 2;
protected static int MAX_NUMBER_STRING_LENGTH = 1024 * 1024 * 1024 * 2;

/**
 * The constructor.
 **/
public MCRCM7TransformXQueryToText()
  {}

/**
 * This method parse the XQuery string and return the result as
 * MCRQueryResultArray. If the type is null or empty or maxresults
 * is lower 1 an empty list was returned.
 *
 * @param query	                the XQuery string
 * @param maxresults            the maximum of results
 * @param type                  the MCRObject type
 * @return			a result list as MCRQueryResultArray
 **/
public final MCRQueryResultArray getResultList(String query, String type, 
  int maxresults) 
  {
  // check the parameter
  MCRQueryResultArray result = new MCRQueryResultArray();
  if ((type == null) || ((type = type.trim()).length() ==0)) {
    return result; }
  if (maxresults < 1) {
    return result; }
  if (query.equals("\'\'")) { query = ""; }
  // transform the search string
  StringBuffer cond = new StringBuffer("");
System.out.println("================================");
System.out.println("MCRCM7TransformXQueryToText : "+query);
System.out.println("================================");
  String rawtext = query.toUpperCase();
  rawtext.replace('\n',' ');
  rawtext.replace('\r',' ');
System.out.println("Raw text :"+rawtext);
  int startpos = 0;
  int stoppos = rawtext.length();
  int operpos = -1;
  String onecond = "";
  String oper = "";
  cond.append('(');
  while (startpos<stoppos) {
    onecond = getNextCondition(startpos,stoppos,rawtext);
//System.out.println("Next cond :"+onecond);
    startpos += onecond.length();
    if (onecond.length()>1) { cond.append('('); }
    cond.append(traceOneCondition("("+onecond.trim()+")"));
    if (onecond.length()>1) { cond.append(')'); }
    if (startpos<stoppos) {
      operpos = rawtext.indexOf("AND",startpos);
      if (operpos != -1) {
        startpos = operpos+3;
        oper = rawtext.substring(operpos,startpos);
        cond.append(' ').append(oper).append(' ');
        continue;
        }
      operpos = rawtext.indexOf("OR",startpos);
      if (operpos != -1) {
        startpos = operpos+2;
        oper = rawtext.substring(operpos,startpos);
        cond.append(' ').append(oper).append(' ');
        continue;
        }
      }
    }
  if (cond.length()==1) { cond.append("( $MC=*$ XXXMYCOREOBJECTXXX* )"); }
  cond.append(')');
System.out.println("MCRCM7TransformXQueryToText : "+cond.toString());
System.out.println("================================");
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
    System.out.println(e.getMessage());
    e.printStackTrace();
    throw new MCRPersistenceException("The text search error.");
    }
//System.out.println("================================");
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
  int i, j, k;
  StringBuffer sb = new StringBuffer(128);
  // search [..]
  int klammerauf = cond.indexOf("[");
  int klammerzu = cond.indexOf("]",klammerauf+1);
  if ((klammerauf==-1)||(klammerzu==-1)) { return ""; }
  // cerate path to the data
  StringBuffer pt = new StringBuffer(128);
  boolean ispath = false;
  String inpath = cond.substring(0,klammerauf);
  if ((inpath.equals("(")) || (inpath.equals("(.")) ||
      (inpath.equals("(*")) || (inpath.equals("(//*")) ||
      (inpath.equals("(/"))) { 
    pt.append("*"); ispath = true; }
  if (!ispath) {
    i = 1;
    if (inpath.substring(0,2).equals("(/")) { i = 2; }
    if (inpath.substring(0,3).equals("(//")) { i++; }
    if (!inpath.substring(0,2).equals("(/")) { pt.append('*'); }
    if (inpath.substring(0,3).equals("(./")) { i = 3; }
    j = 0;
    while ((j!=-1)&&(j<klammerauf)) {
      j=cond.indexOf("/",i);
      if (j!=-1) { 
        if (i != j) { pt.append("XXX").append(cond.substring(i,j)); }
        i = j+1; 
        }
      }
    pt.append("XXX").append(cond.substring(i,klammerauf));
    }
  String pretag = pt.toString();
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
    System.out.println();
    }
*/

  // Check for value as date or number or  MCRObjectID
  GregorianCalendar date = new GregorianCalendar();
  boolean isdate = false;
  boolean isnumber = false;
  boolean ismcrid = false;
  boolean isat = false;
  long number = 0;
  // is value 0 a attribute
  if (tag[0].substring(0,1).equals("@")) { isat = true; }
  // is value 0 a date
  try {
    DateFormat df = MCRUtils.getDateFormat("de");
    date.setTime(df.parse(value[0]));
    isdate = true;
    number = (long)(date.get(Calendar.YEAR)*10000 +
      date.get(Calendar.MONTH)*100 + date.get(Calendar.DAY_OF_MONTH));
    }
  catch (ParseException e) { isdate = false; }
  // is value 0 a number
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
  // is value 0 a MCRObjectId
  try {
    MCRObjectID mid = new MCRObjectID(value[0]);
    if (mid.isValid()) {
      value[0] = mid.getId().replace('_','X'); }
    ismcrid = true;
    }
  catch (MCRException e) { ismcrid = false; }
  // set the path and attribute tags
  StringBuffer sbatag = new StringBuffer(128);
  if ((!tag[0].equals(".")) && (!tag[0].equals("*"))) {
    if (!isat) {
      sbatag.append(pretag).append("XXX").append(tag[0]).append("XXX*"); }
    else {
      sbatag.append(pretag).append("XXX*XXX")
        .append(tag[0].substring(1,tag[0].length())).append("XXX");
      for (int ii=0;ii<value[0].length();ii++) {
        if ((value[0].charAt(ii)>='A')&&(value[0].charAt(ii)<='Z')) {
          sbatag.append(value[0].charAt(ii)); continue; }
        if ((value[0].charAt(ii)>='0')&&(value[0].charAt(ii)<='9')) {
          sbatag.append(value[0].charAt(ii)); continue; }
        if (value[0].charAt(ii)=='*') {
          sbatag.append(value[0].charAt(ii)); continue; }
        sbatag.append('X');    
        }
      sbatag.append("XXX"); }
    }
  else { sbatag.append(""); }
  for (i=1;i<counter;i++) {
    sbatag.append("XXX").append(tag[i].substring(1,tag[i].length()))
          .append("XXX").append(value[i]).append("XXX*"); 
    }
  String atttag = sbatag.toString();
  // number search
  if ((isnumber)||(isdate)) {
    String stag = "( ";
    String etag = " )";
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
    if ((ioper==1)||(ioper==5)||(ioper==6)) {
      sb.append(stag).append("$MC=*$ ").append(atttag);
      for (i=0;i<(lenstrmax-lenstr);i++) { sb.append('0'); }
        sb.append(binstr);
      sb.append(etag); }
    if ((ioper==5)||(ioper==6)) { sb.append(" OR "); }
    if (ioper==2) {
      for (j=0;j<(lenstrmax-lenstr);j++) {
        sb.append(stag).append("$SC=?,MC=*$ ").append(atttag);
        for (k=0;k<j;k++) { sb.append('?'); }
        sb.append('1');
        for (k=j+1;k<lenstrmax;k++) { sb.append('?'); }
        sb.append(" ").append(etag); 
        sb.append(" OR "); 
        }
      for (j=0;j<lenstr;j++) {
        sb.append(stag).append("$SC=?,MC=*$ ").append(atttag);
        for (i=0;i<(lenstrmax-lenstr);i++) { sb.append('0'); }
        for (k=0;k<lenstr;k++) {
          if (k==j) {
            if (binstr.charAt(k)=='0') {
              sb.append('1'); }
            else {
              sb.append('0'); }
            }
          else {
            sb.append('?'); }
          }
        sb.append(" ").append(etag); 
        if (j!=lenstr-1) { sb.append(" OR "); }
        }
      }
    if ((ioper==4)||(ioper==5)) { 
      StringBuffer sbetag = new StringBuffer(32);
      for (k=0;k<lenstr;k++) { sbetag.append('?'); }
      for (i=0;i<(lenstrmax-lenstr);i++) {
        sb.append(stag).append("$SC=?,MC=*$ ").append(atttag);
        for (j=0;j<(lenstrmax-lenstr);j++) {
          if (i==j) { sb.append('1'); }
          else { sb.append('?'); }
          }
        sb.append(sbetag.toString()).append(etag);
        if (i!=lenstrmax-lenstr-1) { sb.append(" OR "); }
        }
      sbetag = new StringBuffer(32);
      for (i=0;i<(lenstrmax-lenstr);i++) { sbetag.append('?'); }
      if (binstr.indexOf("0")!=-1) { sb.append(" OR "); }
      for (j=0;j<lenstr;j++) {
        if (binstr.charAt(j)=='0') {
          sb.append(stag).append("$SC=?,MC=*$ ").append(atttag)
            .append(sbetag.toString());
          for (k=0;k<lenstr;k++) {
            if (k<j) { 
              sb.append(binstr.charAt(k)); }
            else {
              if (k==j) { sb.append('1'); }
              else { sb.append('?'); }
              }
            }
          sb.append(etag);
          if (binstr.indexOf("0",j+1)!=-1) { sb.append(" OR "); }
          }
        }
      }
    if (((ioper==3)||(ioper==6))&&(number!=0.)) { 
      StringBuffer sbetag = new StringBuffer(32);
      for (k=0;k<(lenstrmax-lenstr);k++) { sbetag.append('0'); }
      sb.append(stag).append("$SC=?,MC=*$ ").append(atttag)
        .append(sbetag.toString()).append('0');
      for (k=0;k<lenstr-1;k++) { sb.append('?'); }
      sb.append(etag);
      if (number > 1) {
        sb.append(" OR ");  
        for (j=1;j<lenstr;j++) {
          if (binstr.charAt(j)=='0') { continue; }
          sb.append(stag).append("$SC=?,MC=*$ ").append(atttag)
            .append(sbetag.toString()).append('1');
          for (k=1;k<lenstr;k++) {
            if (k<j) { 
              sb.append(binstr.charAt(k)); }
            else {
              if (k==j) { sb.append('0'); }
              else { sb.append('?'); }
              }
            }
          sb.append(etag);
          if (binstr.indexOf("1",j+1)!=-1) { sb.append(" OR "); }
          }
        }
      }
    return sb.toString();
    }
  // attribute query
  if (isat) {
    sb.append("( $MC=*$ ").append(atttag).append(" )"); 
    return sb.toString();
    }
  // freetext
  if (atttag.length()==0) {
    if (value[0].indexOf("*")!=-1) {
      sb.append("( $MC=*$ ").append(value[0]).append(" )"); }
    else {
      sb.append("( ").append(value[0]).append(" )"); }
    return sb.toString();
    }
  // text in tag
  if (atttag.indexOf("*")!=-1) {
    sb.append("($PARA$ { $MC=*$ ").append(atttag).append(" "); }
  else {
    sb.append("($PARA$ { ").append(atttag).append(" "); }
  if (value[0].indexOf("*")!=-1) {
    sb.append(" $MC=*$ ").append(value[0]).append(" } )"); }
  else {
    sb.append(value[0]).append(" } )"); }
  return sb.toString();
  }

}

