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

package org.mycore.datamodel.metadata;

import java.text.*;
import java.util.*;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * This class implements all methode for handling one document service data.
 * The service data are to use to handel the database with batch jobs
 * automatical changes. The service class holds two types of data, dates
 * and flags. The flags are text strings and are optional.<p>
 * 
 * The dates are represent by a date and a type. Two types are in service
 * data at every time and can't remove:
 * <ul>
 * <li>createdate - for the creating date of the object, this was set
 *                  only one time</li>
 * <li>modifydate - for the accepting date of the object, this was set
 *                  at every changes</li>
 * </ul>
 * Other date types are optional, but as example in Dublin Core:
 * <ul>
 * <li>submitdate - for the submiting date of the object</li>
 * <li>acceptdate - for the accepting date of the object</li>
 * <li>validfromdate - for the date of the object, at this the object is valid 
 *                     to use</li>
 * <li>validtodate - for the date of the object, at this the object is no 
 *                   more valid to use</li>
 * </ul>
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRObjectService
{
// common data
private String NL;
private static String DEFAULT_LANGUAGE = "de";

// service data
private String lang = null;
private ArrayList dates = null;
private ArrayList flags = null;

/**
 * This is the constructor of the MCRObjectService class. All data
 * are set to null.
 */
public MCRObjectService()
  {
  NL = new String((System.getProperties()).getProperty("line.separator"));
  lang = DEFAULT_LANGUAGE;
  dates = new ArrayList();
  MCRMetaDate d = new MCRMetaDate("service","servdate",lang,"createdate",
    false,new GregorianCalendar());
  dates.add(d);
  d = new MCRMetaDate("service","servdate",lang,"modifydate",
    false,new GregorianCalendar());
  dates.add(d);
  flags = new ArrayList();
  }

/**
 * This methode read the XML input stream part from a DOM part for the
 * structure data of the document.
 *
 * @param dom_element_list       a list of relevant DOM elements for
 *                               the metadata
 **/
public final void setFromDOM(org.jdom.Element service_element)
  {
  // Date part
  org.jdom.Element dates_element = service_element.getChild("servdates");
  if (dates_element!=null) {
    List date_element_list = dates_element.getChildren();
    int date_len = date_element_list.size();
    for (int i=0;i<date_len;i++) {  
      org.jdom.Element date_element = (org.jdom.Element)
        date_element_list.get(i);
      String date_element_name = date_element.getName();
      if (!date_element_name.equals("servdate")) { continue; }
      MCRMetaDate date = new MCRMetaDate();
      date.setDataPart("service");
      date.setLang(lang);
      date.setFromDOM(date_element);
      int k = -1;
      for (int j=0;j<dates.size();j++) {
        if (((MCRMetaDate)dates.get(j)).getType().equals(date.getType())) { 
          k = j;  break; } }
      if (k==-1) {
        dates.add(date); }
      else {
        dates.set(k,date); }
      }
    }
  // Flag part
  org.jdom.Element flags_element = service_element.getChild("servflags");
  if (flags_element!=null) {
    List flag_element_list = flags_element.getChildren();
    int flag_len = flag_element_list.size();
    for (int i=0;i<flag_len;i++) {  
      org.jdom.Element flag_element = (org.jdom.Element)
        flag_element_list.get(i);
      String flag_element_name = flag_element.getName();
      if (!flag_element_name.equals("servflag")) { continue; }
      MCRMetaLangText flag = new MCRMetaLangText();
      flag.setLang(lang);
      flag.setDataPart("service");
      flag.setFromDOM(flag_element);
      flags.add(flag);
      }
    }
  }

/**
 * This method get a date for a given type. If the type was not found,
 * an null was returned.
 *
 * @param type           the type of the date
 * @return the date as GregorianCalendar
 **/
public final GregorianCalendar getDate(String type)
  {
  if ((type == null) || ((type = type.trim()).length() ==0)) {
    return null; }
  int i = -1;
  for (int j=0;j<dates.size();j++) {
    if (((MCRMetaDate)dates.get(j)).getType().equals(type)) { i = j; } }
  if (i==-1) { return null; }
  MCRMetaDate d = (MCRMetaDate)dates.get(i);
  return d.getDate();
  }

/**
 * This method remove a date for a given type. 
 *
 * @param type           the type of the date
 **/
public final void removeDate(String type)
  {
  if ((type == null) || ((type = type.trim()).length() ==0)) {
    return; }
  int i = -1;
  for (int j=0;j<dates.size();j++) {
    if (((MCRMetaDate)dates.get(j)).getType().equals(type)) { i = j; } }
  if (i==-1) { return; }
  dates.remove(i);
  }

/**
 * This methode set a date element in the dates list to a actual date value.
 * If the given type exists, the date was update.
 *
 * @param type           the type of the date
 **/
public final void setDate(String type)
  {
  if ((type == null) || ((type = type.trim()).length() ==0)) {
    return; }
  int i = -1;
  for (int j=0;j<dates.size();j++) {
    if (((MCRMetaDate)dates.get(j)).getType().equals(type)) { i = j; } }
  if (i==-1) {
    MCRMetaDate d = new MCRMetaDate("service","servdate",null,type,
      false,new GregorianCalendar());
    dates.add(d);
    }
  else {
    MCRMetaDate d = (MCRMetaDate)dates.get(i);
    d.setDate(new GregorianCalendar());
    dates.set(i,d);
    }
  }

/**
 * This methode set a date element in the dates list to a given date value.
 * If the given type exists, the date was update.
 *
 * @param type           the type of the date
 * @param date           the given date
 **/
public final void setDate(String type, GregorianCalendar date)
  {
  if ((type == null) || ((type = type.trim()).length() ==0)) {
    return; }
  if (date == null) { return; }
  int i = -1;
  for (int j=0;j<dates.size();j++) {
    if (((MCRMetaDate)dates.get(j)).getType().equals(type)) { i = j; } }
  if (i==-1) {
    MCRMetaDate d = new MCRMetaDate("service","servdate",null,type,false,date);
    dates.add(d);
    }
  else {
    MCRMetaDate d = (MCRMetaDate)dates.get(i);
    d.setDate(date);
    dates.set(i,d);
    }
  }

/**
 * This methode add a flag to the flag list.
 *
 * @param value          - the new flag as string
 **/
public final void addFlag(String value)
  {
  if ((value == null) || ((value = value.trim()).length() ==0)) {
    return; }
  MCRMetaLangText flag = new MCRMetaLangText("service","servflag",null,null,
    false,value);
  flags.add(flag);
  }
 
/**
 * This methode get all flags from the flag list as a string.
 *
 * @return the flags string
 **/
public final String getFlags()
  {
  StringBuffer sb = new StringBuffer("");
  for (int i=0;i<flags.size();i++) {
    sb.append(((MCRMetaLangText)flags.get(i)).getText()).append(" "); }
  return sb.toString();
  }

/**
 * This methode get a single flag from the flag list as a string.
 *
 * @exception IndexOutOfBoundsException throw this exception, if
 *                              the index is false
 * @return a flag string
 **/
public final String getFlag(int index) throws IndexOutOfBoundsException
  {
  if ((index<0)||(index>flags.size())) {
    throw new IndexOutOfBoundsException("Index error in removeFlag."); }
  return ((MCRMetaLangText)flags.get(index)).getText();
  }

/**
 * This methode return a boolean value if the given flag is set or not.
 *
 * @param value                 a searched flag
 * @return true if the flag was found in the list
 **/
public final boolean isFlagSet(String value)
  {
  if ((value == null) || ((value = value.trim()).length() ==0)) {
    return false; }
  for (int i=0;i<flags.size();i++) {
    if (((MCRMetaLangText)flags.get(i)).getText().equals(value)) { 
      return true; }
    }
  return false;
  }
 
/**
 * This methode remove a flag from the flag list.
 *
 * @param index                 a index in the list
 * @exception IndexOutOfBoundsException throw this exception, if
 *                              the index is false
 **/
public final void removeFlag(int index) throws IndexOutOfBoundsException
  {
  if ((index<0)||(index>flags.size())) {
    throw new IndexOutOfBoundsException("Index error in removeFlag."); }
  flags.remove(index);
  }

/**
 * This methode set a flag in the flag list.
 *
 * @param index                 a index in the list
 * @param value                 the value of a flag as string
 * @exception IndexOutOfBoundsException throw this exception, if
 *                              the index is false
 **/
public final void replaceFlag(int index, String value) 
  throws IndexOutOfBoundsException
  {
  if ((index<0)||(index>flags.size())) {
    throw new IndexOutOfBoundsException("Index error in replaceFlag."); }
  if ((value == null) || ((value = value.trim()).length() ==0)) {
    return; }
  MCRMetaLangText flag = new MCRMetaLangText("service","servflag",null,null,
    false,value);
  flags.set(index,flag);
  }

/**
 * This methode create a XML stream for all structure data.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a JDOM Element with the XML data of the structure data part
 **/
public final org.jdom.Element createXML() throws MCRException
  {
  if (!isValid()) {
    throw new MCRException("The content is not valid."); }
  org.jdom.Element elm = new org.jdom.Element("service");
  if (dates.size()!=0) {
    org.jdom.Element elmm = new org.jdom.Element("servdates");
    elmm.setAttribute("class","MCRMetaDate");
    elmm.setAttribute("heritable","false");
    elmm.setAttribute("parasearch","true");
    elmm.setAttribute("textsearch","false");
    for (int i=0;i<dates.size();i++) {
      elmm.addContent(((MCRMetaDate)dates.get(i)).createXML()); }
    elm.addContent(elmm); 
    }
  if (flags.size()!=0) {
    org.jdom.Element elmm = new org.jdom.Element("servflags");
    elmm.setAttribute("class","MCRMetaLangText");
    elmm.setAttribute("heritable","false");
    elmm.setAttribute("parasearch","true");
    elmm.setAttribute("textsearch","false");
    for (int i=0;i<flags.size();i++) {
      elmm.addContent(((MCRMetaLangText)flags.get(i)).createXML()); }
    elm.addContent(elmm); 
    }
  return elm;
  }

/**
 * This methode create a typed content list for all structure data.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a MCRTypedContent with the data of the metadata part
 **/
public final MCRTypedContent createTypedContent() throws MCRException
  {
  if (!isValid()) {
    throw new MCRException("The content is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  tc.addTagElement(tc.TYPE_MASTERTAG,"service");
  if (dates.size()!=0) {
    tc.addTagElement(tc.TYPE_TAG,"servdates");
    for (int i=0;i<dates.size();i++) {
      tc.addMCRTypedContent(((MCRMetaDate)dates.get(i))
        .createTypedContent(true));
      }
    }
  if (flags.size()!=0) {
    tc.addTagElement(tc.TYPE_TAG,"servflags");
    for (int i=0;i<flags.size();i++) {
      tc.addMCRTypedContent(((MCRMetaLangText)flags.get(i))
        .createTypedContent(true)); }
    }
  return tc;
  }

/**
 * This method check the validation of the content of this class.
 * The method returns <em>true</em> if
 * <ul>
 * <li> the date value of "createdate" is not null or empty
 * <li> the date value of "modifydate" is not null or empty
 * </ul>
 * otherwise the method return <em>false</em>
 *
 * @return a boolean value
 **/
public final boolean isValid()
  {
  if (getDate("createdate") == null) { return false; }
  if (getDate("modifydate") == null) { return false; }
  return true;
  }

}

