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

/**
 * This class implements all methode for handling one document service data.
 * The service data are to use to handel the database with batch jobs
 * automatical changes.<p>
 *
 * The following data fields are in this class:
 * <ul>
 * <li>createdate - for the creating date of the object, this was set
 *                  only one time</li>
 * <li>submitdate - for the submiting date of the object</li>
 * <li>acceptdate - for the accepting date of the object</li>
 * <li>modifydate - for the accepting date of the object, this was set
 *                  at every changes</li>
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
private String NL;
private GregorianCalendar createdate;
private GregorianCalendar submitdate;
private GregorianCalendar acceptdate;
private GregorianCalendar modifydate;
private GregorianCalendar validfromdate;
private GregorianCalendar validtodate;
private Vector flag;
private int vec_max_length = 1;

private static DateFormat df =
  DateFormat.getDateInstance(DateFormat.MEDIUM,Locale.GERMANY);

/**
 * This is the constructor of the MCRObjectService class. All data
 * are set to null.
 */
public MCRObjectService()
  {
  NL = new String((System.getProperties()).getProperty("line.separator"));
  createdate = null;
  submitdate = null;
  acceptdate = null;
  modifydate = null;
  validfromdate = null;
  validtodate = null;
  vec_max_length = MCRConfiguration.instance()
    .getInt("MCR.metadata_service_vec_max_length",1);
  flag = new Vector(vec_max_length);
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
  Node textelement, flags_node;
  Element dates_element, flags_element;
  NodeList date_list, flag_list;
  Element service_element = (Element)dom_element_list.item(0);
  NodeList service_dates_list =
    service_element.getElementsByTagName("dates");
  int dates_len = service_dates_list.getLength();
  if (dates_len != 0) {
    dates_element = (Element)service_dates_list.item(0);
    date_list = dates_element.getElementsByTagName("createdate");
    if (date_list.getLength() != 0) {
      textelement = (Node)(date_list.item(0)).getFirstChild();
      setCreateDate(((Text)textelement).getData().trim());
      }
    date_list = dates_element.getElementsByTagName("submitdate");
    if (date_list.getLength() != 0) {
      textelement = (Node)(date_list.item(0)).getFirstChild();
      setSubmitDate(((Text)textelement).getData().trim());
      }
    date_list = dates_element.getElementsByTagName("acceptdate");
    if (date_list.getLength() != 0) {
      textelement = (Node)(date_list.item(0)).getFirstChild();
      setAcceptDate(((Text)textelement).getData().trim());
      }
    date_list = dates_element.getElementsByTagName("modifydate");
    if (date_list.getLength() != 0) {
      textelement = (Node)(date_list.item(0)).getFirstChild();
      setModifyDate(((Text)textelement).getData().trim());
      }
    date_list = dates_element.getElementsByTagName("validfromdate");
    if (date_list.getLength() != 0) {
      textelement = (Node)(date_list.item(0)).getFirstChild();
      setValidFromDate(((Text)textelement).getData().trim());
      }
    date_list = dates_element.getElementsByTagName("validtodate");
    if (date_list.getLength() != 0) {
      textelement = (Node)(date_list.item(0)).getFirstChild();
      setValidToDate(((Text)textelement).getData().trim());
      }
    }
  NodeList service_flags_list =
    service_element.getElementsByTagName("flags");
  Node service_flags_node = service_flags_list.item(0);
  NodeList flags_list = service_flags_node.getChildNodes();
  int flags_len = flags_list.getLength();
  for (int i=0;i<flags_len;i++) {
    flags_node = flags_list.item(i);
    if (flags_node.getNodeType() != Node.ELEMENT_NODE) { continue; }
    textelement = flags_node.getFirstChild();
    flag.addElement(((Text)textelement).getData().trim());
    }
  }

/**
 * This methode return the create date as GregorianCalendar.
 *
 * @return the create date
 **/
public final GregorianCalendar getCreateDate()
  { return createdate; }

/**
 * This methode return the create date as string.
 *
 * @return the create date
 **/
public final String getCreateString()
  {
  if (createdate == null) { return null; }
  return df.format(createdate.getTime());
  }

