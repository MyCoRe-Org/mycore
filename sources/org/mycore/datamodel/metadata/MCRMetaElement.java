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

import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;

/**
 * This class is designed to to have a basic class for all metadata.
 * The class has inside a ArrayList that holds all metaddata elements
 * for one XML tag.
 * Furthermore, this class supports the linking of a document owing this
 * metadata element to another document, the id of which is given in the
 * xlink:href attribute of the MCRMetaLink representing the link. The
 * class name of such a metadata element must be MCRMetaLink, and the
 * metadata element is considered to be a folder of links.
 *
 * @author Jens Kupferschmidt
 * @author Mathias Hegner
 * @version $Revision$ $Date$
 **/
public class MCRMetaElement
{

// common data
private static String NL =
  new String((System.getProperties()).getProperty("line.separator"));
public static String DEFAULT_LANGUAGE = "de";
public static boolean DEFAULT_PARAMETRIC_SEARCH = true;
public static boolean DEFAULT_TEXT_SEARCH = false;
public static boolean DEFAULT_HERITABLE = false;
public static boolean DEFAULT_NOT_INHERIT = false;
private String META_PACKAGE_NAME = "org.mycore.datamodel.metadata.";

// logger
static Logger logger=Logger.getLogger(MCRMetaElement.class.getName());

// MetaElement data
private String lang = null;
private String classname = null;
private String tag = null;
private boolean heritable;
private boolean notinherit;
private boolean parasearch;
private boolean textsearch;
private ArrayList list = null;

/**
 * This is the constructor of the MCRMetaElement class. 
 * The default language for the element was set to <b>en</b>.
 **/
public MCRMetaElement()
  {
  lang = DEFAULT_LANGUAGE; 
  classname = "";
  tag = "";
  parasearch = DEFAULT_PARAMETRIC_SEARCH;
  textsearch = DEFAULT_TEXT_SEARCH;
  heritable = DEFAULT_HERITABLE;
  notinherit = DEFAULT_NOT_INHERIT;
  list = new ArrayList();
  }

/**
 * This is the constructor of the MCRMetaElement class. 
 * The default language for the element was set. If the default languge
 * is empty or false <b>de</b> was set.
 *
 * @param default_lang     the default language
 **/
public MCRMetaElement(String default_lang)
  {
  if ((default_lang == null) || 
    ((default_lang = default_lang.trim()).length() ==0)) {
    lang = DEFAULT_LANGUAGE; }
  else {
    lang = default_lang; }
  classname = "";
  tag = "";
  parasearch = DEFAULT_PARAMETRIC_SEARCH;
  textsearch = DEFAULT_TEXT_SEARCH;
  heritable = DEFAULT_HERITABLE;
  notinherit = DEFAULT_NOT_INHERIT;
  list = new ArrayList();
  }

/**
 * This is the constructor of the MCRMetaElement class. 
 *
 * @param set_lang         the default language
 * @param set_classname    the name of the MCRMeta... class
 * @param set_tag          the name of this tag
 * @param set_parasearch   set this flag to true if you want to have this
 *                         element in a parametric search
 * @param set_textsearch   set this flag to true if you want to have this
 *                         element in a text search
 * @param set_hreitable    set this flag to true if all child objects of this 
 *                         element can inherit this data
 * @param set_notinherit   set this flag to true if this element should not
 *                         inherit from his parent object
 * @param set_list         a list of MCRMeta... data lines to add in this 
 *                         element
 **/
public MCRMetaElement(String set_lang, String set_classname, String set_tag,
  boolean set_parasearch, boolean set_textsearch, boolean set_hreitable,
  boolean set_notinherit, ArrayList set_list)
  {
  if ((set_lang != null) && ((set_lang = set_lang.trim()).length() !=0)) {
    lang = set_lang; }
  classname = ""; setClassName(set_classname);
  tag = ""; setTag(set_tag);
  parasearch = set_parasearch;
  textsearch = set_textsearch;
  heritable = set_hreitable;
  notinherit = set_notinherit;
  list = new ArrayList();
  if (set_list != null) {
    for (int i=0; i<set_list.size();i++) { list.add(set_list.get(i)); }
    }
  }

/**
 * This methode return the name of this metadata class as string.
 *
 * @return the name of this metadata class as string
 **/
public final String getClassName()
  { return classname; }

/**
 * This methode return the instance of an element from the list with index i.
 *
 * @return the instance of an element, if index is out of range return null
 **/
public final MCRMetaInterface getElement(int index)
  { 
  if ((index<0) || (index>list.size())) { return null; }
  return (MCRMetaInterface)list.get(index); 
  }

/**
 * This methode return the parametric search flag of this metadata as boolean 
 * value.
 *
 * @return the parametric search flag of this metadata class
 **/
public final boolean getParametricSearch()
  { return parasearch; }

/**
 * This methode return the text search flag of this metadata as boolean value.
 *
 * @return the text search flag of this metadata class
 **/
public final boolean getTextSearch()
  { return textsearch; }

/**
 * This methode return the heritable flag of this metadata as boolean value.
 *
 * @return the heritable flag of this metadata class
 **/
public final boolean getHeritable()
  { return heritable; }

/**
 * This methode return the nonherit flag of this metadata as boolean value.
 *
 * @return the notherit flag of this metadata class
 **/
public final boolean getNotInherit()
  { return notinherit; }

/**
 * This methode return the default language of this metadata class as string.
 *
 * @return the default language of this metadata class as string
 **/
public final String getLang()
  { return lang; }

/**
 * This methode return the tag of this metadata class as string.
 *
 * @return the tag of this metadata class as string
 **/
public final String getTag()
  { return tag; }

/**
 * This methode set the parametric search flag for the metadata class.
 *
 * @param parasearch           the parametric search flag as string
 */
public void setParametricSearch(String parasearch)
  {
  this.parasearch = DEFAULT_PARAMETRIC_SEARCH;
  if ((parasearch == null) || ((parasearch = parasearch.trim()).length() ==0))
    { return; }
  if (parasearch.toLowerCase().equals("true")) { 
    this.parasearch = true; return; }
  if (parasearch.toLowerCase().equals("false")) { 
    this.parasearch = false; return; }
  }

/**
 * This methode set the parametric search flag for the metadata class.
 *
 * @param parasearch           the parametric search flag as boolean value
 */
public void setParametricSearch(boolean parasearch)
  { this.parasearch = parasearch; }

/**
 * This methode set the  text search flag for the metadata class.
 *
 * @param textsearch           the text search flag as string
 */
public void setTextSearch(String textsearch)
  {
  this.textsearch = DEFAULT_TEXT_SEARCH;
  if ((textsearch == null) || ((textsearch = textsearch.trim()).length() ==0))
    { return; }
  if (textsearch.toLowerCase().equals("true")) { 
    this.textsearch = true; return; }
  if (textsearch.toLowerCase().equals("false")) { 
    this.textsearch = false; return; }
  }

/**
 * This methode set the text search flag for the metadata class.
 *
 * @param textsearch           the text search flag as boolean value
 */
public void setTextSearch(boolean textsearch)
  { this.textsearch = textsearch; }

/**
 * This methode set the heritable flag for the metadata class.
 *
 * @param heritable            the heritable flag as string
 */
public void setHeritable(String heritable)
  {
  this.heritable = DEFAULT_HERITABLE;
  if ((heritable == null) || ((heritable = heritable.trim()).length() ==0))
    { return; }
  if (heritable.toLowerCase().equals("true")) { 
    this.heritable = true; return; }
  if (heritable.toLowerCase().equals("false")) { 
    this.heritable = false; return; }
  }

/**
 * This methode set the heritable flag for the metadata class.
 *
 * @param heritable            the heritable flag as boolean value
 */
public void setHeritable(boolean heritable)
  { this.heritable = heritable; }

/**
 * This methode set the notinherit flag for the metadata class.
 *
 * @param notinherit           the notinherit flag as string
 */
public void setNotInherit(String notinherit)
  {
  this.notinherit = DEFAULT_NOT_INHERIT;
  if ((notinherit == null) || ((notinherit = notinherit.trim()).length() ==0))
    { return; }
  if (notinherit.toLowerCase().equals("true")) { 
    this.notinherit = true; return; }
  if (notinherit.toLowerCase().equals("false")) { 
    this.notinherit = false; return; }
  }

/**
 * This methode set the notinherit flag for the metadata class.
 *
 * @param notinherit           the notinherit flag as boolean value
 */
public void setNotInherit(boolean notinherit)
  { this.notinherit = notinherit; }

/**
 * This methode set the tag for the metadata class.
 *
 * @param tag                   the tag for the metadata class
 */
public void setTag(String tag)
  {
  if ((tag == null) || ((tag = tag.trim()).length() ==0)) { return; }
  this.tag = tag;
  }

/**
 * This methode set the element class name for the metadata elements.
 *
 * @param classname             the class name for the metadata elements
 */
public final void setClassName(String classname)
  {
  if ((classname == null) || ((classname = classname.trim()).length() ==0)) {
    return; }
  this.classname = classname;
  }

/**
 * <em>size</em> returns the number of elements in this instance.
 * 
 * @return int                  the size of "list"
 */
public final int size ()
{
	return list.size();
}

/**
 * The method add a metadata object, that implements the MCRMetaInterface
 * to this element.
 *
 * @param obj a metadata object
 * @exception MCRException if the class name of the object is not the same 
 * like the name of all store metadata in this element.
 **/
public final void addMetaObject(MCRMetaInterface obj) 
  { list.add(obj); }

/**
 * The method removes all inherited metadata objects of this MCRMetaElement.
 **/
public final void removeInheritedObject()
  {
  for (int i=0;i<size();i++) {
    if (((MCRMetaInterface)list.get(i)).getInherited() > 0) { list.remove(i); }
    }
  }

/**
 * This methode read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param element a relevant JDOM element for the metadata
 * @exception MCRException if the class can't loaded
 **/
public final void setFromDOM(org.jdom.Element element) throws MCRException
  {
  tag = element.getName();
  classname = element.getAttributeValue("class");
  String fullname = META_PACKAGE_NAME+classname;
  setHeritable(element.getAttributeValue("heritable"));
  setNotInherit(element.getAttributeValue("notinherit"));
  setParametricSearch(element.getAttributeValue("parasearch"));
  setTextSearch(element.getAttributeValue("textsearch"));
  List element_list = element.getChildren();
  int len = element_list.size();
  for (int i=0;i<len;i++) {
    org.jdom.Element subtag = (org.jdom.Element)element_list.get(i);
    Object obj = new Object();
    try {
      obj = Class.forName(fullname).newInstance();
      ((MCRMetaInterface)obj).setLang(lang);
      ((MCRMetaInterface)obj).setFromDOM(subtag);
      }
    catch (ClassNotFoundException e) {
      throw new MCRException(fullname+" ClassNotFoundException"); }
    catch (IllegalAccessException e) {
      throw new MCRException(fullname+" IllegalAccessException"); }
    catch (InstantiationException e) {
      throw new MCRException(fullname+" InstantiationException"); }
    list.add(obj);
    }
  }

/**
 * This methode create a XML stream for all data in this class, defined
 * by the MyCoRe XML MCRLangText definition for the given subtag.
 *
 * @param flag true if all inherited data should be include, else false
 * @exception MCRException if the content of this class is not valid
 * @return a JDOM Element with the XML Element part
 **/
public final org.jdom.Element createXML(boolean flag) throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("MCRMetaElement : The content is not valid."); }
  org.jdom.Element elm = new org.jdom.Element(tag);
  int j = 0;
  for (int i=0;i<list.size();i++) {
    if(((MCRMetaInterface)list.get(i)).getInherited() > 0); { j++; }
    }
  if ((j == 0) && (!flag)) { return elm; }
  elm.setAttribute("class",classname);
  elm.setAttribute("heritable",new Boolean(heritable).toString());
  elm.setAttribute("notinherit",new Boolean(notinherit).toString());
  elm.setAttribute("parasearch",new Boolean(parasearch).toString());
  elm.setAttribute("textsearch",new Boolean(textsearch).toString());
  for (int i=0;i<list.size();i++) {
    if (!flag) {
      if (((MCRMetaInterface)list.get(i)).getInherited() > 0) { continue; } }
    elm.addContent(((MCRMetaInterface)list.get(i)).createXML()); }
  return elm;
  }

