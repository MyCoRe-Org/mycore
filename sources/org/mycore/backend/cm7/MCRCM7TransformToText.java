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
 * This is the basic tranformer implementation for CM 7 search text strings
 * to the CM Text Search Engine.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRCM7TransformToText
{
// common data
protected static String NL =
  new String((System.getProperties()).getProperty("line.separator"));
// 31 Bit
protected static int MAX_DATE_STRING_LENGTH = 1024 * 1024 * 1024 * 2;
protected static int MAX_NUMBER_STRING_LENGTH = 1024 * 1024 * 1024 * 1024 * 1024 * 2;

/**
 * The constructor.
 **/
public MCRCM7TransformToText()
  {}

/**
 * The method returns the search string for a XML text field for
 * the IBM Content Manager 7 persistence system.<p>
 * A full XML tag element shows like<br>
 * &lt;subtag sattrib="svalue"&gt;<br>
 * &lt;innertag iattrib="ivalue"&gt;<br>
 * text<br>
 * &lt;/innertag&gt;<br>
 * &lt;/subtag&gt;
 *
 * @param part               the global part of the elements like 'metadata'
 *                           or 'service'
 * @param tag                the tagname of an element list
 * @param subtag             the tagname of an element from the list in a tag
 * @param sattrib            the optional attribute vector of a subtag
 * @param svalue             the optional value vector of sattrib
 * @param innertag           the optional inner tag of a subtag element
 * @param iattrib            the optional attribute vector of a innertag
 * @param ivalue             the optional value vector of iattrib
 * @param text               the text value of this element
 * @return the search string for the CM7 text search engine
 **/
public final String createSearchStringText(String part, String tag,
  String subtag, String [] sattrib, String [] svalue, String innertag, 
  String [] iattrib, String [] ivalue, String text)
  {
  if ((subtag == null) || ((subtag = subtag.trim()).length() ==0)) {
    return ""; }
  StringBuffer sb = new StringBuffer(1024);
  sb.append("XXX").append(part.toUpperCase()).append("XXX");
  if (tag!=null) { sb.append(tag.toUpperCase()).append("XXX"); }
  sb.append(subtag.toUpperCase()).append("XXX");
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

/**
 * The method returns the search string for a XML date field for
 * the IBM Content Manager 7 persistence system.<p>
 * The date was transformed to a bit string with 
 * <em>10000*year+100*month+day</em>.
 * A full XML tag element shows like<br>
 * &lt;subtag sattrib="svalue" ... &gt;<br>
 * date<br>
 * &lt;/subtag&gt;
 *
 * @param part               the global part of the elements like 'metadata'
 *                           or 'service'
 * @param tag                the tagname of an element list
 * @param subtag             the tagname of an element from the list in a tag
 * @param sattrib            the optional attribute vector of a subtag
 * @param svalue             the optional value vector of sattrib
 * @param date               the date value of this element
 * @return the search string for the CM7 text search engine
 **/
public final String createSearchStringDate(String part, String tag,
  String subtag, String [] sattrib, String [] svalue, GregorianCalendar date)
  {
  if ((subtag == null) || ((subtag = subtag.trim()).length() ==0)) {
    return ""; }
  if (date == null) { return ""; }
  StringBuffer sb = new StringBuffer(1024);
  sb.append("XXX").append(part.toUpperCase()).append("XXX")
    .append(tag.toUpperCase()).append("XXX")
    .append(subtag.toUpperCase()).append("XXX");
  if (sattrib != null) {
    for (int i=0;i<sattrib.length;i++) {
      if (sattrib[i].toUpperCase().equals("LANG")) { continue; }
      sb.append(sattrib[i].toUpperCase()).append("XXX")
        .append(svalue[i].toUpperCase()).append("XXX");
      }
    }
  int idate = date.get(Calendar.YEAR)*10000 +
              date.get(Calendar.MONTH)*100 +
              date.get(Calendar.DAY_OF_MONTH);
  String binstr = Integer.toBinaryString(idate);
  String binstrmax = Integer.toBinaryString(MAX_DATE_STRING_LENGTH);
  int lenstr = binstr.length();
  int lenstrmax = binstrmax.length();
  for (int i=0;i<(lenstrmax-lenstr);i++) { sb.append('0'); }
  sb.append(binstr);
  sb.append(NL);
  return sb.toString();
  }

/**
 * The method returns the search string for a XML boolean field for
 * the IBM Content Manager 7 persistence system.<p>
 * The boolean value was transformed to a string.
 * A full XML tag element shows like<br>
 * &lt;subtag sattrib="svalue" ... &gt;<br>
 * bvalue<br>
 * &lt;/subtag&gt;
 *
 * @param part               the global part of the elements like 'metadata'
 *                           or 'service'
 * @param tag                the tagname of an element list
 * @param subtag             the tagname of an element from the list in a tag
 * @param sattrib            the optional attribute vector of a subtag
 * @param svalue             the optional value vector of sattrib
 * @param bvalue             the boolean value of this element
 * @return the search string for the CM7 text search engine
 **/
public final String createSearchStringBoolean(String part, String tag,
  String subtag, String [] sattrib, String [] svalue, boolean bvalue)
  {
  if ((subtag == null) || ((subtag = subtag.trim()).length() ==0)) {
    return ""; }
  StringBuffer sb = new StringBuffer(1024);
  sb.append("XXX").append(part.toUpperCase()).append("XXX")
    .append(tag.toUpperCase()).append("XXX")
    .append(subtag.toUpperCase()).append("XXX");
  if (sattrib != null) {
    for (int i=0;i<sattrib.length;i++) {
      if (sattrib[i].toUpperCase().equals("LANG")) { continue; }
      sb.append(sattrib[i].toUpperCase()).append("XXX")
        .append(svalue[i].toUpperCase()).append("XXX");
      }
    }
  if (bvalue) {
    sb.append("TRUEXXX"); }
  else {
    sb.append("FALSEXXX"); }
  sb.append(NL);
  return sb.toString();
  }

/**
 * The method returns the search string for a XML field with numbers for
 * the IBM Content Manager 7 persistence system.<p>
 * The number was transformed to a string to cut the number of the third decimal
 * position and tronspose it then in a integer number by multiply with 1000.
 * the number must be in range of 10^10 to -10^10.
 * A full XML tag element shows like<br>
 * &lt;subtag sattrib="svalue" ... &gt;<br>
 * number<br>
 * &lt;/subtag&gt;
 *
 * @param part               the global part of the elements like 'metadata'
 *                           or 'service'
 * @param tag                the tagname of an element list
 * @param subtag             the tagname of an element from the list in a tag
 * @param sattrib            the optional attribute vector of a subtag
 * @param svalue             the optional value vector of sattrib
 * @param number             the number value of this element as a double value
 * @return the search string for the CM7 text search engine
 **/
public final String createSearchStringDouble(String part, String tag,
  String subtag, String [] sattrib, String [] svalue, double number)
  {
  if ((subtag == null) || ((subtag = subtag.trim()).length() ==0)) {
    return ""; }
  if ((number < -10.e10) || (number > 10.e10)) { return ""; }
  StringBuffer sb = new StringBuffer(1024);
  sb.append("XXX").append(part.toUpperCase()).append("XXX")
    .append(tag.toUpperCase()).append("XXX")
    .append(subtag.toUpperCase()).append("XXX");
  if (sattrib != null) {
    for (int i=0;i<sattrib.length;i++) {
      if (sattrib[i].toUpperCase().equals("LANG")) { continue; }
      sb.append(sattrib[i].toUpperCase()).append("XXX")
        .append(svalue[i].toUpperCase()).append("XXX");
      }
    }
  long a = Math.round(number*100.); 
  String binstr = Long.toBinaryString(a);
  String binstrmax = Integer.toBinaryString(MAX_NUMBER_STRING_LENGTH);
  int lenstr = binstr.length();
  int lenstrmax = binstrmax.length();
  for (int i=0;i<(lenstrmax-lenstr);i++) { sb.append('0'); }
  sb.append(binstr);
  sb.append(NL);
  return sb.toString();
  }

/**
 * The method returns the search string for a XML link field for
 * the IBM Content Manager 7 persistence system.<p>
 * A full XML tag element shows like<br>
 * &lt;subtag xlink:href="href" xlink:sattrib="svalue" ... /&gt;
 *
 * @param part               the global part of the elements like 'metadata'
 *                           or 'service'
 * @param tag                the tagname of an element list
 * @param subtag             the tagname of an element from the list in a tag
 * @param sattrib            the optional attribute vector of a subtag
 * @param svalue             the optional value vector of sattrib
 * @param href               the reference value of this element
 * @return the search string for the CM7 text search engine
 **/
public final String createSearchStringHref(String part, String tag,
  String subtag, String [] sattrib, String [] svalue, MCRObjectID href)
  {
  if ((subtag == null) || ((subtag = subtag.trim()).length() ==0)) {
    return ""; }
  if (href == null) { return ""; }
  StringBuffer sb = new StringBuffer(1024);
  sb.append("XXX").append(part.toUpperCase()).append("XXX")
    .append(tag.toUpperCase()).append("XXX")
    .append(subtag.toUpperCase()).append("XXX ");
  if (sattrib != null) {
    for (int i=0;i<sattrib.length;i++) {
      sb.append("XXX").append(sattrib[i].toUpperCase()).append("XXX")
        .append(svalue[i].toUpperCase()).append("XXX ");
      }
    }
  MCRCM7Persistence mcr_pers = new MCRCM7Persistence();
  String label = mcr_pers.receiveLabel(href);
  sb.append(label.toUpperCase()).append(NL);
  sb.append("XXX").append(part.toUpperCase()).append("XXX")
    .append(tag.toUpperCase()).append("XXX")
    .append(subtag.toUpperCase()).append("XXX ");
  if (sattrib != null) {
    for (int i=0;i<sattrib.length;i++) {
      sb.append("XXX").append(sattrib[i].toUpperCase()).append("XXX")
        .append(svalue[i].toUpperCase()).append("XXX ");
      }
    }
  sb.append(href.getId().replace('_','X').toUpperCase()).append(NL);
  return sb.toString();
  }

}

