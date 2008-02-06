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

package org.mycore.datamodel.classifications;

import java.io.Serializable;

/**
 * This class implements a label of a classification or a category.
 * 
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 * 
 * @version $Revision$ $Date$
 */
public class MCRLabel implements Cloneable, Serializable {

    private static final long serialVersionUID = 946493314913567890L;

    String lang, text, description;

    /**
     * Constructor
     */
    public MCRLabel() {
        super();
    }

    /**
     * The constructor set all label data.
     * 
     * @param lang the language entry
     * @param text the text entry
     * @param description the description entry
     */
    public MCRLabel(String lang, String text, String description) {
        super();
        this.lang = lang;
        this.text = text;
        this.description = description;
    }
    
    /**
     * The method get the language as String.
     * 
     * @return the language as String
     */
    public String getLang() {
        return lang;
    }

    /**
     * The method set the language from String.
     * 
     * @return the language from String
     */
    public void setLang(String lang) {
        this.lang = lang;
    }

    /**
     * The method get the text as String.
     * 
     * @return the text as String
     */
    public String getText() {
        return text;
    }

    /**
     * The method set the text from String.
     * 
     * @return the text from String
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * The method get the description as String.
     * 
     * @return the description as String
     */
    public String getDescription() {
        return description;
    }

    /**
     * The method set the description from String.
     * 
     * @return the description from String
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public MCRLabel clone() {
        MCRLabel clone = null;
        try {
            clone = (MCRLabel) super.clone();
        } catch (CloneNotSupportedException ce) {
            // Can not happen
        }
        return clone;
    }

}
