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
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;

/**
 * This class implements all methods for handling a name with the
 * MCRMetaPersonName datamodel. The MCRMetaPersonName class represents a natural
 * or legal person specified by a list of names parts.
 *
 * @author J. Vogler
 * @author J. Kupferschmidt
 */
public final class MCRMetaPersonName extends MCRMetaDefault {

    private String firstname;

    private String callname;

    private String surname;

    private String fullname;

    private String academic;

    private String peerage;

    private String numeration;

    private String title;

    private String prefix;

    private String affix;

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This is the constructor. <br>
     * The language element was set to <b>application default language</b>. All
     * other elements will be set to an empty string.
     */
    public MCRMetaPersonName() {
        super();
        lang = DEFAULT_LANGUAGE;
        firstname = "";
        callname = "";
        surname = "";
        fullname = "";
        academic = "";
        peerage = "";
        numeration = "";
        title = "";
        prefix = "";
        affix = "";
    }

    /**
     * This is the constructor. <br>
     * The method set all common fields of all MCRMetaXXX datamodel types. It
     * set the language to the <b>application default language</b>. The type
     * attribute will be set to an empty String.
     *
     * @param subtag
     *            the name of the subtag
     * @param inherited
     *            a value &gt;= 0
     *
     * @exception MCRException
     *                if the parameter values are invalid
     */
    public MCRMetaPersonName(String subtag, int inherited) throws MCRException {
        super(subtag, DEFAULT_LANGUAGE, "", inherited);
        type = "";
        firstname = "";
        callname = "";
        surname = "";
        fullname = "";
        academic = "";
        peerage = "";
        numeration = "";
        title = "";
        prefix = "";
        affix = "";
    }

    /**
     * This method get the first name text element.
     *
     * @return the first name
     */
    public String getFirstName() {
        if (firstname != null && !firstname.isEmpty()) {
            return firstname;
        }
        return callname;
    }

    /**
     * This method set the first name text element.
     */
    public void setFirstName(String firstname) {
        this.firstname = firstname != null ? firstname.trim() : "";
    }

    /**
     * This method get the call name text element.
     *
     * @return the call name
     */
    public String getCallName() {
        if (callname != null && !callname.isEmpty()) {
            return callname;
        }
        return firstname;
    }

    /**
     * This method set the call name text element.
     */
    public void setCallName(String callName) {
        this.callname = callName != null ? callName.trim() : "";
    }

    /**
     * This method get the surname text element.
     *
     * @return the surname
     */
    public String getSurName() {
        return surname;
    }

    /**
     * This method set the surname text element.
     */
    public void setSurName(String surname) {
        this.surname = surname != null ? surname.trim() : "";
    }

    /**
     * This method get the full name text element.
     *
     * @return the full name
     */
    public String getFullName() {
        if (fullname != null && !fullname.isEmpty()) {
            return fullname;
        }
        String sb = getAcademic() + " " + getPeerage() + " " + getFirstName() + " " + getPrefix() + " " + getSurName();
        return sb.trim();
    }

    /**
     * This method set the full name text element.
     */
    public void setFullName(String fullName) {
        this.fullname = fullName != null ? fullName.trim() : "";
    }

    /**
     * This method get the academic text element.
     *
     * @return the academic
     */
    public String getAcademic() {
        return academic;
    }

    /**
     * This method set the academic text element.
     */
    public void setAcademic(String academic) {
        this.academic = academic != null ? academic.trim() : "";
    }

    /**
     * This method get the peerage text element.
     *
     * @return the peerage
     */
    public String getPeerage() {
        return peerage;
    }

    /**
     * This method set the peerage text element.
     */
    public void setPeerage(String peerage) {
        this.peerage = peerage != null ? peerage.trim() : "";
    }

    /**
     * This method get the numeration text element.
     *
     * @return the numeration
     */
    public String getNumeration() {
        return numeration;
    }

    /**
     * This method set the numeration text element.
     */
    public void setNumeration(String numeration) {
        this.numeration = numeration != null ? numeration.trim() : "";
    }

    /**
     * This method get the title text element.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * This method set the title text element.
     */
    public void setTitle(String title) {
        this.title = title != null ? title.trim() : "";
    }

    /**
     * This method get the prefix text element.
     *
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * This method set the prefix text element.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix != null ? prefix.trim() : "";
    }

    /**
     * This method get the affix text element.
     *
     * @return the affix
     */
    public String getAffix() {
        return affix;
    }

