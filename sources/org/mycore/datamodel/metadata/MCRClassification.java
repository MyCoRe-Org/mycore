/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  *** 
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package mycore.datamodel;

import java.io.*;
import java.util.*;
import mycore.common.*;
import mycore.xml.MCRXMLHelper;

/**
 * This class implements all methods for a classification and extended
 * the MCRClassificationObject class.
 *
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRClassification 
  {

  private static int MAX_CATEGORY_DEEP = 15;
  private MCRClassificationItem cl;
  private ArrayList cat;

  /**
   * The constructor
   **/
  public MCRClassification()
    { }

  /**
   * The method fill the instance of this class with a given JODM tree.
   *
   * @param jdom the classification as jdom tree
   **/
  private final void setFromJDOM( org.jdom.Document jdom ) 
    { 
    org.jdom.Element root = jdom.getRootElement();
    String ID = (String)root.getAttribute("ID").getValue();
    MCRObjectID mcr_id = new MCRObjectID(ID);
    cl = new MCRClassificationItem(ID);
    List element_list = root.getChildren();
    org.jdom.Element categories = null;
    int len = element_list.size();
    for (int i=0;i<len;i++) {
      org.jdom.Element tag = (org.jdom.Element)element_list.get(i);
      if (tag.getName().equals("label")) {
        String text = tag.getAttributeValue("text");
        String desc = tag.getAttributeValue("description");
        String lang = tag.getAttributeValue("lang");
        cl.addData(lang,text,desc);
        }
      else {
        categories = (org.jdom.Element)tag.clone(); }
      }
//System.out.println(cl.toString());
//System.out.println(categories.getName());
    cat = new ArrayList();
    MCRClassificationObject [] pid = new 
      MCRClassificationObject[MAX_CATEGORY_DEEP];
    int [] pos = new int[MAX_CATEGORY_DEEP];
    List [] list = new List[MAX_CATEGORY_DEEP];
    pid[0] = cl;
    pos[0] = 0;
    list[0] = categories.getChildren();
    int deep = 0;
    while(deep != -1) {
      if (pos[deep] >= list[deep].size()) { 
        deep--; if(deep < 0) {break;}
        pos[deep]++; continue; }
      org.jdom.Element cattag = (org.jdom.Element)list[deep].get(pos[deep]);
      String name = cattag.getName();
//System.out.println(name);
      if (name.equals("label")) { pos[deep]++; continue; }
      if (!name.equals("category")) { pos[deep]++; continue; }
      String theID = cattag.getAttribute("ID").getValue();
//System.out.println(theID);
      MCRCategoryItem ci = new MCRCategoryItem(theID,pid[deep]);
      List catlist = cattag.getChildren();
      int catlen = catlist.size();
      boolean catflag = false;
      for (int i=0;i<catlen;i++) {
        org.jdom.Element tag = (org.jdom.Element)catlist.get(i);
        if (tag.getName().equals("label")) {
          String text = tag.getAttributeValue("text");
          String desc = tag.getAttributeValue("description");
          String lang = tag.getAttributeValue("lang");
          ci.addData(lang,text,desc);
          }
        else {
          catflag = true; }
        }
      cat.add(ci);
//System.out.println(ci.toString());
      if (catflag) {
        pos[deep+1] = 0;
        pid[deep+1] = ci;
        list[deep+1] = catlist;
        deep++;
        }
      else {
        pos[deep]++; }
//System.out.println("deep="+deep+"  pos="+pos[deep]);
      }
//System.out.println("fertsch");
    }
   
  /**
   * The method create a MCRClassification from the given JDOM tree.
   *
   * @param jdom the classification as jdom tree
   **/
  public final String createFromJDOM( org.jdom.Document jdom ) 
    { 
    setFromJDOM(jdom);
    cl.create();
    for (int i=0;i<cat.size();i++) {
      ((MCRCategoryItem)cat.get(i)).create(); }
    return cl.getClassificationID();
    }
  
  /**
   * The method create a MCRClassification from the given XML array.
   *
   * @param xml the classification as byte array XML tree
   * @exception MCRException if the parser can't build a JDOM tree
   **/
  public final String createFromXML( byte [] xml ) throws MCRException
    { 
    try {
      org.jdom.input.SAXBuilder bulli = new org.jdom.input.SAXBuilder(false);
      org.jdom.Document jdom = bulli.build(new ByteArrayInputStream(xml));
      return createFromJDOM(jdom);
      }
    catch (Exception e) {
      throw new MCRException(e.getMessage()); }
    }
  
  /**
   * The method create a MCRClassification from the given URI.
   *
   * @param uri the classification URI
   * @exception MCRException if the parser can't build a JDOM tree
   **/
  public final String createFromURI( String uri ) throws MCRException
    { 
    try {
      org.jdom.input.DOMBuilder bulli = new org.jdom.input.DOMBuilder(false);
      org.jdom.Document jdom = bulli.build(MCRXMLHelper.parseURI(uri));
      return createFromJDOM(jdom);
      }
    catch (Exception e) {
      throw new MCRException(e.getMessage()); }
    }
  
  /**
   * The method delete the MCRClassification for the given ID.
   *
   * @param ID the classification ID to delete
   **/
  public final void delete( String ID ) 
    { 
    if (cl == null) { cl = new MCRClassificationItem(ID); }
    cl.delete(ID);
    }

  /**
   * The method update a MCRClassification from the given JDOM tree.
   *
   * @param jdom the classification as jdom tree
   **/
  public final String updateFromJDOM( org.jdom.Document jdom ) 
    { 
    org.jdom.Element root = jdom.getRootElement();
    String ID = (String)root.getAttribute("ID").getValue();
    MCRObjectID mcr_id = new MCRObjectID(ID);
    cl = new MCRClassificationItem(ID);
    cl.delete(cl.getClassificationID());
    setFromJDOM(jdom);
    cl.create();
    for (int i=0;i<cat.size();i++) {
      ((MCRCategoryItem)cat.get(i)).create(); }
    return cl.getClassificationID();
    }

  /**
   * The method update a MCRClassification from the given XML array.
   *
   * @param xml the classification as byte array XML tree
   * @exception MCRException if the parser can't build a JDOM tree
   **/
  public final String updateFromXML( byte [] xml ) throws MCRException
    { 
    try {
      org.jdom.input.SAXBuilder bulli = new org.jdom.input.SAXBuilder(false);
      org.jdom.Document jdom = bulli.build(new ByteArrayInputStream(xml));
      return updateFromJDOM(jdom);
      }
    catch (Exception e) {
      throw new MCRException(e.getMessage()); }
    }
  
  /**
   * The method update a MCRClassification from the given URI.
   *
   * @param uri the classification URI
   * @exception MCRException if the parser can't build a JDOM tree
   **/
  public final String updateFromURI( String uri ) throws MCRException
    { 
    try {
      org.jdom.input.DOMBuilder bulli = new org.jdom.input.DOMBuilder(false);
      org.jdom.Document jdom = bulli.build(MCRXMLHelper.parseURI(uri));
      return updateFromJDOM(jdom);
      }
    catch (Exception e) {
      throw new MCRException(e.getMessage()); }
    }
  
  /**
   * The method return the classification as JDOM tree.
   *
   * @param ID the classification ID to delete
   * @return the classification as JDOM
   **/
  public final org.jdom.Document receiveClassificationAsJDOM(String ID)
    {
    org.jdom.Element elm = new org.jdom.Element("mycoreclass");
    org.jdom.Document doc = new org.jdom.Document(elm);
    elm.setAttribute("ID",ID);
    elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi",
      MCRObject.XSI_URL));
    String SLASH = System.getProperty("file.separator");
    MCRConfiguration mcr_conf = MCRConfiguration.instance();
    String mcr_schema_path = mcr_conf.getString("MCR.root_path")+SLASH+"schema"
      +SLASH+"MCRClassification.xsd";
    elm.setAttribute("noNamespaceSchemaLocation",mcr_schema_path,
      org.jdom.Namespace.getNamespace("xsi",MCRObject.XSI_URL));
    try {
      cl = MCRClassificationItem.getClassificationItem(ID); }
    catch (Exception e) { return doc; }
