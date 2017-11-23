/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.metadata;

import org.mycore.common.MCRException;

import com.google.gson.JsonObject;

/**
 * This interface is designed to to have a general description of the common
 * method set of all metadata classes.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public interface MCRMetaInterface extends Cloneable {

    /**
     * This method get the inherited element.
     * 
     * @return the inherited flag as int
     */
    int getInherited();

    /**
     * This method get the language element.
     * 
     * @return the language
     */
    String getLang();

    /**
     * This method get the subtag element.
     * 
     * @return the subtag
     */
    String getSubTag();

    /**
     * This method get the type element.
     * 
     * @return the type
     */
    String getType();

    /**
     * This method set the inherited level. This can be 0 or an integer higher
     * 0.
     * 
     * @param value
     *            the inherited level value, if it is &lt; 0, 0 is set
     */
    void setInherited(int value);

    /**
     * This method increments the inherited value with 1.
     */
    void incrementInherited();

    /**
     * This method decrements the inherited value with 1.
     */
    void decrementInherited();

    /**
     * This methode set the default language to the class.
     * 
     * @param default_lang
     *            the default language
     */
    void setLang(String default_lang);

    /**
     * This method set the subtag element. If the value of <em>set_subtag</em>
     * is null or empty an exception is throwed.
     * 
     * @param set_subtag
     *            the subtag
     * @exception MCRException
     *                if the set_subtag value is null or empty
     */
    void setSubTag(String set_subtag) throws MCRException;

    /**
     * This method set the type element. If the value of <em>set_type</em> is
     * null or empty nothing was changed.
     * 
     * @param set_type
     *            the optional type
     */
    void setType(String set_type);

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    void setFromDOM(org.jdom2.Element element);

    /**
     * This method create a XML stream for a metadata part.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML data of the metadata part
     */
    org.jdom2.Element createXML() throws MCRException;

    /**
     * This method creates a JSON representation of the metadata part.
     * 
     * @return a GSON object containing the json data of the metadata part 
     */
    JsonObject createJSON();

    /**
     * This method check the validation of the content of this class.
     * 
     * @return a boolean value
     */
    boolean isValid();

    /**
     * Validates the content of this class.
     * 
     * @throws MCRException the content is invalid
     */
    void validate() throws MCRException;

    /**
     * This method make a clone of this class.
     */
    MCRMetaInterface clone();

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    void debug();

}
