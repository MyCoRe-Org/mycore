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
 * This class implements all method for handling with the MCRMetaBoolean part
 * of a metadata object. The MCRMetaBoolean class present a logical value
 * of true or false and optional a type. 
 * <p>
 * &lt;tag class="MCRMetaBoolean" heritable="..."&gt;<br>
 * &lt;subtag type="..."&gt;<br>
 * true|false or yes|no or ja|nein or wahr|falsch
 * &lt;/subtag&gt;<br>
 * &lt;/tag&gt;<br>
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
final public class MCRMetaBoolean extends MCRMetaDefault 
  implements MCRMetaInterface 
{

// MCRMetaBoolean data
private boolean value;

/**
 * This is the constructor. <br>
 * The language element was set to <b>en</b>.
 * The boolean value was set to false.
 */
public MCRMetaBoolean()
  {
  super();
  value = false;
  }

/**
 * This is the constructor. <br>
 * The language element was set to <b>en</b>.
 * The subtag element was set
 * to the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
 * is null or empty an exception was throwed. The type element was set to
 * the value of <em>set_type<em>, if it is null, an empty string was set
 * to the type element. The boolean string <em>set_value<em>
 * was set to a boolean element, if it is null, false was set.
 *
 * @param set_datapart     the global part of the elements like 'metadata'
 *                         or 'service'
 * @param set_subtag       the name of the subtag
 * @param set_type         the optional type string
 * @param set_value        the boolean value (true or false) as string
 * @exception MCRException if the set_subtag value is null or empty
 */
public MCRMetaBoolean(String set_datapart, String set_subtag, 
  String default_lang, String set_type, String set_value) throws MCRException
  {
  super(set_datapart,set_subtag,"en",set_type);
  if (set_value != null) { 
    set_value = set_value.toLowerCase().trim();
    if (set_value.equals("true")) { value = true; return; }
    if (set_value.equals("ja")) { value = true; return; }
    if (set_value.equals("yes")) { value = true; return; }
    if (set_value.equals("wahr")) { value = true; return; }
    value = false; 
    }
  }

/**
 * This is the constructor. <br>
 * The language element was set to <b>en</b>.
 * The subtag element was set
 * to the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
 * is null or empty an exception was throwed. The type element was set to
 * the value of <em>set_type<em>, if it is null, an empty string was set
 * to the type element. The boolean element was set to the value of 
 * <em>set_value<em>, if it is null, false was set.
 *
 * @param set_datapart     the global part of the elements like 'metadata'
 *                         or 'service'
 * @param set_subtag       the name of the subtag
 * @param set_type         the optional type string
 * @param set_value        the boolean value
 * @exception MCRException if the set_subtag value is null or empty
 */
public MCRMetaBoolean(String set_datapart, String set_subtag, 
  String set_type, boolean set_value) throws MCRException
  {
  super(set_datapart,set_subtag,"en",set_type);
  value = set_value; 
  }

/**
 * This method set value. It set false if the string is corrupt.
 *
 * @param set_value       the boolean value (true or false) as string
 **/
public final void setValue(String set_value)
  {
  set_value = set_value.toLowerCase().trim();
  if (set_value != null) { 
    set_value = set_value.toLowerCase().trim();
    if (set_value.equals("true")) { value = true; return; }
    if (set_value.equals("ja")) { value = true; return; }
    if (set_value.equals("yes")) { value = true; return; }
    if (set_value.equals("wahr")) { value = true; return; }
    value = false; 
    }
  }

/**
 * This method set the value. 
 *
 * @param set_value       the boolean value
 **/
public final void setValue(boolean set_value)
  { value = set_value; }

/**
 * This method get the value element.
 *
 * @return the value as Boolean
 **/
public final boolean getValue()
  { return value; }

/**
 * This method get the value element as String.
 *
 * @return the value as String
 **/
public final String getValueToString()
  { return (new Boolean(value).toString()); }

/**
 * This method read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param metadata_boolean_node a relevant DOM element for the metadata
 **/
public final void setFromDOM(Node metadata_boolean_node)
  {
  super.setFromDOM(metadata_boolean_node);
  Node boolean_textelement = metadata_boolean_node.getFirstChild();
  String temp_value = ((Text)boolean_textelement).getData().trim();
  if (temp_value==null) { value = false; return; }
  setValue(temp_value);
  }

/**
 * This method create a XML stream for all data in this class, defined
 * by the MyCoRe XML MCRBoolean definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a XML string with the XML MCRBoolean part
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
  sb.append(getValueToString());
  sb.append("</").append(subtag).append('>').append(NL);
  return sb.toString();
  }

/**
 * This method create a Text Search stream for all data in this class, defined
 * by the MyCoRe TS MCRMetaBoolean definition for the given tag and subtag.
 * The content of this stream is depended by the implementation for
 * the persistence database. It was choose over the
 * <em>MCR.persistence_type</em> configuration.
 *
 * @param mcr_query   a class they implement the <b>MCRQueryInterface</b>
 * @param tag                the tagname of an element list
 * @exception MCRException if the content of this class is not valid
 * @return a TS string with the TS MCRMetaBoolean part
 **/
public final String createTS(Object mcr_query, String tag) throws MCRException
  {
  if (!isValid()) {
    debug();
    throw new MCRException("The content is not valid."); }
  String [] sattrib = new String[1];
  String [] svalue = new String[1];
  if ((type != null) && ((type = type.trim()).length() !=0)) {
    sattrib[0] = "type"; svalue[0] = type; }
  else {
    sattrib[0] = null; svalue[0] = null; }
  StringBuffer sb = new StringBuffer(1024);
  sb.append(((MCRQueryInterface)mcr_query).createSearchStringBoolean(datapart,
    tag,subtag,sattrib,svalue,value));
  return sb.toString();
  }

/**
 * This method check the validation of the content of this class.
 * The method returns <em>true</em> if
 * <ul>
 * <li> the subtag is not null or empty
 * </ul>
 * otherwise the method return <em>false</em>
 *
 * @return a boolean value
 **/
public final boolean isValid()
  {
  if (!super.isValid()) { return false; }
  return true;
  }

/**
 * This method print all data content from the MCRMetaBoolean class.
 **/
public final void debug()
  {
  System.out.println("MCRMetaBoolean debug start:");
  super.debug();
  System.out.println(value);
  System.out.println("MCRMetaBoolean debug end"+NL);
  }

}