/**
 * This methode return the submit date as GregorianCalendar.
 *
 * @return the submit date
 **/
public final GregorianCalendar getSubmitDate()
  { return submitdate; }

/**
 * This methode return the submit date as string.
 *
 * @return the submit date
 **/
public final String getSubmitString()
  {
  if (submitdate == null) { return null; }
  return df.format(submitdate.getTime());
  }

/**
 * This methode return the accept date as GregorianCalendar.
 *
 * @return the accept date
 **/
public final GregorianCalendar getAcceptDate()
  { return acceptdate; }

/**
 * This methode return the accept date as string.
 *
 * @return the accept date
 **/
public final String getAcceptString()
  {
  if (acceptdate == null) { return null; }
  return df.format(acceptdate.getTime());
  }

/**
 * This methode return the modify date as GregorianCalendar.
 *
 * @return the modify date
 **/
public final GregorianCalendar getModifyDate()
  { return modifydate; }

/**
 * This methode return the modify date as string.
 *
 * @return the modify date
 **/
public final String getModifyString()
  {
  if (modifydate == null) { return null; }
  return df.format(modifydate.getTime());
  }

/**
 * This methode return the validfrom date as GregorianCalendar.
 *
 * @return the validfrom date
 **/
public final GregorianCalendar getValidFromDate()
  { return validfromdate; }

/**
 * This methode return the validfrom date as string.
 *
 * @return the validfrom date
 **/
public final String getValidFromString()
  {
  if (validfromdate == null) { return null; }
  return df.format(validfromdate.getTime());
  }

/**
 * This methode return the validto date as GregorianCalendar.
 *
 * @return the validto date
 **/
public final GregorianCalendar getValidToDate()
  { return validtodate; }

/**
 * This methode return the validto date as string.
 *
 * @return the validto date
 **/
public final String getValidToString()
  {
  if (validtodate == null) { return null; }
  return df.format(validtodate.getTime());
  }

/**
 * This methode return the value of the single flag with index number.
 *
 * @return the string of flag from index
 **/
public final String getFlag(int index)
  { return (String)flag.elementAt(index); }

/**
 * This methode return the values of the flag list as string.
 * each element begins with the <em>XXXFLAGXXX</em> string.
 *
 * @return the string of the flags
 **/
public final String getFlags()
  { 
  StringBuffer sb = new StringBuffer(256);
  for (int i=0;i<flag.size();i++) {
    sb.append("XXXFLAGXXX ").append(flag.elementAt(i)).append(' '); }
  return sb.toString();
  }

/**
 * This methode set the create date to the actual date.
 **/
public final void setCreateDate()
  { createdate = new GregorianCalendar(); }

/**
 * This methode set the create date to the given date.
 **/
public final void setCreateDate(GregorianCalendar date)
  { 
  createdate = null;
  if (date != null) { createdate = date; }
  }

/**
 * This methode set the create date to the given date.
 *
 * @param date               a date string
 * @exception MCRException   if the date is bad
 **/
public final void setCreateDate(String date) throws MCRException
  {
  createdate = null;
  if ((date == null) || ((date = date.trim()).length() ==0)) { return; }
  try {
    createdate = new GregorianCalendar();
    createdate.setTime(df.parse(date)); }
  catch (ParseException e) {
    throw new MCRException( "Can't parse create date."); }
  }

/**
 * This methode set the submit date to the actual date.
 **/
public final void setSubmitDate()
  { submitdate = new GregorianCalendar(); }

/**
 * This methode set the submit date to the given date.
 **/
public final void setSubmitDate(GregorianCalendar date)
  { 
  submitdate = null;
  if (date != null) { submitdate = date; }
  }

/**
 * This methode set the submit date to the given date.
 *
 * @param date               a date string
 * @exception MCRException   if the date is bad
 **/
public final void setSubmitDate(String date) throws MCRException
  {
  submitdate = null;
  if ((date == null) || ((date = date.trim()).length() ==0)) { return; }
  try {
    submitdate = new GregorianCalendar();
    submitdate.setTime(df.parse(date)); }
  catch (ParseException e) {
    throw new MCRException( "Can't parse submit date."); }
  }