/**
 * This methode create a typed content list for all data in this instance.
 *
 * @param flag true if all inherited data should be include, els false
 * @exception MCRException if the content of this class is not valid
 * @return a MCRTypedContent with the data of the MCRObject data
 **/
public final MCRTypedContent createTypedContent(boolean flag) 
  throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("MCRMetaElement : The content is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  if (!parasearch) { return tc; }
  int j = 0;
  for (int i=0;i<list.size();i++) {
    if(((MCRMetaInterface)list.get(i)).getInherited() > 0); { j++; }
    }
  if ((j == 0) && (!flag)) { return tc; }
  tc.addTagElement(MCRTypedContent.TYPE_TAG,tag);
  for (int i=0;i<list.size();i++) {
    if (!flag) {
      if (((MCRMetaInterface)list.get(i)).getInherited() > 0) { continue; } }
    tc.addMCRTypedContent(((MCRMetaInterface)list.get(i))
      .createTypedContent(parasearch)); }
  return tc;
  }

/**
 * This methode create a String for all text searchable data in this instance.
 *
 * @param flag true if all inherited data should be include, els false
 * @exception MCRException if the content of this class is not valid
 * @return a String with the searchable text
 **/
public final String createTextSearch(boolean flag)
  throws MCRException
  {
  if (!textsearch) { return ""; }
  StringBuffer sb = new StringBuffer(1024);
  int j = 0;
  for (int i=0;i<list.size();i++) {
    if(((MCRMetaInterface)list.get(i)).getInherited() > 0); { j++; }
    }
  if ((j == 0) && (!flag)) { return sb.toString(); }
  for (int i=0;i<list.size();i++) {
    if (!flag) {
      if (((MCRMetaInterface)list.get(i)).getInherited() > 0) { continue; } }
    sb.append(((MCRMetaInterface)list.get(i)).createTextSearch(textsearch))
      .append(' '); }
  return sb.toString();
  }

