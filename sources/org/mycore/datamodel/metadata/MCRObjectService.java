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
 * automatical changes.
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
private Vector flag;
private int vec_max_length = 1;

private static DateFormat df =
  DateFormat.getDateInstance(DateFormat.MEDIUM,Locale.GERMANY);

/**
 * This is the constructor of the MCRObjectService class.
 */
public MCRObjectService()
  {
  NL = new String((System.getProperties()).getProperty("line.separator"));
  createdate = null;
  submitdate = null;
  acceptdate = null;
  modifydate = null;
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
 * This methode return the value of the flag with index number.
 *
 * @return the string of flag from index
 **/
public final String getSingleFlag(int index)
  { return (String)flag.elementAt(index); }

/**
 * This methode return the values of the flags as a XML string.
 *
 * @return the xml string of the flags
 **/
public final String getFlags()
  { 
  StringBuffer sb = new StringBuffer(256);
  for (int i=0;i<flag.size();i++) {
    sb.append("<flag>").append(flag.elementAt(i)).append("</flag>"); }
  return sb.toString();
  }

/**
 * This methode set the create date to the actual date.
 **/
public final void setCreateDate()
  { createdate = new GregorianCalendar(); }

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
 * This methode create a XML stream for all structure data.
 *
 * @return a XML string with the XML data of the structure data part
 **/
public final String createXML()
  {
  StringBuffer sb = new StringBuffer(2048);
  sb.append("<service>").append(NL);
  if ((createdate != null) || (submitdate != null) || 
      (acceptdate != null) || (modifydate != null)) {
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
 * @param type   the type of the persistece system
 * @return a Text Search string with the data of the metadata part
 **/
public final String createTS(String type)
  {
  if (type.equals("CM7")) {
    StringBuffer sb = new StringBuffer(2048);
    sb.append("<service>").append(NL);
    if ((createdate != null) || (submitdate != null) || 
        (acceptdate != null) || (modifydate != null)) {
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
  return "";
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
  for (int i=0;i<flag.size();i++) {
    System.out.println("flag                 = "+flag.elementAt(i)); }
  System.out.println();
  }

}

