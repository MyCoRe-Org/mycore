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

import java.util.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import mycore.common.MCRConfiguration;
import mycore.common.MCRConfigurationException;
import mycore.common.MCRException;
import mycore.datamodel.MCRMetaElement;

/**
 * This class implements all methode for handling with the LangText part
 * of a metadata object. The LangText class present a vector of items,
 * which has peers of a text and his corresponding language. The maximum
 * length of the vector is set in the property configuration.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
final public class MCRMetaLangText extends MCRMetaElement 
  implements MCRMetaInterface
{

// MetaLangText data
private String subtag;
private int vec_max_length = 1;
private Vector text;
private Vector lang;

/**
 * This is the constructor of the MCRMetaLangText class. <br>
 * The methode set the default vector length to the value from the
 * configuration property <em>MCR.metadata_langtext_vec_max_length</em>.
 */
public MCRMetaLangText()
  {
  subtag = "";
  vec_max_length = MCRConfiguration.instance()
    .getInt("MCR.metadata_langtext_vec_max_length",1);
  text = new Vector(vec_max_length);
  lang = new Vector(vec_max_length);
  }

/**
 * This is the constructor of the MCRMetaLangText class. <br>
 * The methode set the default vector length to the value from the
 * configuration property <em>MCR.metadata_langtext_vec_max_length</em>.
 *
 * @param default_lang          the default language
 */
public MCRMetaLangText(String default_lang)
  {
  subtag = "";
  setDefaultLang(default_lang);
  vec_max_length = MCRConfiguration.instance()
    .getInt("MCR.metadata_langtext_vec_max_length",1);
  text = new Vector(vec_max_length);
  lang = new Vector(vec_max_length);
  }

/**
 * This methode add a languge/text pair to the list. 
 *
 * @param replang                the new language string, if this is null or
 *                               empty, the default language was set
 * @param reptex                 the new text string
 * @exception IndexOutOfBoundsException throw this exception, if
 *                              the vector is full
 **/
public final void addElement(String replang, String reptext)
  throws IndexOutOfBoundsException
  {
  if (lang.size()>=vec_max_length) {
    throw new IndexOutOfBoundsException("Index error in addElement."); }
  if ((replang == null) || ((replang = replang.trim()).length() ==0)) {
    lang.addElement(default_lang); }
  else {
    lang.addElement(replang); }
  text.addElement(reptext);
  }

/**
 * This methode get a language element from the vector for the given index.
 *
 * @param index                 the index id of the element
 * @exception IndexOutOfBoundsException throw this exception, if
 *                              the index is false
 **/
public final String getLangElement(int index) throws IndexOutOfBoundsException
  {
  if ((index<0)||(index>lang.size())) {
    throw new IndexOutOfBoundsException("Index error in getLangElement."); }
  return (String)lang.elementAt(index);  
  }

/**
 * This methode get a text element from the vector for the given index.
 *
 * @param index                 the index id of the element
 * @exception IndexOutOfBoundsException throw this exception, if
 *                              the index is false
 **/
public final String getTextElement(int index) throws IndexOutOfBoundsException
  {
  if ((index<0)||(index>lang.size())) {
    throw new IndexOutOfBoundsException("Index error in getTextElement."); }
  return (String)text.elementAt(index);  
  }

/**
 * This methode remove a languge/text pair from a given index of the list. 
 *
 * @param index                  a index of the list
 * @exception IndexOutOfBoundsException throw this exception, if
 *                              the index is false
 **/
public final void removeElement(int index) throws IndexOutOfBoundsException
  {
  if ((index<0)||(index>lang.size())) {
    throw new IndexOutOfBoundsException("Index error in removeElement."); }
  lang.removeElementAt(index);
  text.removeElementAt(index);
  }

/**
 * This methode replace a languge/text pair for a given index of the list. 
 *
 * @param index                  a index of the list
 * @param replang                the new language string, if this is null or
 *                               empty, the default language was set
 * @param reptex                 the new text string
 * @exception IndexOutOfBoundsException throw this exception, if
 *                              the index is false
 **/
