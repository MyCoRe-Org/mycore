/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.metadata;

import org.mycore.common.MCRException;

/**
 * This interface is designed to to have a general description of the common
 * methode set of all metadata classes.
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
    public int getInherited();

    /**
     * This method get the inherited element.
     * 
     * @return the inherited value as string
     */
    public String getInheritedToString();

    /**
     * This method get the language element.
     * 
     * @return the language
     */
    public String getLang();

    /**
     * This method get the subtag element.
     * 
     * @return the subtag
     */
    public String getSubTag();

    /**
     * This method get the type element.
     * 
     * @return the type
     */
    public String getType();

    /**
     * This method set the inherited level. This can be 0 or an integer higher
     * 0.
     * 
     * @param value
     *            the inherited level value, if it is < 0, 0 was set
     */
    public void setInherited(int value);

    /**
     * This method increments the inherited value with 1.
     */
    public void incrementInherited();

    /**
     * This method decrements the inherited value with 1.
     */
    public void decrementInherited();

    /**
     * This methode set the default language to the class.
     * 
     * @param default_lang
     *            the default language
     */
    public void setLang(String default_lang);

    /**
     * This method set the subtag element. If the value of <em>set_subtag</em>
     * is null or empty an exception was throwed.
     * 
     * @param set_subtag
     *            the subtag
     * @exception MCRException
     *                if the set_subtag value is null or empty
     */
    public void setSubTag(String set_subtag) throws MCRException;

    /**
     * This method set the type element. If the value of <em>set_type</em> is
     * null or empty nothing was changed.
     * 
     * @param set_type
     *            the optional type
     */
    public void setType(String set_type);

    /**
     * This methode read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    public void setFromDOM(org.jdom.Element element);

    /**
     * This methode create a XML stream for a metadata part.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML data of the metadata part
     */
    public org.jdom.Element createXML() throws MCRException;

    /**
     * This methode check the validation of the content of this class.
     * 
     * @return a boolean value
     */
    public boolean isValid();

    /**
     * This method make a clone of this class.
     */
    public Object clone();
}
