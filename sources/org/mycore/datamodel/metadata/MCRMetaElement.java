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
 * This class is designed to to have a basic class with
 * common methode set of all metadata classes.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public abstract class MCRMetaElement
{

// common data
protected static String NL =
  new String((System.getProperties()).getProperty("line.separator"));
protected String default_lang = "en";
protected String class_name = "";
protected String tag = "";
protected boolean hereditary = false;

/**
 * This is the constructor of the MCRMetaElement class. 
 **/
public MCRMetaElement()
  {
  }

/**
 * This methode return the name of this metadata class as string.
 *
 * @return the name of this metadata class as string
 **/
public final String getClassName()
  { return class_name; }

/**
 * This methode return the hereditary of this metadata as boolean value.
 *
 * @return the hereditary of this metadata class
 **/
public final boolean getHereditary()
  { return hereditary; }

/**
 * This methode return the default language of this metadata class as string.
 *
 * @return the default language of this metadata class as string
 **/
public final String getDefaultLang()
  { return default_lang; }

/**
 * This methode return the tag of this metadata class as string.
 *
 * @return the tag of this metadata class as string
 **/
public final String getTag()
  { return tag; }

/**
 * This methode set the hereditary for the metadata class.
 *
 * @param hereditary            the hereditary as boolean value
 */
public void setHereditary(boolean hereditary)
  {
  this.hereditary = false;
  if (hereditary) { this.hereditary = hereditary; return; }
  }

/**
 * This methode set the hereditary for the metadata class.
 *
 * @param hereditary            the hereditary as string
 */
public void setHereditary(String hereditary)
  {
  this.hereditary = false;
  if ((hereditary == null) || ((hereditary = hereditary.trim()).length() ==0))
    { return; }
  if (hereditary.equals("true")) { this.hereditary = true; }
  }

/**
 * This methode set the default language for the metadata class.
 *
 * @param default_lang          the default language
 */
public void setDefaultLang(String lang)
  {
  if ((lang == null) || ((lang = lang.trim()).length() ==0)) { return; }
  default_lang = lang;
  }

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
 * This methode read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param dom_element_list       a relevant DOM element for the metadata
 **/
public abstract void setFromDOM(Node metadata_langtext_node);

/**
 * This methode create a XML stream for a metadata part.
 *
 * @return a XML string with the XML data of the metadata part
 **/
public abstract String createXML();

/**
 * This methode create a Text Search stream for a metadata part.
 * The content of this stream is depended by the implementation for
 * the persistence database. It was choose over the
 * <em>MCR.persistence_type</em> configuration.
 *
 * @param mcr_query   a class they implement the <b>MCRQueryInterface</b>
 * @return a Text Search string with the data of the metadata part
 **/
public abstract String createTS(Object mcr_query);

/**
 * This methode print all elements of the metadata class.
 **/
public abstract void debug();

}