/**
 * This methode set the accept date to the actual date.
 **/
public final void setAcceptDate()
  { acceptdate = new GregorianCalendar(); }

/**
 * This methode set the accept date to the given date.
 **/
public final void setAcceptDate(GregorianCalendar date)
  { 
  acceptdate = null;
  if (date != null) { acceptdate = date; }
  }

/**
 * This methode set the accept date to the given date.
 *
 * @param date               a date string
 * @exception MCRException   if the date is bad
 **/
public final void setAcceptDate(String date) throws MCRException
  {
  acceptdate = null;
  if ((date == null) || ((date = date.trim()).length() ==0)) { return; }
  try {
    acceptdate = new GregorianCalendar();
    acceptdate.setTime(df.parse(date)); }
  catch (ParseException e) {
    throw new MCRException( "Can't parse accept date."); }
  }

/**
 * This methode set the modify date to the actual date.
 **/
public final void setModifyDate()
  { modifydate = new GregorianCalendar(); }

/**
 * This methode set the modify date to the given date.
 **/
public final void setModifyDate(GregorianCalendar date)
  { 
  modifydate = null;
  if (date != null) { modifydate = date; }
  }

/**
 * This methode set the modify date to the given date.
 *
 * @param date               a date string
 * @exception MCRException   if the date is bad
 **/
public final void setModifyDate(String date) throws MCRException
  {
  modifydate = null;
  if ((date == null) || ((date = date.trim()).length() ==0)) { return; }
  try {
    modifydate = new GregorianCalendar();
    modifydate.setTime(df.parse(date)); }
  catch (ParseException e) {
    throw new MCRException( "Can't parse modify date."); }
  }

/**
 * This methode set the validfrom date to the actual date.
 **/
public final void setValidFromDate()
  { validfromdate = new GregorianCalendar(); }

/**
 * This methode set the validfrom date to the given date.
 **/
public final void setValidFromDate(GregorianCalendar date)
  { 
  validfromdate = null;
  if (date != null) { validfromdate = date; }
  }

/**
 * This methode set the validfrom date to the given date.
 *
 * @param date               a date string
 * @exception MCRException   if the date is bad
 **/
public final void setValidFromDate(String date) throws MCRException
  {
  validfromdate = null;
  if ((date == null) || ((date = date.trim()).length() ==0)) { return; }
  try {
    validfromdate = new GregorianCalendar();
    validfromdate.setTime(df.parse(date)); }
  catch (ParseException e) {
    throw new MCRException( "Can't parse validfrom date."); }
  }

/**
 * This methode set the validto date to the actual date.
 **/
public final void setValidToDate()
  { validtodate = new GregorianCalendar(); }

/**
 * This methode set the validto date to the given date.
 **/
public final void setValidToDate(GregorianCalendar date)
  { 
  validtodate = null;
  if (date != null) { validtodate = date; }
  }

/**
 * This methode set the validto date to the given date.
 *
 * @param date               a date string
 * @exception MCRException   if the date is bad
 **/
public final void setValidToDate(String date) throws MCRException
  {
  validtodate = null;
  if ((date == null) || ((date = date.trim()).length() ==0)) { return; }
  try {
    validtodate = new GregorianCalendar();
    validtodate.setTime(df.parse(date)); }
  catch (ParseException e) {
    throw new MCRException( "Can't parse validto date."); }
  }

/**
 * This methode add a flag to the flag list.
 *
 * @param value          - the new flag as string
 * @exception IndexOutOfBoundsException throw this exception, if
 *                              the vector is full
 **/
public final void addFlag(String value) throws IndexOutOfBoundsException
  {
  if (flag.size()>=vec_max_length) {
    throw new IndexOutOfBoundsException("Index error in addFlag."); }
  if ((value == null) || ((value = value.trim()).length() ==0)) {
    return; }
  flag.addElement(value);
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
  for (int i=0;i<flag.size();i++) {
    if (flag.elementAt(i).equals(value)) { return true; } }
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
  if ((index<0)||(index>flag.size())) {
    throw new IndexOutOfBoundsException("Index error in removeFlag."); }
  flag.removeElementAt(index);
  }