public final void replaceElement(int index, String replang, String reptext)
  throws IndexOutOfBoundsException
  {
  if ((index<0)||(index>lang.size())) {
    throw new IndexOutOfBoundsException("Index error in replaceElement."); }
  if ((replang == null) || ((replang = replang.trim()).length() ==0)) {
    lang.setElementAt(default_lang,index); }
  else {
    lang.setElementAt(replang,index); }
  text.setElementAt(reptext,index);
  }

/**
 * This methode read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param dom_element_list       a relevant DOM element for the metadata
 **/
public final void setFromDOM(Node metadata_langtext_node)
  {
  String temp_lang = null, temp_text = null;
  setTag(metadata_langtext_node.getNodeName());
  Element langtext_element = (Element)metadata_langtext_node;
  class_name =  langtext_element.getAttribute("type");
  String here_string = langtext_element.getAttribute("hereditary");
  if (here_string == null) { here_string = ""; }
  setHereditary(here_string);
  NodeList langtext_subelement_list = langtext_element.getChildNodes();
  int langtext_subelement_counter = langtext_subelement_list.getLength();
  for (int i = 0; i < langtext_subelement_counter; i++) {
    Node langtext_item = langtext_subelement_list.item(i);
    if (i==1) { subtag = langtext_item.getNodeName(); }
    if (langtext_item.getNodeType() != Node.ELEMENT_NODE) { continue; }
    temp_lang = ((Element)langtext_item).getAttribute("xml:lang");
    if (temp_lang==null) { temp_lang = default_lang; }
    lang.addElement(temp_lang);
    Node langtext_textelement = langtext_item.getFirstChild();
    temp_text = ((Text)langtext_textelement).getData().trim();
    if (temp_text==null) {
      temp_text = ""; }
    text.addElement(temp_text);
    }
  }

/**
 * This methode create a XML stream for all data in this class, defined
 * by the MyCoRe XML LangText definition for the given tag and subtag.
 *
 * @return a XML string with the XML LangText part
 **/
public final String createXML()
  {
  StringBuffer sb = new StringBuffer(1024);
  sb.append('<').append(tag).append(" type=\"MCRMetaLangText\" hereditary=\"")
    .append(hereditary).append("\">").append(NL);
  for (int i=0;i<text.size();i++) {
    sb.append('<').append(subtag).append(" xml:lang=\"")
      .append(lang.elementAt(i)).append("\">").append(NL);
    sb.append(text.elementAt(i));
    sb.append("</").append(subtag).append('>').append(NL);
    }
  sb.append("</").append(tag).append('>').append(NL);
  return sb.toString();
  }

/**
 * This methode create a Text Search stream for all data in this class, defined
 * by the MyCoRe XML LangText definition for the given tag and subtag.
 * The content of this stream is depended by the implementation for
 * the persistence database. It was choose over the
 * <em>MCR.persistence_type</em> configuration.
 *
 * @param mcr_query   a class they implement the <b>MCRQueryInterface</b>
 * @return a XML string with the XML LangText part
 **/
public final String createTS(Object mcr_query)
  {
  StringBuffer sb = new StringBuffer(1024);
  for (int i=0;i<text.size();i++) {
    sb.append(((MCRQueryInterface)mcr_query).createSearchStringText(subtag,
        (String)text.elementAt(i)));
    }
  return sb.toString();
  }

/**
 * This metode print all data content from the MetaLangText vector.
 **/
public final void debug()
  {
  System.out.println("metadata class name            = "+class_name);
  System.out.println("metadata tag                   = "+tag);
  System.out.println("metadata subtag                = "+subtag);
  System.out.println("metadata hereditary            = "+hereditary);
  for (int i=0;i<text.size();i++) {
    System.out.println("--- text with lang "+lang.elementAt(i)+" --->");
    System.out.println(text.elementAt(i));
    System.out.println("-------->");
    }
  System.out.println();
  }

}

