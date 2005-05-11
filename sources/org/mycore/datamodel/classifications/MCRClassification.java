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

package org.mycore.datamodel.classifications;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.metadata.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;

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

// logger
static Logger LOGGER=Logger.getLogger(MCRClassification.class);

// internal data
private static int MAX_CATEGORY_DEEP = 15;
private MCRClassificationItem cl;
private ArrayList cat;

  /**
   * The constructor
   **/
  public MCRClassification()
    { 
    MCRConfiguration config = MCRConfiguration.instance();
    }

  /**
   * The method fill the instance of this class with a given JODM tree.
   * 
   * @param jdom
   *                    the classification as jdom tree
   */
	private final void setFromJDOM(org.jdom.Document jdom) {
		cl = new MCRClassificationItem(new MCRObjectID(jdom.getRootElement()
				.getAttributeValue("ID")).getId());
		List tagList = jdom.getRootElement().getChildren("label");
		Element tag;
		for (int i = 0; i < tagList.size(); i++) {
			tag = (Element) tagList.get(i);
			cl.addData(tag.getAttributeValue("text"), tag
					.getAttributeValue("description"), tag
					.getAttributeValue("lang"));
		}
		LOGGER.debug("processing Classification:" + cl.toString());
		cat = new ArrayList();
		tagList = jdom.getRootElement().getChild("categories").getChildren(
				"category");
		for (int i = 0; i < tagList.size(); i++) {
			breakDownCategories((Element) tagList.get(i), cl);
		}
	}

	private void breakDownCategories(Element category,
			MCRClassificationObject parent) {
		//process labels
		MCRCategoryItem ci = new MCRCategoryItem(category
				.getAttributeValue("ID"), parent);
		List tagList = category.getChildren("label");
		Element element;
		for (int i = 0; i < tagList.size(); i++) {
			element = (Element) tagList.get(i);
			ci.addData(element.getAttributeValue("text"), element
					.getAttributeValue("description"), element
					.getAttributeValue("lang"));
		}
		//process url, if given
		element = category.getChild("url");
		if (element != null) {
			ci.setURL(element.getAttributeValue("href", Namespace.getNamespace(
					"xlink", MCRDefaults.XLINK_URL)));
		}
		cat.add(ci); //add to list of categories
		tagList = category.getChildren("category");
		for (int i = 0; i < tagList.size(); i++) {
			breakDownCategories((Element) tagList.get(i), ci); //process children
		}
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
      org.jdom.Document jdom = MCRXMLHelper.parseURI(uri);
      return createFromJDOM(jdom);
      }
    catch (Exception e) {
      throw new MCRException(e.getMessage(),e); }
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
    cl = new MCRClassificationItem(mcr_id.getId());
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
      org.jdom.Document jdom = MCRXMLHelper.parseURI(uri);
      return updateFromJDOM(jdom);
      }
    catch (Exception e) {
      throw new MCRException(e.getMessage(),e); }
    }
  
  /**
   * The method return the classification as JDOM tree.
   *
   * @param ID the classification ID to delete
   * @return the classification as JDOM
   **/
  public final org.jdom.Document receiveClassificationAsJDOM(String ID)
    {
    MCRLinkTableManager mcr_linktable = MCRLinkTableManager.instance();
    // create the XML tree
    org.jdom.Element elm = new org.jdom.Element("mycoreclass");
    org.jdom.Document doc = new org.jdom.Document(elm);
    elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi",
      MCRDefaults.XSI_URL));
    elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink",
      MCRDefaults.XLINK_URL));
    String mcr_schema_path = "MCRClassification.xsd";
    elm.setAttribute("noNamespaceSchemaLocation",mcr_schema_path,
      org.jdom.Namespace.getNamespace("xsi",MCRDefaults.XSI_URL));
    elm.setAttribute("ID",ID);
    try {
      cl = MCRClassificationItem.getClassificationItem(ID); }
    catch (Exception e) { return doc; }
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
      list[deep+1] = new org.jdom.Element("category");
      list[deep+1].setAttribute("ID",ci.getID());
      int cou = mcr_linktable.countCategoryReferencesFuzzy(ID,ci.getID());
      list[deep+1].setAttribute("counter",Integer.toString(cou));
      for (int i=0;i<ci.getSize();i++) {
        list[deep+1].addContent(ci.getJDOMElement(i)); }
      if (ci.getURL().length() != 0) {
        org.jdom.Element u = new org.jdom.Element("url");
        u.setAttribute("href",ci.getURL(),
         org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL));
        list[deep+1].addContent(u);
        }
      list[deep].addContent(list[deep+1]);
      if (ci.hasChildren()) {
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
    MCRLinkTableManager mcr_linktable = MCRLinkTableManager.instance();
    org.jdom.Element elm = new org.jdom.Element("mycoreclass");
    org.jdom.Document doc = new org.jdom.Document(elm);
    elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi",
      MCRDefaults.XSI_URL));
    elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink",
      MCRDefaults.XLINK_URL));
    elm.setAttribute("ID",classID);
    org.jdom.Element cats = new org.jdom.Element("categories");
    // get the classification
    try {
      cl = MCRClassificationItem.getClassificationItem(classID); }
    catch (Exception e) { cl = null; }
    if (cl != null) { 
      for (int i=0;i<cl.getSize();i++) { elm.addContent(cl.getJDOMElement(i)); }
      // get the category
      MCRCategoryItem ci = cl.getCategoryItem(categID);
      if (ci !=null) {
        org.jdom.Element cat = new org.jdom.Element("category");
        cat.setAttribute("ID",ci.getID());
        int cou = mcr_linktable.countCategoryReferencesFuzzy(classID, categID);
        cat.setAttribute("counter",Integer.toString(cou));
        for (int i=0;i<ci.getSize();i++) {
          cat.addContent(ci.getJDOMElement(i)); }
        if (ci.getURL().length() != 0) {
          org.jdom.Element u = new org.jdom.Element("url");
          u.setAttribute("href",ci.getURL(),
            org.jdom.Namespace.getNamespace("xlink",MCRDefaults.XLINK_URL));
          cat.addContent(u);
          }
        cats.addContent(cat);
        }
      }
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
    query = query.replace('\'','\"');
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
      if (categname.equals("*")) { cat = false; } else { cat = true; }
      }
    if (cat) {
      return receiveCategoryAsJDOM(classname,categname); }
    else {
      return receiveClassificationAsJDOM(classname); }
    }

  /**
   * The method return a category ID for the given category label of
   * a defined classification.
   *
   * @param classid the classification id as MCRObjectID
   * @param labeltext the text of a categoy label
   * @return the correspondig category ID
   **/
  public static final String getCategoryID(MCRObjectID classid, 
    String labeltext)
    {
    if (labeltext == null) { return ""; }
    if (labeltext.length() == 0) { return ""; }
    MCRClassificationItem cl = new MCRClassificationItem(classid);
    MCRCategoryItem cat = cl.getCategoryItemForLabelText(labeltext);
    if (cat == null) { return ""; }
    return cat.getID();
    }

 /**
   * The method returns all availiable classification ID's they are loaded.
   *
   * @return a list of classification ID's as String array
   **/
  public static final String [] getAllClassificationID()
    { 
    return (MCRClassificationItem.manager()).getAllClassificationID();
    }
  }

