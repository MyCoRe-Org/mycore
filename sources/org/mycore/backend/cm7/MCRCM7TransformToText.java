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
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.query.MCRQueryInterface;

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
// 32 Bit
protected static int MAX_BIN_STRING_LENGTH = 1024 * 1024 * 1024 * 2;

/**
 * The constructor.
 **/
public MCRCM7TransformToText()
  {}

/**
 * The method returns the search string for a XML text field for
 * the IBM Content Manager 7 persistence system.<p>
 *
 * @param text               the text value of this element
 * @return the search string for the CM7 text search engine
 **/
public final String createSearchStringText(String text)
  {
  if ((text == null) || ((text = text.trim()).length() ==0)) {
    return ""; }
  StringBuffer sb = new StringBuffer(1024);
  try {
    MCRObjectID mid = new MCRObjectID(text);
    if (mid.isValid()) {
      text = mid.getId().replace('_','X'); }
    }
  catch (MCRException e) { }
  sb.append(text.replace('\n',' ').replace('\r',' ').toUpperCase()); 
  return sb.toString();
  }

/**
 * The method returns the search string for a XML attribute field for
 * the IBM Content Manager 7 persistence system.<p>
 *
 * @param attr               the text value of this element
 * @return the search string for the CM7 text search engine
 **/
public final String createSearchStringAttr(String attr)
  {
  if ((attr == null) || ((attr = attr.trim()).length() ==0)) {
    return ""; }
  StringBuffer sb = new StringBuffer(1024);
  try {
    MCRObjectID mid = new MCRObjectID(attr);
    if (mid.isValid()) {
      attr = mid.getId().replace('_','X');
      sb.append(attr.toUpperCase());
      return sb.toString();
      }
    }
  catch (MCRException e) { }
  String attru = attr.replace('\n',' ').replace('\r',' ').toUpperCase(); 
  for (int i=0;i<attr.length();i++) {
    if ((attru.charAt(i)>='A')&&(attru.charAt(i)<='Z')) { 
      sb.append(attru.charAt(i)); continue; }
    if ((attru.charAt(i)>='0')&&(attru.charAt(i)<='9')) { 
      sb.append(attru.charAt(i)); continue; }
    sb.append('X');  
    }
  return sb.toString();
  }

/**
 * The method returns the search string for a XML date field for
 * the IBM Content Manager 7 persistence system.<p>
 * The date was transformed to a bit string with 
 * <em>10000*year+100*month+day</em>.
 *
 * @param date               the date value of this element
 * @return the search string for the CM7 text search engine
 **/
public final String createSearchStringDate(GregorianCalendar date)
  {
  if (date == null) { return ""; }
  StringBuffer sb = new StringBuffer(1024);
  sb.append("YYY");
  int idate = 0;
  if (date.get(Calendar.ERA) == GregorianCalendar.AD) {
    idate = (4000+date.get(Calendar.YEAR))*10000 +
                  date.get(Calendar.MONTH)*100 +
                  date.get(Calendar.DAY_OF_MONTH); }
  else {
    idate = (4000-date.get(Calendar.YEAR))*10000 +
                  date.get(Calendar.MONTH)*100 +
                  date.get(Calendar.DAY_OF_MONTH); }
  String binstr = Integer.toBinaryString(idate);
  String binstrmax = Integer.toBinaryString(MAX_BIN_STRING_LENGTH);
  int lenstr = binstr.length();
  int lenstrmax = binstrmax.length();
  for (int i=0;i<(lenstrmax-lenstr);i++) { sb.append('0'); }
  sb.append(binstr);
  return sb.toString();
  }

/**
 * The method returns the search string for a XML boolean field for
 * the IBM Content Manager 7 persistence system.<p>
 * The boolean value was transformed to a string.
 *
 * @param bvalue             the boolean value of this element
 * @return the search string for the CM7 text search engine
 **/
public final String createSearchStringBoolean(boolean bvalue)
  {
  if (bvalue) {
    return "TRUE"; }
  else {
    return "FALSE"; }
  }

/**
 * The method returns the search string for a XML field with numbers for
 * the IBM Content Manager 7 persistence system.<p>
 * The number was transformed to a string to cut the number of the third decimal
 * position and transpose it then in a integer number by multiply with 1000.
 * The number must be in range of 10^6 to 0.
 *
 * @param number             the number value of this element as a double value
 * @return the search string for the CM7 text search engine
 **/
public final String createSearchStringDouble(double number)
  {
  if ((number < 0.) || (number > 10.e6)) { return ""; }
  StringBuffer sb = new StringBuffer(1024);
  sb.append("YYY");
  // 3 numbers after decimal point
  long a = (Math.round(number*10000.))/10; 
  String binstr = Long.toBinaryString(a);
  String binstrmax = Integer.toBinaryString(MAX_BIN_STRING_LENGTH);
  int lenstr = binstr.length();
  int lenstrmax = binstrmax.length();
  for (int i=0;i<(lenstrmax-lenstr);i++) { sb.append('0'); }
  sb.append(binstr);
  return sb.toString();
  }

}

