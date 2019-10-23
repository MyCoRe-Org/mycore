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
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;

import com.google.gson.JsonObject;

/**
 * This class implements all method for handling with the MCRMetaLangText part
 * of a metadata object. The MCRMetaLangText class present a single item, which
 * has triples of a text and his corresponding language and optional a type.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRMetaLangText extends MCRMetaDefault {

    protected String text;

    protected String form;

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. All other elements was set to
     * an empty string. The <em>form</em> Attribute is set to 'plain'.
     */
    public MCRMetaLangText() {
        super();
        text = "";
        form = "plain";
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>subtag</em>. If the value of <em>subtag</em>
     * is null or empty an exception was thrown. The type element was set to
     * the value of <em>type</em>, if it is null, an empty string was set
     * to the type element. The text element was set to the value of
     * <em>text</em>, if it is null, an empty string was set
     * to the text element.
     * @param subtag       the name of the subtag
     * @param lang     the default language
     * @param type         the optional type string
     * @param inherted     a value &gt;= 0
     * @param form         the format string, if it is empty 'plain' is set.
     * @param text         the text string
     *
     * @exception MCRException if the subtag value is null or empty
     */
    public MCRMetaLangText(String subtag, String lang, String type, int inherted, String form, String text)
        throws MCRException {
        super(subtag, lang, type, inherted);
        this.text = "";

        if (text != null) {
            this.text = text.trim();
        }

        this.form = "plain";

        if (form != null) {
            this.form = form.trim();
        }
    }

    /**
     * This method set the language, type and text.
     * 
     * @param lang
     *            the new language string, if this is null or empty, nothing is
     *            to do
     * @param type
     *            the optional type string
     * @param text
     *            the new text string
     */
    public final void set(String lang, String type, String form, String text) {
        setLang(lang);
        setType(type);

        if (text != null) {
            this.text = text.trim();
        }

        this.form = "plain";

        if (form != null) {
            this.form = form.trim();
        }
    }

    /**
     * This method set the text.
     * 
     * @param text
     *            the new text string
     */
    public final void setText(String text) {
        if (text != null) {
            this.text = text.trim();
        }
    }

    /**
     * This method set the form attribute.
     * 
     * @param form
     *            the new form string
     */
    public final void setForm(String form) {
        if (form != null) {
            text = form.trim();
        }
    }

    /**
     * This method get the text element.
     * 
     * @return the text
     */
    public final String getText() {
        return text;
    }

    /**
     * This method get the form attribute.
     * 
     * @return the form attribute
     */
    public final String getForm() {
        return form;
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    @Override
    public void setFromDOM(Element element) {
        super.setFromDOM(element);

        String tempText = element.getText();

        if (tempText == null) {
            tempText = "";
        }

        text = tempText.trim();

        String tempForm = element.getAttributeValue("form");

        if (tempForm == null) {
            tempForm = "plain";
        }

        form = tempForm.trim();
    }

    /**
     * This method create a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRMetaLangText definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMetaLangText part
     */
    @Override
    public Element createXML() throws MCRException {
        Element elm = super.createXML();
        MCRUtils.filterTrimmedNotEmpty(form)
            .ifPresent(s -> elm.setAttribute("form", s));
        elm.addContent(text);

        return elm;
    }

    /**
     * Creates the JSON representation. Extends the {@link MCRMetaDefault#createJSON()} method
     * with the following data.
     * 
     * <pre>
     *   {
     *     text: "Hallo Welt",
     *     form: "plain"
     *   }
     * </pre>
     * 
     */
    @Override
    public JsonObject createJSON() {
        JsonObject obj = super.createJSON();
        if (getForm() != null) {
            obj.addProperty("form", getForm());
        }
        obj.addProperty("text", getText());
        return obj;
    }

    /**
     * Validates this MCRMetaLangText. This method throws an exception if:
     * <ul>
     * <li>the subtag is not null or empty</li>
     * <li>the lang value was supported</li>
     * <li>the inherited value is lower than zero</li>
     * <li>the trimmed text is null or empty</li>
     * </ul>
     * 
     * @throws MCRException the MCRMetaLangText is invalid
     */
    public void validate() throws MCRException {
        super.validate();
        text = MCRUtils.filterTrimmedNotEmpty(text)
            .orElseThrow(() -> new MCRException(getSubTag() + ": text is null or empty"));
        form = MCRUtils.filterTrimmedNotEmpty(form)
            .orElse("plain");
    }

    /**
     * clone of this instance
     * 
     * you will get a (deep) clone of this element
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public MCRMetaLangText clone() {
        MCRMetaLangText clone = (MCRMetaLangText) super.clone();

        clone.form = this.form;
        clone.text = this.text;

        return clone;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public final void debug() {
        if (LOGGER.isDebugEnabled()) {
            super.debugDefault();
            LOGGER.debug("Format             = {}", form);
            LOGGER.debug("Text               = {}", text);
            LOGGER.debug(" ");
        }
    }

    /**
     * Check the equivalence between this instance and the given object.
     * 
     * @param obj the MCRMetaLangText object
     * @return true if its equal
     */
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final MCRMetaLangText other = (MCRMetaLangText) obj;
        return Objects.equals(this.text, other.text) && Objects.equals(this.form, other.form);
    }
}
