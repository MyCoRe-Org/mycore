/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.language.MCRLanguageFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.JsonObject;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This class implements any methods for handling the basic data for all
 * metadata classes of the metadata objects. The methods createXML() and
 * createTypedContent() and createTextSearch() are abstract methods.
 *
 * @author Jens Kupferschmidt
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    setterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class MCRMetaDefault implements MCRMetaInterface {
    // public data
    public static final int DEFAULT_LANG_LENGTH = 12;

    public static final int DEFAULT_TYPE_LENGTH = 256;

    public static final int DEFAULT_STRING_LENGTH = 4096;

    // common data
    protected static final String NL = System.getProperties().getProperty("line.separator");

    protected static final String DEFAULT_LANGUAGE = MCRConfiguration2.getString("MCR.Metadata.DefaultLang")
        .orElse(MCRConstants.DEFAULT_LANG);

    protected static final String DEFAULT_ELEMENT_DATAPART = MCRObjectMetadata.XML_NAME;

    protected static final String DEFAULT_ATTRIBUTE_INHERITED = MCRXMLConstants.INHERITED;

    protected static final String DEFAULT_ATTRIBUTE_LANG = MCRXMLConstants.LANG;

    protected static final String DEFAULT_ATTRIBUTE_SEQUENCE = MCRXMLConstants.SEQUENCE;

    protected static final String DEFAULT_ATTRIBUTE_TYPE = MCRXMLConstants.TYPE;

    protected static final int DEFAULT_INHERITED = 0;

    protected static final int DEFAULT_SEQUENCE = -1;

    private static final Logger LOGGER = LogManager.getLogger();

    // MetaLangText data
    protected String subtag;

    protected String lang;

    protected String type;

    protected int sequence;

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
        sequence = DEFAULT_SEQUENCE;
        datapart = DEFAULT_ELEMENT_DATAPART;
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>lang</em> is
     * empty or false <b>en </b> was set. The datapart was set to default. All
     * other elemnts was set to an empty string. The inherited value is set to
     * 0!
     *
     * @param lang
     *            the default language
     */
    public MCRMetaDefault(String lang) {
        this();
        this.lang = lang;
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>subtag</em>. If the value of <em>subtag</em>
     * is null or empty an exception was throwed. The type element was set to
     * the value of <em>type</em>, if it is null, an empty string was set
     * to the type element. The datapart element was set. If the value of
     * <em>datapart,</em> is null or empty the default was set.
     * @param subtag       the name of the subtag
     * @param lang         the language
     * @param type         the optional type string
     * @param inherited     a int value , &gt; 0 if the data are inherited,
     *                         else = 0.
     *
     * @exception MCRException if the subtag value is null or empty
     */
    public MCRMetaDefault(String subtag, String lang, String type, int inherited)
        throws MCRException {
        this(lang);
        setInherited(inherited);
        this.subtag = subtag;
        this.type = type;
        this.sequence = DEFAULT_SEQUENCE;
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>subtag</em>. If the value of <em>subtag</em>
     * is null or empty an exception was throwed. The type element was set to
     * the value of <em>type</em>, if it is null, an empty string was set
     * to the type element. The datapart element was set. If the value of
     * <em>datapart,</em> is null or empty the default was set.
     * @param subtag       the name of the subtag
     * @param lang         the language
     * @param type         the optional type string
     * @param sequence     the optional sequence attribute as integer
     * @param inherited     a int value , &gt; 0 if the data are inherited,
     *                         else = 0.
     *
     * @exception MCRException if the subtag value is null or empty
     */
    public MCRMetaDefault(String subtag, String lang, String type, int sequence, int inherited)
        throws MCRException {
        this(lang);
        setInherited(inherited);
        this.subtag = subtag;
        this.type = type;
        setSequence(sequence);
    }

    /**
     * This method set the inherited level. This can be 0 or an integer higher
     * 0.
     *
     * @param value
     *            the inherited level value, if it is &lt; 0, 0 is set
     */
    @Override
    public final void setInherited(int value) {
        inherited = value;
    }

    /**
     * This method increments the inherited value with 1.
     */
    @Override
    public final void incrementInherited() {
        inherited++;
    }

    /**
     * This method decrements the inherited value with 1.
     */
    @Override
    public final void decrementInherited() {
        inherited--;
    }

    /**
     * This method set the language element. If the value of
     * <em>lang</em> is null, empty or false nothing was changed.
     *
     * @param lang
     *            the language
     */
    @Override
    public final void setLang(String lang) {
        this.lang = lang;
    }

    /**
     * This method set the subtag element. If the value of <em>subtag</em>
     * is null or empty an exception was throwed.
     *
     * @param subtag
     *            the subtag
     * @exception MCRException
     *                if the subtag value is null or empty
     */
    @Override
    public final void setSubTag(String subtag) throws MCRException {
        this.subtag = subtag;
    }

    /**
     * This method set the type element. If the value of <em>type</em> is
     * null or empty nothing was changed.
     *
     * @param type
     *            the optional type
     */
    @Override
    public final void setType(String type) {
        this.type = type;
    }

    /**
     * This method set the sequence element. If the value of <em>sequence</em> is
     * null or empty nothing was changed.
     *
     * @param sequence
     *            the optional sequence attribute
     */
    public final void setSequence(int sequence) {
        this.sequence = sequence;
    }

    /**
     * This method get the inherited element.
     *
     * @return the inherited flag as int
     */
    @Override
    public final int getInherited() {
        return inherited;
    }

    /**
     * This method get the language element.
     *
     * @return the language
     */
    @Override
    public final String getLang() {
        return lang;
    }

    /**
     * This method get the subtag element.
     *
     * @return the subtag
     */
    @Override
    @Schema(hidden = true)
    @JsonIgnore
    public final String getSubTag() {
        return subtag;
    }

    /**
     * This method get the type element.
     *
     * @return the type
     */
    @Override
    public final String getType() {
        return type;
    }

    /**
     * This method get the sequence element.
     *
     * @return the sequence element
     */
    public int getSequence() {
        return sequence;
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     *
     * @param element
     *            a relevant DOM element for the metadata
     * @exception MCRException
     *                if the subtag value is null or empty
     */
    @Override
    public void setFromDOM(Element element) throws MCRException {
        if (element == null) {
            return;
        }

        subtag = element.getName();

        MCRUtils.filterTrimmedNotEmpty(element.getAttributeValue(DEFAULT_ATTRIBUTE_LANG, Namespace.XML_NAMESPACE))
            .ifPresent(tempLang -> lang = tempLang);
        MCRUtils.filterTrimmedNotEmpty(element.getAttributeValue(DEFAULT_ATTRIBUTE_TYPE))
            .ifPresent(tempType -> type = tempType);
        MCRUtils.filterTrimmedNotEmpty(element.getAttributeValue(DEFAULT_ATTRIBUTE_SEQUENCE))
            .map(Integer::parseInt)
            .ifPresent(tempSequence -> sequence = tempSequence);
        MCRUtils.filterTrimmedNotEmpty(element.getAttributeValue(DEFAULT_ATTRIBUTE_INHERITED))
            .map(Integer::parseInt)
            .ifPresent(tempInherited -> inherited = tempInherited);
    }

    /**
     * This abstract method create a XML stream for all data in this class,
     * defined by the MyCoRe XML MCRMeta... definition for the given subtag.
     *
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMeta... part
     */
    @Override
    public Element createXML() throws MCRException {
        try {
            validate();
        } catch (MCRException exc) {
            debug();
            throw exc;
        }
        Element elm = new Element(subtag);
        if (getLang() != null && getLang().length() > 0) {
            elm.setAttribute(DEFAULT_ATTRIBUTE_LANG, getLang(), Namespace.XML_NAMESPACE);
        }
        if (getType() != null && getType().length() > 0) {
            elm.setAttribute(DEFAULT_ATTRIBUTE_TYPE, getType());
        }
        if (getSequence() >= 0) {
            elm.setAttribute(DEFAULT_ATTRIBUTE_SEQUENCE, Integer.toString(getSequence()));
        }
        elm.setAttribute(DEFAULT_ATTRIBUTE_INHERITED, Integer.toString(getInherited()));
        return elm;
    }

    /**
     * Creates a json object in the form of:
     * <pre>
     *   {
     *     lang: "de",
     *     type: "title",
     *     sequence: "0001"
     *     inherited: 0
     *   }
     * </pre>
     */
    @Override
    public JsonObject createJSON() {
        JsonObject obj = new JsonObject();
        if (getLang() != null) {
            obj.addProperty(DEFAULT_ATTRIBUTE_LANG, getLang());
        }
        if (getType() != null) {
            obj.addProperty(DEFAULT_ATTRIBUTE_TYPE, getType());
        }
        if (getSequence() >= 0) {
            obj.addProperty(DEFAULT_ATTRIBUTE_SEQUENCE, getSequence());
        }
        obj.addProperty(DEFAULT_ATTRIBUTE_INHERITED, getInherited());
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
    @Override
    @JsonIgnore
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
    @Override
    public void validate() throws MCRException {
        subtag = MCRUtils.filterTrimmedNotEmpty(subtag).orElse(null);
        if (subtag == null) {
            throw new MCRException("No tag name defined!");
        }
        if (lang != null && !MCRLanguageFactory.obtainInstance().isSupportedLanguage(lang)) {
            throw new MCRException(getSubTag() + ": language is not supported: " + lang);
        }
        if (getInherited() < 0) {
            throw new MCRException(getSubTag() + ": inherited can not be smaller than '0': " + getInherited());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(datapart, inherited, lang, subtag, type, sequence);
    }

    /**
     * checks if two metadata objects are equal
     * 
     * This method should not call super.equals(obj)
     * to avoid identity check <code>this == obj</code>.
     *  
     * 
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MCRMetaDefault other = (MCRMetaDefault) obj;
        return Objects.equals(datapart, other.datapart)
            && Objects.equals(inherited, other.inherited)
            && Objects.equals(lang, other.lang)
            && Objects.equals(subtag, other.subtag)
            && Objects.equals(type, other.type)
            && Objects.equals(sequence, other.sequence);
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
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
            LOGGER.debug("Sequence           = {}", String.valueOf(sequence));
            LOGGER.debug("DataPart           = {}", datapart);
            LOGGER.debug("Inhreited          = {}", String.valueOf(inherited));
        }
    }

    @Override
    public MCRMetaDefault clone() {
        MCRMetaDefault clone = MCRClassTools.clone(getClass(), super::clone);

        clone.subtag = this.subtag;
        clone.lang = this.lang;
        clone.type = this.type;
        clone.sequence = this.sequence;
        clone.datapart = this.datapart;
        clone.inherited = this.inherited;

        return clone;
    }

}
