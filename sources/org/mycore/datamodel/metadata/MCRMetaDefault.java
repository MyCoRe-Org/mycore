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

import org.w3c.dom.Node;
import mycore.common.MCRException;

/**
 * This class implements methode for handling some data for all metadata
 * classes of metadata objects.
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
 * The default language for the element was set to <b>en</b>.
 */
public MCRMetaDefault()
  {
  lang = DEFAULT_LANGUAGE;
  subtag = "";
  type = "";
  }

/**
 * This methode set the default language to the class.
 *
 * @param default_lang           the default language
 **/
public void setLang(String default_lang)
  {
  if ((default_lang == null) ||
    ((default_lang = default_lang.trim()).length() ==0)) {
    lang = DEFAULT_LANGUAGE; }
  else {
    lang = default_lang; }
  }

/**
 * This methode set the subtag string to the class.
 *
 * @param subtag                 the subtag
 **/
public void setSubTag(String subtag)
  {
  if ((subtag == null) || ((subtag = subtag.trim()).length() ==0)) {
    return; }
  this.subtag = subtag;
  }

/**
 * This methode set the type string to the class.
 *
 * @param type                   the type
 **/
public void setType(String type)
  {
  if ((type == null) || ((type = type.trim()).length() ==0)) {
    return; }
  this.type = type;
  }

/**
 * This methode get the language element.
 *
 * @return the language
 **/
public final String getLang()
  { return lang; }

/**
 * This methode get the subtag element.
 *
 * @return the subtag
 **/
public final String getSubTag()
  { return subtag; }

/**
 * This methode get the type element.
 *
 * @return the type
 **/
public final String getType()
  { return type; }

/**
 * This methode read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param dom_element_list       a relevant DOM element for the metadata
 **/
public abstract void setFromDOM(Node metadata_langtext_node);

/**
 * This abstract methode create a XML stream for all data in this class,
 * defined by the MyCoRe XML MCRMeta... definition for the given subtag.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a XML string with the XML MCRMeta... part
 **/
public abstract String createXML() throws MCRException;

/**
 * This abstract methode create a Text Search stream for all data in this class,
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
 * This abstract methode check the validation of the content MCRMeta... class.
 *
 * @return a boolean value
 **/
public abstract boolean isValid() ;

/**
 * This abstract metode print all data content from the MCRMeta... class.
 **/
public abstract void debug();

}

