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
import mycore.common.MCRConfiguration;
import mycore.common.MCRException;
import mycore.common.MCRPersistenceException;
import mycore.datamodel.MCRQueryInterface;

/**
 * This is the tranformer implementation for CM 7 from  Miless Query language
 * to the CM Text Search Engine.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRCM7TransformMilessToText implements MCRQueryInterface
{
// common data
protected static String NL =
  new String((System.getProperties()).getProperty("line.separator"));

/**
 * The constructor.
 **/
public MCRCM7TransformMilessToText()
  {}

/**
 * This method parse the Miless query string and return a vector of 
 * MCRObjectId's as strings.
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
  if ((query == null) || ((query = query.trim()).length() ==0)) {
    throw new MCRPersistenceException("The query is empty."); }
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
  while (startpos<stoppos) {
    onecond = getNextCondition(startpos,stoppos,rawtext);
    startpos += onecond.length();
    if (onecond.indexOf("CONTAINS") != -1) {
      cond.append(setContainsCondition(onecond)); }
    if (startpos<stoppos) {
      operpos = rawtext.indexOf("(",startpos);
      if (operpos != -1) {
        oper = rawtext.substring(startpos,operpos-1).trim();
        cond.append(' ').append(oper).append(' ');
        startpos = operpos;
        }
      }
    }
  if (cond.length()==0) { cond.append("(\"*\")"); }
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
    throw new MCRPersistenceException("The text search error.");
    }
  System.out.println("================================");
  return result;
  }

/**
 * This private methode get the next condition of the query string.
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
 * This private methode set the part of the CM7 search string for the
 * 'CONTAINS' condition.
 *
 * @param condition   a single condition
 * @return the CM7 query substring
 **/
private final String setContainsCondition(String condition)
  {
  StringBuffer sb = new StringBuffer(128);
  int i = condition.indexOf("CONTAINS");
  String value = condition.substring(i+8,condition.length()-1);
  value = value.replace('"',' ').trim();
  String tag = condition.substring(1,i).trim();
  sb.append("($PARA$ { ").append(value).append(" XXX").append(tag)
    .append("XXX })");
  return sb.toString();
  }

/**
 * The methode returns the search string for a XML text field for
 * the IBM Content Manager 7 persistence system.a<p>
 * A full XML tag element shows like<br>
 * &lt;subtag sattrib="svalue"&gt;<br>
 * &lt;innertag iattrib="ivalue"&gt;<br>
 * text<br>
 * &lt;/innertag&gt;<br>
 * &lt;/subtag&gt;
 *
 * @param subtag             the tagname of an element from the list in a tag
 * @param sattrib            the optional attribute vector of a subtag
 * @param svalue             the optional value vector of sattrib
 * @param innertag           the optional inner tag of a subtag element
 * @param iattrib            the optional attribute vector of a innertag
 * @param ivalue             the optional value vector of iattrib
 * @param text               the text value of this element
 * @return the search string for the CM7 text search engine
 **/
public final String createSearchStringText(String subtag, String [] sattrib,
  String [] svalue, String innertag, String [] iattrib, String [] ivalue,
  String text)
  {
  if ((subtag == null) || ((subtag = subtag.trim()).length() ==0)) {
    return ""; }
  StringBuffer sb = new StringBuffer(1024);
  sb.append("XXX").append(subtag.toUpperCase()).append("XXX");
  if ((innertag != null) && ((innertag = innertag.trim()).length() !=0)) {
    sb.append(innertag.toUpperCase()).append("XXX"); }
  sb.append(' ');
  if (sattrib != null) {
    for (int i=0;i<sattrib.length;i++) {
      sb.append("XXX").append(sattrib[i].toUpperCase()).append("XXX")
        .append(svalue[i].toUpperCase()).append("XXX ");
      }
    }
  if (iattrib != null) {
    for (int i=0;i<iattrib.length;i++) {
      sb.append("XXX").append(iattrib[i].toUpperCase()).append("XXX")
        .append(ivalue[i].toUpperCase()).append("XXX ");
      }
    }
  if ((text != null) && ((text = text.trim()).length() !=0)) {
    sb.append(text.replace('\n',' ').replace('\r',' ').toUpperCase()); }
  sb.append(NL);
  return sb.toString();
  }

}