/**
 * This methode replace a flag in the flag list.
 *
 * @param index                 a index in the list
 * @param value                 the value of a flag as string
 * @exception IndexOutOfBoundsException throw this exception, if
 *                              the index is false
 **/
public final void replaceFlag(int index, String value) 
  throws IndexOutOfBoundsException
  {
  if ((index<0)||(index>flag.size())) {
    throw new IndexOutOfBoundsException("Index error in replaceFlag."); }
  if ((value == null) || ((value = value.trim()).length() ==0)) {
    return; }
  flag.setElementAt(value,index);
  }

/**
 * This methode sets the new flag vector from a given string.
 * Each new flag begins with XXXFLAGXXX in the string.
 *
 * @param value                 the flag string
 **/
public final void setFlags(String value)
  {
  if (value == null) { return; }
  int len = 0;
  value = value.trim();
  len = value.length();
  if (len == 0) { return; }
  int i = value.indexOf("XXXFLAGXXX");
  if ( i== -1) { return; }
  while (i+11<=len) {
    int j = value.indexOf("XXXFLAGXXX",i+11);
    if (j == -1) {
      addFlag(value.substring(i+11,len)); i = len; }
    else {
      addFlag(value.substring(i+11,j)); i = j; }
    }
  }

/**
 * This methode create a XML stream for all structure data.
 *
 * @return a XML string with the XML data of the structure data part
 **/
public final String createXML()
  {
  StringBuffer sb = new StringBuffer(2048);
  sb.append("<service>").append(NL);
  if ((createdate != null) || (submitdate != null) || 
      (acceptdate != null) || (modifydate != null) ||
      (validfromdate != null) || (validtodate != null)) {
    sb.append("<dates>").append(NL);
    if (createdate != null) {
      sb.append("<createdate>").append(getCreateString())
        .append("</createdate>").append(NL); }
    if (submitdate != null) {
      sb.append("<submitdate>").append(getSubmitString())
        .append("</submitdate>").append(NL); }
    if (acceptdate != null) {
      sb.append("<acceptdate>").append(getAcceptString())
        .append("</acceptdate>").append(NL); }
    if (modifydate != null) {
      sb.append("<modifydate>").append(getModifyString())
        .append("</modifydate>").append(NL); }
    if (validfromdate != null) {
      sb.append("<validfromdate>").append(getValidFromString())
        .append("</validfromdate>").append(NL); }
    if (validtodate != null) {
      sb.append("<validtodate>").append(getValidToString())
        .append("</validtodate>").append(NL); }
    sb.append("</dates>").append(NL);
    }
  if (flag.size()!=0) {
    sb.append("<flags>").append(NL);
    for (int i=0;i<flag.size();i++) {
      sb.append("<flag>").append(flag.elementAt(i)).append("</flag>")
        .append(NL); }
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
 * @return a Text Search string with the data of the metadata part
 **/
public final String createTS(Object mcr_query)
  {
  StringBuffer sb = new StringBuffer(2048);
  if (flag.size()!=0) {
    for (int i=0;i<flag.size();i++) {
      sb.append(((MCRQueryInterface)mcr_query).createSearchStringText("Flag",
        (String)flag.elementAt(i)));
      }
    }
  else {
    sb.append("").append(NL); }
  return sb.toString();
  }

/**
 * This metode print all data content from the internal data of the
 * metadata class.
 **/
public final void debug()
  {
  System.out.println("The document service data content :");
  if (createdate != null) {
    System.out.println("create date          = "+getCreateString()); }
  if (submitdate != null) {
    System.out.println("submit date          = "+getSubmitString()); }
  if (acceptdate != null) {
    System.out.println("accept date          = "+getAcceptString()); }
  if (modifydate != null) {
    System.out.println("modify date          = "+getModifyString()); }
  if (validfromdate != null) {
    System.out.println("validfrom date       = "+getValidFromString()); }
  if (validtodate != null) {
    System.out.println("validto date         = "+getValidToString()); }
  for (int i=0;i<flag.size();i++) {
    System.out.println("flag                 = "+flag.elementAt(i)); }
  System.out.println();
  }

}

