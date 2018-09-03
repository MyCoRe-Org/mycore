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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;

import com.google.gson.JsonObject;

/**
 * This class implements all method for handling with the MCRMetaBoolean part of
 * a metadata object. The MCRMetaBoolean class present a logical value of true
 * or false and optional a type.
 * <p>
 * &lt;tag class="MCRMetaBoolean" heritable="..."&gt; <br>
 * &lt;subtag type="..."&gt; <br>
 * true|false<br>
 * &lt;/subtag&gt; <br>
 * &lt;/tag&gt; <br>
 * 
 * @author Jens Kupferschmidt
 */
public final class MCRMetaBoolean extends MCRMetaDefault {

    private boolean value;

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. The boolean value was set to
     * false.
     */
    public MCRMetaBoolean() {
        super();
        value = false;
    }

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. The subtag element was set to
     * the value of <em>set_subtag</em>. If the value of <em>set_subtag</em>
     * is null or empty an exception was thrown. The type element was set to
     * the value of <em>set_type</em>, if it is null, an empty string was set
     * to the type element. The boolean string <em>set_value</em>
     * was set to a boolean element, if it is null, false was set.
     * @param set_subtag       the name of the subtag
     * @param default_lang     the language
     * @param set_type         the optional type string
     * @param set_inherted     a value &gt;= 0
     * @param set_value        the boolean value (true or false) as string
     *
     * @exception MCRException if the set_subtag value is null or empty
     */
    @Deprecated
    public MCRMetaBoolean(String set_subtag, String default_lang, String set_type, int set_inherted, String set_value)
        throws MCRException {
        this(set_subtag, set_type, set_inherted, false);
        setValue(set_value);
    }

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. The subtag element was set to
     * the value of <em>set_subtag</em>. If the value of <em>set_subtag</em>
     * is null or empty an exception was thrown. The type element was set to
     * the value of <em>set_type</em>, if it is null, an empty string was set
     * to the type element. The boolean string <em>set_value</em>
     * was set to a boolean element, if it is null, false was set.
     * @param set_subtag       the name of the subtag
     * @param set_type         the optional type string
     * @param set_inherted     a value &gt;= 0
     * @param set_value        the boolean value (true or false) as string
     *
     * @exception MCRException if the set_subtag value is null or empty
     */
    public MCRMetaBoolean(String set_subtag, String set_type, int set_inherted, String set_value)
        throws MCRException {
        super(set_subtag, null, set_type, set_inherted);
        setValue(set_value);
    }

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. The subtag element was set to
     * the value of <em>set_subtag</em>. If the value of <em>set_subtag</em>
     * is null or empty an exception was thrown. The type element was set to
     * the value of <em>set_type</em>, if it is null, an empty string was set
     * to the type element. The boolean string <em>set_value</em>
     * was set to a boolean element, if it is null, false was set.
     * @param set_subtag       the name of the subtag
     * @param set_type         the optional type string
     * @param set_inherted     a value &gt;= 0
     * @param set_value        the boolean value (true or false)
     * @exception MCRException if the set_subtag value is null or empty
     */
    public MCRMetaBoolean(String set_subtag, String set_type, int set_inherted, boolean set_value) throws MCRException {
        super(set_subtag, null, set_type, set_inherted);
        setValue(set_value);
   }

    /**
     * This method set value. It set false if the string is corrupt.
     * 
     * @param set_value
     *            the boolean value (true or false) as string
     */
    public void setValue(String set_value) {
        value = Boolean.parseBoolean(set_value);
    }

    /**
     * This method set the value.
     * 
     * @param set_value
     *            the boolean value
     */
    public void setValue(boolean set_value) {
        value = set_value;
    }

    /**
     * This method get the value element.
     * 
     * @return the value as Boolean
     */
    public boolean getValue() {
        return value;
    }

    /**
     * This method get the value element as String.
     * 
     * @return the value as String
     */
    public String getValueToString() {
        return String.valueOf(value);
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    @Override
    public void setFromDOM(org.jdom2.Element element) {
        super.setFromDOM(element);
        setValue(element.getTextTrim());
    }

    /**
     * This method create a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRBoolean definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRBoolean part
     */
    @Override
    public Element createXML() throws MCRException {
        Element elm = super.createXML();
        elm.addContent(getValueToString());
        return elm;
    }

    /**
     * Creates the JSON representation. Extends the {@link MCRMetaDefault#createJSON()} method
     * with the following data.
     * 
     * <pre>
     *   {
     *     value: true|false
     *   }
     * </pre>
     * 
     */
    @Override
    public JsonObject createJSON() {
        JsonObject obj = super.createJSON();
        obj.addProperty("value", getValue());
        return obj;
    }

    /**
     * clone of this instance
     * 
     * you will get a (deep) clone of this element
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public MCRMetaBoolean clone() {
        MCRMetaBoolean clone = (MCRMetaBoolean) super.clone();

        clone.value = this.value;

        return clone;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public void debug() {
        if (LOGGER.isDebugEnabled()) {
            super.debugDefault();
            LOGGER.debug("Value              = {}", Boolean.toString(value));
            LOGGER.debug(" ");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final MCRMetaBoolean other = (MCRMetaBoolean) obj;
        return this.value == other.value;
    }

}
