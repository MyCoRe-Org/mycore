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
import org.w3c.dom.Text;
import mycore.common.MCRException;

/**
 * This class implements all methode for handling with the MCRMetaLangText part
 * of a metadata object. The MCRMetaLangText class present a single item,
 * which has peers of a text and his corresponding language. 
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
final public class MCRMetaLangText extends MCRMetaDefault 
  implements MCRMetaInterface 
{

// MetaLangText data
private String text;

/**
 * This is the constructor. <br>
 * The default language for the element was set to <b>en</b>.
 */
public MCRMetaLangText()
  {
  lang = DEFAULT_LANGUAGE;
  subtag = "";
  text = "";
  }

/**
 * This is the constructor. <br>
 * The default language for the element was set. If the default languge
 * is empty or false <b>en</b> was set.
 *
 * @param default_lang     the default language
 */
public MCRMetaLangText(String default_lang)
  {
  if ((default_lang == null) || 
    ((default_lang = default_lang.trim()).length() ==0)) {
    lang = DEFAULT_LANGUAGE; }
  else {
    lang = default_lang; }
  subtag = "";
  text = "";
  }

/**
 * This is the constructor. <br>
 * The default language for the element was set. If the default languge
 * is empty or false <b>en</b> was set.
 * The subtag was set to the value of set_subtag.
 *
 * @param set_subtag       the name of the subtag
 * @param default_lang     the default language
 * @exception MCRException if the set_subtag value is null or empty
 */
public MCRMetaLangText(String set_subtag, String default_lang)
  throws MCRException
  {
  if ((set_subtag == null) || ((set_subtag = set_subtag.trim()).length() ==0)) {
    throw new MCRException("MCRMetaLangText : The set_subtag is"+
      " null or empty."); }
  if ((default_lang == null) ||
    ((default_lang = default_lang.trim()).length() ==0)) {
    lang = DEFAULT_LANGUAGE; }
  else {
    lang = default_lang; }
  subtag = set_subtag;
  text = "";
  }

/**
 * This methode set a languge/text pair. 
 *
 * @param set_lang        the new language string, if this is null or
 *                        empty, nothing is to do
 * @param set_text        the new text string
 **/
public final void set(String set_lang, String set_text)
  {
  if ((set_lang != null) && ((set_lang = set_lang.trim()).length() !=0)) {
    lang = set_lang; }
  text = set_text;
  }

/**
 * This methode get the text element.
 *
 * @return the text
 **/
public final String getText()
  { return text; }

/**
 * This methode read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param dom_element_list       a relevant DOM element for the metadata
 **/
public final void setFromDOM(Node metadata_langtext_node)
  {
  subtag = metadata_langtext_node.getNodeName(); 
  String temp_lang = ((Element)metadata_langtext_node).getAttribute("xml:lang");
  if (temp_lang!=null) { lang = temp_lang; }
  type = ((Element)metadata_langtext_node).getAttribute("type");
  Node langtext_textelement = metadata_langtext_node.getFirstChild();
  String temp_text = ((Text)langtext_textelement).getData().trim();
  if (temp_text==null) { temp_text = ""; }
  text = temp_text;
  }

/**
 * This methode create a XML stream for all data in this class, defined
 * by the MyCoRe XML MCRMetaLangText definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a XML string with the XML MCRMetaLangText part
 **/
public final String createXML() throws MCRException
  {
  if (!isValid()) {
    throw new MCRException("MCRMetaLangText : The content is not valid."); }
  StringBuffer sb = new StringBuffer(1024);
  sb.append('<').append(subtag).append(" xml:lang=\"").append(lang);
  if ((type != null) && ((type = type.trim()).length() !=0)) {
    sb.append("\" type=\"").append(type); }
  sb.append("\">").append(NL);
  sb.append(text);
  sb.append("</").append(subtag).append('>').append(NL);
  return sb.toString();
  }

/**
 * This methode create a Text Search stream for all data in this class, defined
 * by the MyCoRe TS MCRMetaLangText definition for the given tag and subtag.
 * The content of this stream is depended by the implementation for
 * the persistence database. It was choose over the
 * <em>MCR.persistence_type</em> configuration.
 *
 * @param mcr_query   a class they implement the <b>MCRQueryInterface</b>
 * @exception MCRException if the content of this class is not valid
 * @return a TS string with the TS MCRMetaLangText part
 **/
public final String createTS(Object mcr_query) throws MCRException
  {
  if (!isValid()) {
    throw new MCRException("MCRMetaLangText : The content is not valid."); }
  StringBuffer sb = new StringBuffer(1024);
  if ((type != null) && ((type = type.trim()).length() !=0)) {
    sb.append(((MCRQueryInterface)mcr_query).createSearchStringAttrib(subtag,
      "type",type)); }
  sb.append(((MCRQueryInterface)mcr_query).createSearchStringText(subtag,
    text));
  return sb.toString();
  }

/**
 * This methode check the validation of the content of this class.
 * The methode returns <em>true</em> if
 * <ul>
 * <li> the subtag is not null or empty
 * <li> the text is not null or empty
 * </ul>
 * otherwise the methode return <em>false</em>
 *
 * @return a boolean value
 **/
public final boolean isValid()
  {
  if ((text == null) || ((text = text.trim()).length() ==0)) {
    return false; }
  if ((subtag == null) || ((subtag = subtag.trim()).length() ==0)) {
    return false; }
  return true;
  }

/**
 * This metode print all data content from the MCRMetaLangText class.
 **/
public final void debug()
  {
  System.out.println("--- text for "+subtag+" with lang "+lang+" and type "+
    type+" ---");
  System.out.println(text);
  System.out.println("--- ---");
  }

}