//System.out.println(cl.toString());
    for (int i=0;i<cl.getSize();i++) {
      elm.addContent(cl.getJDOMElement(i)); }
    // get all categories
    MCRCategoryItem [] [] catlist = new MCRCategoryItem[MAX_CATEGORY_DEEP][];
    int [] pos = new int[MAX_CATEGORY_DEEP];
    int [] len = new int[MAX_CATEGORY_DEEP];
    org.jdom.Element [] list = new org.jdom.Element[MAX_CATEGORY_DEEP];
    catlist [0] = cl.getChildren();
    pos[0] = 0;
    len[0] = cl.getNumChildren();
    list[0] = new org.jdom.Element("categories");
    int deep = 0;
    while (deep > -1) {
      if (pos[deep] >= len[deep]) { 
        deep--; if(deep < 0) {break;}
        pos[deep]++; continue; }
      MCRCategoryItem ci = catlist[deep][pos[deep]];
//System.out.println(ci.toString());
      list[deep+1] = new org.jdom.Element("category");
      list[deep+1].setAttribute("ID",ci.getID());
      for (int i=0;i<ci.getSize();i++) {
        list[deep+1].addContent(ci.getJDOMElement(i)); }
      list[deep].addContent(list[deep+1]);
      if (ci.hasChildren()) {
//System.out.println(ci.getNumChildren());
        catlist [deep+1] = ci.getChildren();
        len [deep+1] = ci.getNumChildren();
        pos[deep+1] = 0;
        deep++;
        }
      else {
        pos[deep]++; }
      }
    elm.addContent(list[0]);

    return doc;
    }

  /**
   * The method return the classification as XML byte array.
   *
   * @param classID the classification ID 
   * @return the classification as XML
   **/
  public final byte[] receiveClassificationAsXML(String classID)
    {
    org.jdom.Document doc = receiveClassificationAsJDOM(classID);
    byte[] xml = MCRUtils.getByteArray(doc);
    return xml;
    }

  /**
   * The method return the category as XML byte array.
   *
   * @param classID the classification ID
   * @param categID the category ID
   * @return the classification as XML
   **/
  public final org.jdom.Document receiveCategoryAsJDOM(String classID, 
    String categID)
    {
    org.jdom.Element elm = new org.jdom.Element("mycoreclass");
    org.jdom.Document doc = new org.jdom.Document(elm);
    elm.setAttribute("ID",classID);
    // get the classification
    try {
      cl = MCRClassificationItem.getClassificationItem(classID); }
    catch (Exception e) { return doc; }
    for (int i=0;i<cl.getSize();i++) {
      elm.addContent(cl.getJDOMElement(i)); }
    // get the category
    MCRCategoryItem ci = cl.getCategoryItem(categID);
    org.jdom.Element cats = new org.jdom.Element("categories");
    org.jdom.Element cat = new org.jdom.Element("category");
    cat.setAttribute("ID",ci.getID());
    for (int i=0;i<ci.getSize();i++) {
      cat.addContent(ci.getJDOMElement(i)); }
    cats.addContent(cat);
    elm.addContent(cats);
    return doc;
    }

  /**
   * The method return the category as XML byte array.
   *
   * @param classID the classification ID
   * @param categID the category ID
   * @return the classification as XML
   **/
  public final byte[] receiveCategoryAsXML(String classID, String categID)
    {
    org.jdom.Document doc = receiveCategoryAsJDOM(classID,categID);
    byte[] xml = MCRUtils.getByteArray(doc);
    return xml;
    }

  /**
   * The method get a XQuery and responds a JDOM document.
   *
   * @param query the query string
   * @return a JDOM document
   **/
  public final org.jdom.Document search(String query)
    {
    // classification ID
    boolean cat = false;
    String classname = "";
    String categname = "";
    int classstart = query.indexOf("@ID");
    if (classstart == -1) { return null; }
    classstart = query.indexOf("\"",classstart);
    if (classstart == -1) { return null; }
    int classstop = query.indexOf("\"",classstart+1);
    if (classstop == -1) { return null; }
    classname = query.substring(classstart+1,classstop);
    // category ID
    int categstart = query.indexOf("@ID",classstop+1);
    int categstop = -1;
    if (categstart != -1) {
      categstart = query.indexOf("\"",categstart);
      if (categstart == -1) { return null; }
      categstop = query.indexOf("\"",categstart+1);
      if (categstop == -1) { return null; }
      categname = query.substring(categstart+1,categstop);
      cat = true;
      }
    System.out.println(classname+"   "+categname);
    if (cat) {
      return receiveCategoryAsJDOM(classname,categname); }
    else {
      return receiveClassificationAsJDOM(classname); }
    }
  }

