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

import mycore.common.MCRException;

/**
 * This interface is designed to to have a general description of the
 * common methode set of all metadata classes.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public interface MCRMetaInterface
{

/**
 * This methode set the default language to the class.
 *
 * @param default_lang           the default language
 **/
public void setLang(String default_lang);

/**
 * This methode read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param element a relevant JDOM element for the metadata
 **/
public void setFromDOM(org.jdom.Element element);

/**
 * This methode create a XML stream for a metadata part.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a JDOM Element with the XML data of the metadata part
 **/
public org.jdom.Element createXML() throws MCRException;

/**
 * This methode create a typed content list for all data in this instance.
 *
 * @param parasearch true if the data should parametric searchable
 * @exception MCRException if the content of this class is not valid
 * @return a MCRTypedContent with the data of the MCRObject data
 **/
public MCRTypedContent createTypedContent(boolean parasearch)
  throws MCRException;

/**
 * This abstract method create a String for the text searchable data.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a String with the text search data
 **/
public String createTextSearch(boolean textsearch)
  throws MCRException;

/**
 * This methode check the validation of the content of this class.
 *
 * @return a boolean value
 **/
public boolean isValid();

/**
 * This methode print all elements of the metadata class.
 **/
public void debug();

}

