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

package mycore.datamodel;

import java.text.*;
import java.util.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import mycore.common.MCRConfiguration;
import mycore.common.MCRException;
import mycore.datamodel.MCRMetaDate;
import mycore.datamodel.MCRMetaLangText;

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

// service data
private ArrayList dates = null;
private ArrayList flags = null;

/**
 * This is the constructor of the MCRObjectService class. All data
 * are set to null.
 */
public MCRObjectService()
  {
  NL = new String((System.getProperties()).getProperty("line.separator"));
  dates = new ArrayList();
  MCRMetaDate d = new MCRMetaDate("service","date",null,"createdate",
    new GregorianCalendar());
  dates.add(d);
  d = new MCRMetaDate("service","date",null,"modifydate",
    new GregorianCalendar());
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
public final void setFromDOM(NodeList dom_element_list)
  {
  Node date_element, flag_element;
  NodeList date_list, flag_list;
  Element service_element = (Element)dom_element_list.item(0);
  // Date part
  NodeList service_dates_list =
    service_element.getElementsByTagName("dates");
  if (service_dates_list.getLength()>0) {
    Node service_dates_element = service_dates_list.item(0);
    NodeList service_date_list = service_dates_element.getChildNodes();
    int date_len = service_date_list.getLength();
    for (int i=0;i<date_len;i++) {  
      date_element = service_date_list.item(i);
      if (date_element.getNodeType() != Node.ELEMENT_NODE) { continue; }
      MCRMetaDate date = new MCRMetaDate();
      date.setDataPart("service");
      date.setFromDOM(date_element);
      dates.add(date);
      }
    }
  // Flag part
  NodeList service_flags_list =
    service_element.getElementsByTagName("flags");
  if (service_dates_list.getLength()>0) {
    Node service_flags_element = service_flags_list.item(0);
    NodeList service_flag_list = service_flags_element.getChildNodes();
    int flag_len = service_flag_list.getLength();
    for (int i=0;i<flag_len;i++) {
      flag_element = service_flag_list.item(i);
      if (flag_element.getNodeType() != Node.ELEMENT_NODE) { continue; }
      MCRMetaLangText flag = new MCRMetaLangText();
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
    MCRMetaDate d = new MCRMetaDate("service","date",null,type,
      new GregorianCalendar());
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
    MCRMetaDate d = new MCRMetaDate("service","date",null,type,date);
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
  MCRMetaLangText flag = new MCRMetaLangText("service","flag",null,null,value);
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
  MCRMetaLangText flag = new MCRMetaLangText("service","flag",null,null,value);
  flags.set(index,flag);
  }

/**
 * This methode create a XML stream for all structure data.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a XML string with the XML data of the structure data part
 **/
public final String createXML() throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  StringBuffer sb = new StringBuffer(2048);
  sb.append("<service>").append(NL);
  if (dates.size()!=0) {
    sb.append("<dates>").append(NL);
    for (int i=0;i<dates.size();i++) {
      sb.append(((MCRMetaDate)dates.get(i)).createXML()); }
    sb.append("</dates>").append(NL);
    }
  if (flags.size()!=0) {
    sb.append("<flags>").append(NL);
    for (int i=0;i<flags.size();i++) {
      sb.append(((MCRMetaLangText)flags.get(i)).createXML()); }
    sb.append("</flags>").append(NL);
    }
  sb.append("</service>").append(NL);
  return sb.toString();
  }

/**
 * This methode create a Text Search stream for all structure data.
 * The content of this stream is depended by the implementation for
 * the persistence database. It was choose over the
 * <em>MCR.persistence_type</em> configuration.
 *
 * @param mcr_query   a class they implement the <b>MCRQueryInterface</b>
 * @exception MCRException if the content of this class is not valid
 * @return a Text Search string with the data of the metadata part
 **/
public final String createTS(Object mcr_query)  throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  StringBuffer sb = new StringBuffer(2048);
  if (dates.size()!=0) {
    for (int i=0;i<dates.size();i++) {
      sb.append(((MCRMetaDate)dates.get(i)).createTS(mcr_query)); }
    }
  else {
    sb.append(""); }
  if (flags.size()!=0) {
    for (int i=0;i<flags.size();i++) {
      sb.append(((MCRMetaLangText)flags.get(i)).createTS(mcr_query)); }
    }
  else {
    sb.append(""); }
  return sb.toString();
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

/**
 * This metode print all data content from the internal data of the
 * metadata class.
 **/
public final void debug()
  {
  System.out.println("MCRMetaService debug start:");
  for (int i=0;i<dates.size();i++) {
    ((MCRMetaDate)dates.get(i)).debug(); }
  for (int i=0;i<flags.size();i++) {
    ((MCRMetaLangText)flags.get(i)).debug(); }
  System.out.println("MCRMetaService debug end"+NL);
  }

}