    /**
     * This method set the affix text element.
     */
    public void setAffix(String affix) {
        this.affix = affix != null ? affix.trim() : "";
    }

    /**
     * This method reads the XML input stream part from a DOM part for the
     * metadata of the document.
     *
     * @param element
     *            a relevant JDOM element for the metadata
     */
    @Override
    public void setFromDOM(Element element) {
        super.setFromDOM(element);
        setFirstName(element.getChildTextTrim("firstname"));
        setCallName(element.getChildTextTrim("callname"));
        setSurName(element.getChildTextTrim("surname"));
        setFullName(element.getChildTextTrim("fullname"));
        setAcademic(element.getChildTextTrim("academic"));
        setPeerage(element.getChildTextTrim("peerage"));
        setNumeration(element.getChildTextTrim("numeration"));
        setTitle(element.getChildTextTrim("title"));
        setPrefix(element.getChildTextTrim("prefix"));
        setAffix(element.getChildTextTrim("affix"));
    }

    /**
     * This method creates a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRMetaPersonName definition for the given subtag.
     *
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMetaPersonName part
     */
    @Override
    public Element createXML() throws MCRException {
        Element elm = super.createXML();
        BiConsumer<String, String> addContent = (name, value) -> MCRUtils.filterTrimmedNotEmpty(value)
            .ifPresent(trimmedValue -> elm.addContent(new Element(name).addContent(trimmedValue)));
        addContent.accept("firstname", firstname);
        addContent.accept("callname", callname);
        addContent.accept("fullname", fullname);
        addContent.accept("surname", surname);
        addContent.accept("academic", academic);
        addContent.accept("peerage", peerage);
        addContent.accept("numeration", numeration);
        addContent.accept("title", title);
        addContent.accept("prefix", prefix);
        addContent.accept("affix", affix);

        return elm;
    }

    /**
     * Validates this MCRMetaPersonName. This method throws an exception if:
     * <ul>
     * <li>the subtag is not null or empty</li>
     * <li>the lang value was supported</li>
     * <li>the inherited value is lower than zero</li>
     * <li>getFullName() returns an empty string</li>
     * </ul>
     *
     * @throws MCRException the MCRMetaPersonName is invalid
     */
    @Override
    public void validate() throws MCRException {
        super.validate();
        if (getFullName().isEmpty()) {
            throw new MCRException(getSubTag() + ": full name is empty");
        }
    }

    /**
     * Clone of this instance. You will get a (deep) clone of this element.
     *
     * @see java.lang.Object#clone()
     */
    @Override
    public MCRMetaPersonName clone() {
        MCRMetaPersonName clone = (MCRMetaPersonName) super.clone();

        clone.firstname = this.firstname;
        clone.callname = this.callname;
        clone.surname = this.surname;
        clone.fullname = this.fullname;
        clone.academic = this.academic;
        clone.peerage = this.peerage;
        clone.numeration = this.numeration;
        clone.title = this.title;
        clone.prefix = this.prefix;
        clone.affix = this.affix;

        return clone;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public void debug() {
        if (LOGGER.isDebugEnabled()) {
            super.debugDefault();
            LOGGER.debug("First name         = {}", firstname);
            LOGGER.debug("Call name          = {}", callname);
            LOGGER.debug("Surname            = {}", surname);
            LOGGER.debug("Full name          = {}", fullname);
            LOGGER.debug("Academic           = {}", academic);
            LOGGER.debug("Peerage            = {}", peerage);
            LOGGER.debug("Numeration         = {}", numeration);
            LOGGER.debug("Title              = {}", title);
            LOGGER.debug("Prefix             = {}", prefix);
            LOGGER.debug("Affix              = {}", affix);
            LOGGER.debug("");
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
            + Objects.hash(academic, affix, callname, firstname, fullname, numeration, peerage, prefix, surname, title);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final MCRMetaPersonName other = (MCRMetaPersonName) obj;
        return Objects.equals(academic, other.academic)
            && Objects.equals(affix, other.affix)
            && Objects.equals(callname, other.callname)
            && Objects.equals(firstname, other.firstname)
            && Objects.equals(fullname, other.fullname)
            && Objects.equals(numeration, other.numeration)
            && Objects.equals(peerage, other.peerage)
            && Objects.equals(prefix, other.prefix)
            && Objects.equals(surname, other.surname)
            && Objects.equals(title, other.title);
    }
}
