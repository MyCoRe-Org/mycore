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

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.language.MCRLanguageFactory;

import com.google.gson.JsonObject;

/**
 * This class implements any methods for handling the basic data for all
 * metadata classes of the metadata objects. The methods createXML() and
 * createTypedContent() and createTextSearch() are abstract methods.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public abstract class MCRMetaDefault implements MCRMetaInterface {
    // public data
    public static final int DEFAULT_LANG_LENGTH = 12;

    public static final int DEFAULT_TYPE_LENGTH = 256;

    public static final int DEFAULT_STRING_LENGTH = 4096;

    // common data
    protected static final String NL = System.getProperties().getProperty("line.separator");

    protected static final String DEFAULT_LANGUAGE = MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang",
        MCRConstants.DEFAULT_LANG);

    protected static final String DEFAULT_DATAPART = "metadata";

    protected static final int DEFAULT_INHERITED = 0;

    // logger
    private static Logger LOGGER = LogManager.getLogger();

    // MetaLangText data
    protected String subtag;

    protected String lang;

    protected String type;

    protected int inherited;

    protected String datapart;

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. The datapart element was set
     * to <b>metadata </b>All other elemnts was set to an empty string. The
     * inherited value is set to 0!
     */
    public MCRMetaDefault() {
        inherited = DEFAULT_INHERITED;
        datapart = DEFAULT_DATAPART;
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is
     * empty or false <b>en </b> was set. The datapart was set to default. All
     * other elemnts was set to an empty string. The inherited value is set to
     * 0!
     * 
     * @param default_lang
     *            the default language
     */
    public MCRMetaDefault(String default_lang) {
        this();
        lang = default_lang;
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>set_subtag</em>. If the value of <em>set_subtag</em>
     * is null or empty an exception was throwed. The type element was set to
     * the value of <em>set_type</em>, if it is null, an empty string was set
     * to the type element. The datapart element was set. If the value of
     * <em>set_datapart,</em> is null or empty the default was set.
     * @param set_subtag       the name of the subtag
     * @param default_lang     the default language
     * @param set_type         the optional type string
     * @param set_inherited     a int value , &gt; 0 if the data are inherited,
     *                         else = 0.
     *
     * @exception MCRException if the set_subtag value is null or empty
     */
    public MCRMetaDefault(String set_subtag, String default_lang, String set_type, int set_inherited)
        throws MCRException {
        this(default_lang);
        setInherited(set_inherited);
        subtag = set_subtag;
        type = set_type;
    }

    /**
     * This method set the inherited level. This can be 0 or an integer higher
     * 0.
     * 
     * @param value
     *            the inherited level value, if it is &lt; 0, 0 is set
     */
    public final void setInherited(int value) {
        inherited = value;
    }

    /**
     * This method increments the inherited value with 1.
     */
    public final void incrementInherited() {
        inherited++;
    }

    /**
     * This method decrements the inherited value with 1.
     */
    public final void decrementInherited() {
        inherited--;
    }

    /**
     * This method set the language element. If the value of
     * <em>default_lang</em> is null, empty or false nothing was changed.
     * 
     * @param default_lang
     *            the default language
     */
    public final void setLang(String default_lang) {
        lang = default_lang;
    }

    /**
     * This method set the subtag element. If the value of <em>set_subtag</em>
     * is null or empty an exception was throwed.
     * 
     * @param set_subtag
     *            the subtag
     * @exception MCRException
     *                if the set_subtag value is null or empty
     */
    public final void setSubTag(String set_subtag) throws MCRException {
        subtag = set_subtag;
    }

    /**
     * This method set the type element. If the value of <em>set_type</em> is
     * null or empty nothing was changed.
     * 
     * @param set_type
     *            the optional type
     */
    public final void setType(String set_type) {
        type = set_type;
    }

    /**
     * This method get the inherited element.
     * 
     * @return the inherited flag as int
     */
    public final int getInherited() {
        return inherited;
    }

    /**
     * This method get the language element.
     * 
     * @return the language
     */
    public final String getLang() {
        return lang;
    }

    /**
     * This method get the subtag element.
     * 
     * @return the subtag
     */
    public final String getSubTag() {
        return subtag;
    }

    /**
     * This method get the type element.
     * 
     * @return the type
     */
    public final String getType() {
        return type;
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant DOM element for the metadata
     * @exception MCRException
     *                if the set_subtag value is null or empty
     */
    public void setFromDOM(org.jdom2.Element element) throws MCRException {
        if (element == null) {
            return;
        }

        String temp_subtag = element.getName();

        if (temp_subtag == null || (temp_subtag = temp_subtag.trim()).length() == 0) {
            throw new MCRException("The subtag is null or empty.");
        }

        subtag = temp_subtag;

        String temp_lang = element.getAttributeValue("lang", org.jdom2.Namespace.XML_NAMESPACE);

        if (temp_lang != null && (temp_lang = temp_lang.trim()).length() != 0) {
            lang = temp_lang;
        }

        String temp_type = element.getAttributeValue("type");

        if (temp_type != null && (temp_type = temp_type.trim()).length() != 0) {
            type = temp_type;
        }

        String temp_herit = element.getAttributeValue("inherited");

        if (temp_herit != null && (temp_herit = temp_herit.trim()).length() != 0) {
            try {
                inherited = Integer.parseInt(temp_herit);
            } catch (NumberFormatException e) {
                inherited = 0;
            }
        }
    }

    /**
     * This abstract method create a XML stream for all data in this class,
     * defined by the MyCoRe XML MCRMeta... definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMeta... part
     */
    public Element createXML() throws MCRException {
        try {
            validate();
        } catch (MCRException exc) {
            debug();
            throw exc;
        }
        Element elm = new Element(subtag);
        if (getLang() != null && getLang().length() > 0)
            elm.setAttribute("lang", getLang(), Namespace.XML_NAMESPACE);
        if (getType() != null && getType().length() > 0) {
            elm.setAttribute("type", getType());
        }
        elm.setAttribute("inherited", Integer.toString(getInherited()));
        return elm;
    }

    /**
     * Creates a json object in the form of:
     * <pre>
     *   {
     *     lang: "de",
     *     type: "title",
     *     inherited: 0
     *   }
     * </pre>
     */
    @Override
    public JsonObject createJSON() {
        JsonObject obj = new JsonObject();
        if (getLang() != null) {
            obj.addProperty("lang", getLang());
        }
        if (getType() != null) {
            obj.addProperty("type", getType());
        }
        obj.addProperty("inherited", getInherited());
        return obj;
    }

    /**
     * This method check the validation of the content of this class. The method
     * returns <em>true</em> if
     * <ul>
     * <li>the subtag is not null or empty
     * <li>the lang value was supported
     * </ul>
     * otherwise the method return <em>false</em>
     * 
     * @return a boolean value
     */
    public boolean isValid() {
        try {
            validate();
            return true;
        } catch (MCRException exc) {
            LOGGER.warn("The the metadata element '{}' is invalid.", subtag, exc);
        }
        return false;
    }

    /**
     * Validates this MCRMetaDefault. This method throws an exception if:
     * <ul>
     * <li>the subtag is not null or empty</li>
     * <li>the lang value was supported</li>
     * <li>the inherited value is lower than zero</li>
     * </ul>
     * 
     * @throws MCRException the MCRMetaDefault is invalid
     */
    public void validate() throws MCRException {
        if (subtag == null || (subtag = subtag.trim()).length() == 0) {
            throw new MCRException("No tag name defined!");
        }
        if (lang != null && !MCRLanguageFactory.instance().isSupportedLanguage(lang)) {
            throw new MCRException(getSubTag() + ": language is not supported: " + lang);
        }
        if (getInherited() < 0) {
            throw new MCRException(getSubTag() + ": inherited can not be smaller than '0': " + getInherited());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(datapart, inherited, lang, subtag, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        MCRMetaDefault other = (MCRMetaDefault) obj;
        return Objects.equals(datapart, other.datapart) && Objects.equals(inherited, other.inherited)
            && Objects.equals(lang, other.lang) && Objects.equals(subtag, other.subtag)
            && Objects.equals(type, other.type);
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    public void debug() {
        if (LOGGER.isDebugEnabled()) {
            debugDefault();
            LOGGER.debug(" ");
        }
    }

    /**
     * This method put common debug data to the logger (for the debug mode).
     */
    public final void debugDefault() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SubTag             = {}", subtag);
            LOGGER.debug("Language           = {}", lang);
            LOGGER.debug("Type               = {}", type);
            LOGGER.debug("DataPart           = {}", datapart);
            LOGGER.debug("Inhreited          = {}", String.valueOf(inherited));
        }
    }

    @Override
    public abstract MCRMetaInterface clone();
}
