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

import org.jdom.Namespace;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;

/**
 * This class implements all method for handling with the MCRMetaLangText part
 * of a metadata object. The MCRMetaLangText class present a single item,
 * which has triples of a text and his corresponding language and optional a
 * type. 
 *
 * @author Jens Kupferschmidt
 * @author Johannes Bühler
 * @version $Revision$ $Date$
 **/
public class MCRMetaXML extends MCRMetaDefault 
  implements MCRMetaInterface 
{

// XML data
protected String xmlstream;

/**
 * This is the constructor. <br>
 * The language element was set to <b>en</b>.
 * All other elemnts was set to null;
 */
public MCRMetaXML()
  {
  super();
  xmlstream = "";
  }

/**
 * This is the constructor. <br>
 * The language element was set. If the value of <em>default_lang</em>
 * is null, empty or false <b>en</b> was set. The subtag element was set
 * to the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
 * is null or empty an exception was throwed. The type element was set to
 * the value of <em>set_type<em>, if it is null, an empty string was set
 * to the type element. The xml element was set to the value of 
 * <em>set_xml<em>.
 *
 * @param set_datapart     the global part of the elements like 'metadata'
 *                         or 'service'
 * @param set_subtag       the name of the subtag
 * @param default_lang     the default language
 * @param set_type         the optional type string
 * @param set_inherted     a value >= 0
 * @param set_xml          the XML stream as byte array
 * @exception MCRException if the set_subtag value is null or empty
 */
public MCRMetaXML(String set_datapart, String set_subtag, 
  String default_lang, String set_type, int set_inherted, String set_xml) 
  throws MCRException
  {
  super(set_datapart,set_subtag,default_lang,set_type,set_inherted);
  if (set_xml != null) xmlstream = set_xml;
  }

/**
 * This method set the XML stream. 
 *
 * @param set_xml         the XML stream as String
 **/
public final void setXML(String set_xml)
  { xmlstream = set_xml; }

/**
 * This method get the XML stream element.
 *
 * @return the XML as String
 **/
public final String getXML()
  { return xmlstream; }

/**
 * This method read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param element a relevant JDOM element for the metadata
 **/
public void setFromDOM(org.jdom.Element element)
  {
  super.setFromDOM(element);
  String temp = (element.getText()).trim();
  if (temp==null) { temp = ""; }
  xmlstream = temp;
  }

/**
 * This method create a XML stream for all data in this class, defined
 * by the MyCoRe XML MCRMetaLangText definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a JDOM Element with the XML MCRMetaLangText part
 **/
public org.jdom.Element createXML() throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content of MCRMetaXML is not valid."); }
  org.jdom.Element elm = new org.jdom.Element(subtag);
  elm.setAttribute("lang",lang,Namespace.XML_NAMESPACE);
  elm.setAttribute("inherited",(new Integer(inherited)).toString()); 
  if ((type != null) && ((type = type.trim()).length() !=0)) {
    elm.setAttribute("type",type); }
  elm.addContent(xmlstream);
  return elm;
  }

/**
 * This methode create a typed content list for all data in this instance.
 *
 * @param parasearch true if the data should parametric searchable
 * @exception MCRException if the content of this class is not valid
 * @return a MCRTypedContent with the data of the MCRObject data
 **/
public MCRTypedContent createTypedContent(boolean parasearch)
  throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  MCRTypedContent tc = new MCRTypedContent();
  if(!parasearch) { return tc; }
  tc.addTagElement(MCRTypedContent.TYPE_SUBTAG,subtag);
  tc.addXMLElement(MCRTypedContent.TYPE_VALUE,null,xmlstream);
  tc.addStringElement(MCRTypedContent.TYPE_ATTRIBUTE,"lang",lang);
  if ((type = type.trim()).length() !=0) {
    tc.addStringElement(MCRTypedContent.TYPE_ATTRIBUTE,"type",type); }
  return tc;
  }

/**
 * This methode create an empty String for all cases
 *
 * @param textsearch true if the data should text searchable
 * @exception MCRException if the content of this class is not valid
 * @return a String with the text value
 **/
public String createTextSearch(boolean textsearch)
  throws MCRException
  { return ""; }

/**
 * This method check the validation of the content of this class.
 * The method returns <em>true</em> if
 * <ul>
 * <li> the subtag is not null or empty
 * <li> the text is not null or empty
 * </ul>
 * otherwise the method return <em>false</em>
 *
 * @return a boolean value
 **/
public boolean isValid()
  {
  if (!super.isValid()) { return false; }
  if (xmlstream == null) { return false; }
  return true;
  }

/**
 * This method make a clone of this class.
 **/
public Object clone()
  {
  MCRMetaXML out = new MCRMetaXML(datapart,subtag,lang,type,inherited,
    xmlstream);
  return (Object)out;
  }

/**
 * This method put debug data to the logger (for the debug mode).
 **/
public final void debug()
  {
  logger.debug("Start Class : MCRMetaLangText");
  super.debugDefault();
  logger.debug("XML                = \n"+xmlstream);
  }

}

