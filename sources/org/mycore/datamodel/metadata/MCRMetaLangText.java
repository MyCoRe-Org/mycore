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
 * This class implements all method for handling with the MCRMetaLangText part
 * of a metadata object. The MCRMetaLangText class present a single item,
 * which has triples of a text and his corresponding language and optional a
 * type. 
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
 * The language element was set to <b>en</b>.
 * All other elemnts was set to an empty string.
 */
public MCRMetaLangText()
  {
  super();
  text = "";
  }

/**
 * This is the constructor. <br>
 * The language element was set. If the value of <em>default_lang</em>
 * is null, empty or false <b>en</b> was set. The subtag element was set
 * to the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
 * is null or empty an exception was throwed. The type element was set to
 * the value of <em>set_type<em>, if it is null, an empty string was set
 * to the type element. The text element was set to the value of 
 * <em>set_text<em>, if it is null, an empty string was set
 * to the text element.
 *
 * @param set_datapart     the global part of the elements like 'metadata'
 *                         or 'service'
 * @param set_subtag       the name of the subtag
 * @param default_lang     the default language
 * @param set_type         the optional type string
 * @param set_text         the text string
 * @exception MCRException if the set_subtag value is null or empty
 */
public MCRMetaLangText(String set_datapart, String set_subtag, 
  String default_lang, String set_type, String set_text) throws MCRException
  {
  super(set_datapart,set_subtag,default_lang,set_type);
  text = "";
  if (set_text != null) { text = set_text.trim(); }
  }

/**
 * This method set the languge, type and text. 
 *
 * @param set_lang        the new language string, if this is null or
 *                        empty, nothing is to do
 * @param set_type        the optional type syting
 * @param set_text        the new text string
 **/
public final void set(String set_lang, String set_type, String set_text)
  {
  setLang(set_lang);
  setType(set_type);
  if (set_text != null) { text = set_text.trim(); }
  }

/**
 * This method set the text. 
 *
 * @param set_text        the new text string
 **/
public final void setText(String set_text)
  { if (set_text != null) { text = set_text.trim(); } }

/**
 * This method get the text element.
 *
 * @return the text
 **/
public final String getText()
  { return text; }

/**
 * This method read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param metadata_langtext_node a relevant DOM element for the metadata
 **/
public final void setFromDOM(Node metadata_langtext_node)
  {
  super.setFromDOM(metadata_langtext_node);
  Node langtext_textelement = metadata_langtext_node.getFirstChild();
  String temp_text = ((Text)langtext_textelement).getData().trim();
  if (temp_text==null) { temp_text = ""; }
  text = temp_text;
  }

/**
 * This method create a XML stream for all data in this class, defined
 * by the MyCoRe XML MCRMetaLangText definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a XML string with the XML MCRMetaLangText part
 **/
public final String createXML() throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  StringBuffer sb = new StringBuffer(1024);
  sb.append('<').append(subtag).append(" xml:lang=\"").append(lang)
    .append("\"");
  if ((type != null) && ((type = type.trim()).length() !=0)) {
    sb.append(" type=\"").append(type).append("\""); }
  sb.append(">").append(NL);
  sb.append(text);
  sb.append("</").append(subtag).append('>').append(NL);
  return sb.toString();
  }

/**
 * This method create a Text Search stream for all data in this class, defined
 * by the MyCoRe TS MCRMetaLangText definition for the given tag and subtag.
 * The content of this stream is depended by the implementation for
 * the persistence database. It was choose over the
 * <em>MCR.persistence_type</em> configuration.
 *
 * @param mcr_query   a class they implement the <b>MCRQueryInterface</b>
 * @param tag                the tagname of an element list
 * @exception MCRException if the content of this class is not valid
 * @return a TS string with the TS MCRMetaLangText part
 **/
public final String createTS(Object mcr_query, String tag) throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  String [] sattrib = null;
  String [] svalue = null;
  if ((type != null) && ((type = type.trim()).length() !=0)) {
    sattrib = new String[1]; sattrib[0] = "type";
    svalue = new String[1]; svalue[0] = type; 
    }
  StringBuffer sb = new StringBuffer(1024);
  sb.append(((MCRQueryInterface)mcr_query).createSearchStringText(datapart,
    tag,subtag,sattrib,svalue,null,null,null,text));
  return sb.toString();
  }

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
public final boolean isValid()
  {
  if (!super.isValid()) { return false; }
  if ((text == null) || ((text = text.trim()).length() ==0)) {
    return false; }
  return true;
  }

/**
 * This method print all data content from the MCRMetaLangText class.
 **/
public final void debug()
  {
  System.out.println("MCRMetaLangText debug start:");
  super.debug();
  if (text.trim().length()!=0) { System.out.println(text); }
  System.out.println("MCRMetaLangText debug end"+NL);
  }

}