/**
 * This methode check the validation of the content of this class.
 * The methode returns <em>true</em> if
 * <ul>
 * <li> the classname is not null or empty
 * <li> the tag is not null or empty
 * <li> if the list is empty
 * <li> the lang value was supported
 * </ul>
 * otherwise the methode return <em>false</em>
 *
 * @return a boolean value
 **/
public final boolean isValid()
  {
  if ((classname == null) || ((classname = classname.trim()).length() ==0)) {
    return false; }
  if (list.size() == 0) { return false; }
  if ((tag == null) || ((tag = tag.trim()).length() ==0)) {
    return false; }
  if (!MCRUtils.isSupportedLang(lang)) { return false; }
  return true;
  }

/**
 * This method make a clone of this class.
 **/
public final Object clone()
  {
  MCRMetaElement out = new MCRMetaElement(lang);
  out.setClassName(classname);
  out.setTag(tag);
  out.setParametricSearch(parasearch);
  out.setTextSearch(textsearch);
  out.setHeritable(heritable);
  out.setNotInherit(notinherit);
  for (int i=0;i<size();i++) {
    MCRMetaInterface mif = (MCRMetaInterface)
      (((MCRMetaInterface)list.get(i)).clone());
    out.addMetaObject(mif);
    }
  return (Object)out;
  }

/**
 * This method put debug data to the logger (for the debug mode).
 **/
public final void debug()
  {
  // get the instance of MCRConfiguration
  MCRConfiguration config = MCRConfiguration.instance();
  // set the logger property
  PropertyConfigurator.configure(config.getLoggingProperties());
  // the output
  logger.debug("ClassName          = "+classname);
  logger.debug("Tag                = "+tag);
  logger.debug("Language           = "+lang);
  logger.debug("Heritable          = "+String.valueOf(heritable));
  logger.debug("NotInherit         = "+String.valueOf(notinherit));
  logger.debug("ParametricSearch   = "+String.valueOf(parasearch));
  logger.debug("TextSearch         = "+String.valueOf(textsearch));
  logger.debug("Elements           = "+String.valueOf(list.size()));
  }

}

