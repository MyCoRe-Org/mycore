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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import mycore.common.MCRException;

/**
 * This class implements all methods for handling with the MCRMetaCorporation part
 * of a metadata object. The MCRMetaCorporation class represents a natural corporation
 * specified by a list of names. 
 *
 * @author J. Vogler
 * @version $Revision$ $Date$
 **/
final public class MCRMetaCorporation extends MCRMetaDefault 
  implements MCRMetaInterface 
{

// MetaCorporation data
private String name;
private String nickname;
private String parent;
private String property;

/**
 * This is the constructor. <br>
 * The language element was set to <b>en</b>.
 * All other elemnts are set to an empty string.
 */
public MCRMetaCorporation()
  {
  super();
  name      = "";
  nickname  = "";
  parent    = "";
  property  = "";
  }

/**
 * This is the constructor. <br>
 * The language element was set. If the value of <em>default_lang</em>
 * is null, empty or false <b>en</b> was set. The subtag element was set
 * to the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
 * is null or empty an exception was throwed. The type element was set to
 * the value of <em>set_type<em>, if it is null, an empty string was set
 * to the type element. The name, nickname, parent  and property element
 * was set to the value of <em>set_...<em>, if they are null,
 * an empty string was set to this element.
 *
 * @param set_datapart    the global part of the elements like 'metadata'
 *                        or 'service'
 * @param set_subtag      the name of the subtag
 * @param default_lang    the default language
 * @param set_type        the optional type string
 * @param set_name        the first name
 * @param set_nickname    the call name
 * @param set_parent      the sure name
 * @param set_property       the property title
 * @exception MCRException if the parameter values are invalid
 **/
public MCRMetaCorporation(String set_datapart, String set_subtag, 
  String default_lang, String set_type, String set_name, String set_nickname,
  String set_parent, String set_property) throws MCRException
  {
  super(set_datapart,set_subtag,default_lang,set_type);
  name      = "";
  nickname  = "";
  parent    = "";
  property  = "";
  set(set_name,set_nickname,set_parent,set_property);
  }

/**
 * This methode set all name componets. 
 *
 * @param set_name        the first name
 * @param set_nickname    the call name
 * @param set_parent      the sure name
 * @param set_property       the property title
 **/
public final void set(String set_name, String set_nickname, String 
  set_parent, String set_property)
  {
  if ((set_name    == null) || (set_nickname == null) ||
      (set_parent  == null) || (set_property    == null))   {
    throw new MCRException("One parameter is null."); }
  name      = set_name.trim();
  nickname  = set_nickname.trim();
  parent    = set_parent.trim();
  property  = set_property.trim();
  }

/**
 * This method get the name text element.
 *
 * @return the name
 **/
public final String getName()
  { return name; }

/**
 * This method get the nickname text element.
 *
 * @return the nickname
 **/
public final String getNickname()
  { return nickname; }

/**
 * This method get the parent text element.
 *
 * @return the parent
 **/
public final String getParent()
  { return parent; }

/**
 * This method get the property text element.
 *
 * @return the property
 **/
public final String getProperty()
  { return property; }


/**
 * This method reads the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param metadata_corporation_node       a relevant DOM element for the metadata
 **/
public final void setFromDOM(Node metadata_corporation_node)
  {
  super.setFromDOM(metadata_corporation_node);
  NodeList childs_name_nodelist = metadata_corporation_node.getChildNodes();
  name      = seekElementText("name",      childs_name_nodelist);
  nickname  = seekElementText("nickname",  childs_name_nodelist);
  parent    = seekElementText("parent",    childs_name_nodelist);
  property  = seekElementText("property",  childs_name_nodelist);
  }

private final String seekElementText(String seek_element, 
  NodeList childs_nodelist)
  {
  String temp_child_node_name;
  String seek_element_text = "";
  int length_childs_nodelist = childs_nodelist.getLength();
  for (int i = 0; i < length_childs_nodelist; i++) {
    temp_child_node_name = childs_nodelist.item(i).getNodeName();
    if ( temp_child_node_name.equals(seek_element) ) {
      Node seek_node  = childs_nodelist.item(i);
      NodeList seek_childs_nodelist = seek_node.getChildNodes();
      Node text_node  = seek_childs_nodelist.item(0);
      int text_node_type   = text_node.getNodeType();
      if ( text_node_type  == Node.TEXT_NODE ) {
        seek_element_text = text_node.getNodeValue().trim();
        return seek_element_text.trim(); }
      }
    }
  return seek_element_text;
  } 

/**
 * This method creates a XML stream for all data in this class, defined
 * by the MyCoRe XML MCRMetaCorporation definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a XML string with the XML MCRMetaCorporation part
 **/
public final String createXML() throws MCRException
  {
  if (!isValid()) {
    throw new MCRException("The content is not valid."); }
  StringBuffer sb = new StringBuffer(1024);
  sb.append("<").append(subtag).append(" type=\"").append(type)
    .append("\" xml:lang=\"").append(lang).append("\" >").append(NL);
  if ((name      = name    .trim()).length()   !=0) {
    sb.append("<name>")    .append(name)    .append("</name>").append(NL);     }
  if ((nickname  = nickname.trim()).length()   !=0) {
    sb.append("<nickname>").append(nickname).append("</nickname>").append(NL); }
  if ((parent    = parent  .trim()).length()   !=0) {
    sb.append("<parent>")  .append(parent)  .append("</parent>").append(NL);   }
  if ((property     = property   .trim()).length()   !=0) {
    sb.append("<property>").append(property).append("</property>").append(NL);    }
  sb.append("</").append(subtag).append(">").append(NL); 
  return sb.toString();
  }

/**
 * This methode create a typed content list for all data in this instance.
 *
 * @param parametric true if the data should parametric searchable
 * @param textsearch true if the data should text searchable
 * @exception MCRException if the content of this class is not valid
 * @return a MCRTypedContent with the data of the MCRObject data
 **/
public final MCRTypedContent createTypedContent(boolean parametric,
  boolean textsearch) throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  tc.addTagElement(tc.TYPE_SUBTAG,subtag);
  if ((name = name.trim()).length() !=0) {
    tc.addTagElement(tc.TYPE_SUB2TAG,"name");
    tc.addStringElement(tc.TYPE_VALUE,null,name,parametric,textsearch);
    tc.addStringElement(tc.TYPE_ATTRIBUTE,"xml:lang",lang,parametric,
      textsearch);
    if ((type = type.trim()).length() !=0) {
      tc.addStringElement(tc.TYPE_ATTRIBUTE,"type",type,parametric,
        textsearch); }
    }
  if ((nickname = nickname.trim()).length() !=0) {
    tc.addTagElement(tc.TYPE_SUB2TAG,"nickname");
    tc.addStringElement(tc.TYPE_VALUE,null,nickname,parametric,textsearch);
    tc.addStringElement(tc.TYPE_ATTRIBUTE,"xml:lang",lang,parametric,
      textsearch);
    if ((type = type.trim()).length() !=0) {
      tc.addStringElement(tc.TYPE_ATTRIBUTE,"type",type,parametric,
        textsearch); }
    }
  if ((parent = parent.trim()).length() !=0) {
    tc.addTagElement(tc.TYPE_SUB2TAG,"parent");
    tc.addStringElement(tc.TYPE_VALUE,null,parent,parametric,textsearch);
    tc.addStringElement(tc.TYPE_ATTRIBUTE,"xml:lang",lang,parametric,
      textsearch);
    if ((type = type.trim()).length() !=0) {
      tc.addStringElement(tc.TYPE_ATTRIBUTE,"type",type,parametric,
        textsearch); }
    }
  if ((property = property.trim()).length() !=0) {
    tc.addTagElement(tc.TYPE_SUB2TAG,"property");
    tc.addStringElement(tc.TYPE_VALUE,null,property,parametric,textsearch);
    tc.addStringElement(tc.TYPE_ATTRIBUTE,"xml:lang",lang,parametric,
      textsearch);
    if ((type = type.trim()).length() !=0) {
      tc.addStringElement(tc.TYPE_ATTRIBUTE,"type",type,parametric,
        textsearch); }
    }
  return tc;
  }

/**
 * This method checks the validation of the content of this class.
 * The method returns <em>false</em> if
 * <ul>
 * <li> the name      is empty and
 * <li> the nickname  is empty and
 * <li> the parent    is empty and
 * <li> the property     is empty and
 * </ul>
 * otherwise the method returns <em>true</em>.
 *
 * @return a boolean value
 **/
public final boolean isValid()
  {
  if (((name     = name     .trim()).length()  ==0) && 
      ((nickname = nickname .trim()).length()  ==0) &&
      ((parent   = parent   .trim()).length()  ==0) &&
      ((property    = property    .trim()).length()  ==0)) {
    return false; }
  return true;
  }

/**
 * This metod prints all data content from the MCRMetaCorporation class.
 **/
public final void debug()
  {
  System.out.println("MCRMetaCorporation debug start:");
  super.debug();
  System.out.println("<name>"+name+"</name>");
  System.out.println("<nickname>"+nickname+"</nickname>");
  System.out.println("<parent>"+parent+"</parent>");
  System.out.println("<property>"+property+"</property>");
  System.out.println("MCRMetaCorporation debug end"+NL);
  }

}

