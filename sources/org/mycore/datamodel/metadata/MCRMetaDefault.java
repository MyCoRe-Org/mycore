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
import mycore.common.MCRException;

/**
 * This class implements any methods for handling the basic data for all 
 * metadata classes of the metadata objects. The methods createXML() and
 * createTS() are abstract methods.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public abstract class MCRMetaDefault
{

// common data
protected static String NL =
  new String((System.getProperties()).getProperty("line.separator"));
protected static String DEFAULT_LANGUAGE = "en";

// MetaLangText data
protected String subtag;
protected String lang;
protected String type;

/**
 * This is the constructor. <br>
 * The language element was set to <b>en</b>.
 * All other elemnts was set to an empty string.
 */
public MCRMetaDefault()
  {
  lang = DEFAULT_LANGUAGE;
  subtag = "";
  type = "";
  }

/**
 * This is the constructor. <br>
 * The language element was set. If the value of <em>default_lang</em>
 * is empty or false <b>en</b> was set. All other elemnts was set to an 
 * empty string.
 *
 * @param default_lang     the default language
 */
public MCRMetaDefault(String default_lang)
  {
  lang = DEFAULT_LANGUAGE;
  if ((default_lang != null) && 
    ((default_lang = default_lang.trim()).length() !=0)) {
    lang = default_lang; }
  subtag = "";
  type = "";
  }

/**
 * This is the constructor. <br>
 * The language element was set. If the value of <em>default_lang</em>
 * is null, empty or false <b>en</b> was set. The subtag element was set 
 * to the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
 * is null or empty an exception was throwed. The type element was set to 
 * the value of <em>set_type<em>, if it is null, an empty string was set 
 * to the type element.
 *
 * @param set_subtag       the name of the subtag
 * @param default_lang     the default language
 * @param set_type         the optional type string
 * @exception MCRException if the set_subtag value is null or empty
 */
public MCRMetaDefault(String set_subtag, String default_lang, String set_type)
  throws MCRException
  {
  lang = DEFAULT_LANGUAGE;
  subtag = "";
  type = "";
  if ((set_subtag == null) || ((set_subtag = set_subtag.trim()).length() ==0)) {
    throw new MCRException("The set_subtag is null or empty."); }
  subtag = set_subtag;
  if ((default_lang != null) &&
    ((default_lang = default_lang.trim()).length() !=0)) {
    lang = default_lang; }
  if (set_type != null) {
    type = set_type; }
  }

/**
 * This method set the language element. If the value of <em>default_lang</em>
 * is null, empty or false nothing was changed.
 *
 * @param default_lang           the default language
 **/
public final void setLang(String default_lang)
  {
  if ((default_lang == null) ||
    ((default_lang = default_lang.trim()).length() ==0)) {
    lang = DEFAULT_LANGUAGE; }
  else {
    lang = default_lang; }
  }

/**
 * This method set the subtag element. If the value of <em>set_subtag</em>
 * is null or empty an exception was throwed.
 *
 * @param set_subtag             the subtag
 * @exception MCRException if the set_subtag value is null or empty
 **/
public final void setSubTag(String set_subtag) throws MCRException
  {
  if ((set_subtag == null) || ((set_subtag = set_subtag.trim()).length() ==0)) {
    throw new MCRException("The set_subtag is null or empty."); }
  subtag = set_subtag;
  }

/**
 * This method set the type element. If the value of <em>set_type</em>
 * is null or empty nothing was changed.
 *
 * @param set_type               the optional type
 **/
public final void setType(String set_type)
  {
  if (set_type == null) { return; }
  type = set_type;
  }

/**
 * This method get the language element.
 *
 * @return the language
 **/
public final String getLang()
  { return lang; }

/**
 * This method get the subtag element.
 *
 * @return the subtag
 **/
public final String getSubTag()
  { return subtag; }

/**
 * This method get the type element.
 *
 * @return the type
 **/
public final String getType()
  { return type; }

/**
 * This method read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param dom_metadata_node      a relevant DOM element for the metadata
 * @exception MCRException if the set_subtag value is null or empty
 **/
public void setFromDOM(Node dom_metadata_node) throws MCRException
  {
  String temp_subtag = dom_metadata_node.getNodeName(); 
  if ((temp_subtag == null) ||
      ((temp_subtag = temp_subtag.trim()).length() ==0)) {
    throw new MCRException("The subtag is null or empty."); }
  subtag = temp_subtag;
  String temp_lang = ((Element)dom_metadata_node).getAttribute("xml:lang");
  if ((temp_lang!=null) && ((temp_lang = temp_lang.trim()).length() !=0)) {
    lang = temp_lang; }
  String temp_type = ((Element)dom_metadata_node).getAttribute("type");
  if ((temp_type!=null) && ((temp_type = temp_type.trim()).length() !=0)) {
    type = temp_type; }
  }

/**
 * This abstract method create a XML stream for all data in this class,
 * defined by the MyCoRe XML MCRMeta... definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a XML string with the XML MCRMeta... part
 **/
public abstract String createXML() throws MCRException;

/**
 * This abstract method create a Text Search stream for all data in this class,
 * defined by the MyCoRe TS MCRMeta... definition for the given tag and subtag.
 * The content of this stream is depended by the implementation for
 * the persistence database. It was choose over the
 * <em>MCR.persistence_type</em> configuration.
 *
 * @param mcr_query   a class they implement the <b>MCRQueryInterface</b>
 * @exception MCRException if the content of this class is not valid
 * @return a TS string with the TS MCRMeta... part
 **/
public abstract String createTS(Object mcr_query) throws MCRException;

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
public boolean isValid()
  {
  if ((subtag == null) || ((subtag = subtag.trim()).length() ==0)) {
    return false; }
  return true;
  }

/**
 * This method print all data content from this class.
 **/
public void debug()
  {
  if (type.trim().length()!=0) {
    System.out.println("--- SubTag "+subtag+" with lang "+lang+" and type "+
      type+" ---"); }
  else {
    System.out.println("--- SubTag "+subtag+" with lang "+lang+" ---"); }
  }

}

