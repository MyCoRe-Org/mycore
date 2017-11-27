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

/**
 * This class implements all methods for handling a name with the
 * MCRMetaPersonName datamodel. The MCRMetaPersonName class represents a natural
 * or juristic person specified by a list of names parts.
 * 
 * @author J. Vogler
 * @author J. Kupferschmidt
 * @version $Revision$ $Date: 2013-01-23 14:55:57 +0100 (Mi, 23. Jan
 *          2013) $
 */
public final class MCRMetaPersonName extends MCRMetaDefault {
    // MetaPerson data
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
     * @param set_subtag
     *            the name of the subtag
     * @param set_inherted
     *            a value &gt;= 0
     * 
     * @exception MCRException
     *                if the parameter values are invalid
     */
    public MCRMetaPersonName(String set_subtag, int set_inherted) throws MCRException {
        super(set_subtag, DEFAULT_LANGUAGE, "", set_inherted);
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
    public final String getFirstName() {
        return firstname;
    }

    /**
     * This method set the first name text element.
     */
    public final void setFirstName(String set_firstname) {
        if (set_firstname != null) {
            firstname = set_firstname.trim();
        } else {
            firstname = "";
        }
    }

    /**
     * This method get the call name text element.
     * 
     * @return the call name
     */
    public final String getCallName() {
        return callname;
    }

    /**
     * This method set the call name text element.
     */
    public final void setCallName(String set_callname) {
        if (set_callname != null) {
            callname = set_callname.trim();
        } else {
            callname = "";
        }
    }

    /**
     * This method get the surname text element.
     * 
     * @return the surname
     */
    public final String getSurName() {
        return surname;
    }

    /**
     * This method set the surname text element.
     */
    public final void setSurName(String set_surname) {
        if (set_surname != null) {
            surname = set_surname.trim();
        } else {
            surname = "";
        }
    }

    /**
     * This method get the full name text element.
     * 
     * @return the full name
     */
    public final String getFullName() {
        return fullname;
    }

    /**
     * This method set the full name text element.
     */
    public final void setFullName(String set_fullname) {
        if (set_fullname != null) {
            fullname = set_fullname.trim();
        } else {
            fullname = "";
        }
    }

    /**
     * This method get the academic text element.
     * 
     * @return the academic
     */
    public final String getAcademic() {
        return academic;
    }

    /**
     * This method set the academic text element.
     */
    public final void setAcademic(String set_academic) {
        if (set_academic != null) {
            academic = set_academic.trim();
        } else {
            academic = "";
        }
    }

    /**
     * This method get the peerage text element.
     * 
     * @return the peerage
     */
    public final String getPeerage() {
        return peerage;
    }

    /**
     * This method set the peerage text element.
     */
    public final void setPeerage(String set_peerage) {
        if (set_peerage != null) {
            peerage = set_peerage.trim();
        } else {
            peerage = "";
        }
    }

    /**
     * This method get the numeration text element.
     * 
     * @return the numeration
     */
    public final String getNumeration() {
        return numeration;
    }

    /**
     * This method set the numeration text element.
     */
    public final void setNumeration(String set_numeration) {
        if (set_numeration != null) {
            numeration = set_numeration.trim();
        } else {
            numeration = "";
        }
    }

    /**
     * This method get the title text element.
     * 
     * @return the title
     */
    public final String getTitle() {
        return title;
    }

    /**
     * This method set the title text element.
     */
    public final void setTitle(String set_title) {
        if (set_title != null) {
            title = set_title.trim();
        } else {
            title = "";
        }
    }

    /**
     * This method get the prefix text element.
     * 
     * @return the prefix
     */
    public final String getPrefix() {
        return prefix;
    }

    /**
     * This method set the prefix text element.
     */
    public final void setPrefix(String set_prefix) {
        if (set_prefix != null) {
            prefix = set_prefix.trim();
        } else {
            prefix = "";
        }
    }

    /**
     * This method get the affix text element.
     * 
     * @return the affix
     */
    public final String getAffix() {
        return affix;
    }

    /**
     * This method set the affix text element.
     */
    public final void setAffix(String set_affix) {
        if (set_affix != null) {
            affix = set_affix.trim();
        } else {
            affix = "";
        }
    }

    /**
     * This method reads the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    @Override
    public final void setFromDOM(org.jdom2.Element element) {
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
    public final org.jdom2.Element createXML() throws MCRException {
        Element elm = super.createXML();
        if ((firstname = firstname.trim()).length() != 0) {
            elm.addContent(new org.jdom2.Element("firstname").addContent(firstname));
        }

        if ((callname = callname.trim()).length() != 0) {
            elm.addContent(new org.jdom2.Element("callname").addContent(callname));
        }

        if ((fullname = fullname.trim()).length() != 0) {
            elm.addContent(new org.jdom2.Element("fullname").addContent(fullname));
        }

        if ((surname = surname.trim()).length() != 0) {
            elm.addContent(new org.jdom2.Element("surname").addContent(surname));
        }

        if ((academic = academic.trim()).length() != 0) {
            elm.addContent(new org.jdom2.Element("academic").addContent(academic));
        }

        if ((peerage = peerage.trim()).length() != 0) {
            elm.addContent(new org.jdom2.Element("peerage").addContent(peerage));
        }

        if ((numeration = numeration.trim()).length() != 0) {
            elm.addContent(new org.jdom2.Element("numeration").addContent(numeration));
        }

        if ((title = title.trim()).length() != 0) {
            elm.addContent(new org.jdom2.Element("title").addContent(title));
        }

        if ((prefix = prefix.trim()).length() != 0) {
            elm.addContent(new org.jdom2.Element("prefix").addContent(prefix));
        }

        if ((affix = affix.trim()).length() != 0) {
            elm.addContent(new org.jdom2.Element("affix").addContent(affix));
        }

        return elm;
    }

    /**
     * Validates this MCRMetaPersonName. This method throws an exception if:
     * <ul>
     * <li>the subtag is not null or empty</li>
     * <li>the lang value was supported</li>
     * <li>the inherited value is lower than zero</li>
     * <li>the firstname, the callname or the fullname is null</li>
     * </ul>
     * 
     * @throws MCRException the MCRMetaPersonName is invalid
     */
    public void validate() throws MCRException {
        super.validate();
        if (firstname == null || callname == null || fullname == null) {
            throw new MCRException(getSubTag() + ": one of fullname, callname or firstname is null.");
        }
        if ((firstname = firstname.trim()).length() == 0) {
            firstname = callname;
        }
        if ((callname = callname.trim()).length() == 0) {
            callname = firstname;
        }
        if ((fullname = fullname.trim()).length() == 0) {
            String sb = academic + ' ' + peerage + ' ' + firstname + ' ' + prefix + ' ' + surname;
            fullname = sb.trim();
            if (fullname.length() == 0) {
                throw new MCRException(getSubTag() + ": full name / first name or surname is empty");
            }
        }
    }

    /**
     * This method make a clone of this class.
     */
    @Override
    public final MCRMetaPersonName clone() {
        return new MCRMetaPersonName(subtag, inherited);
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public final void debug() {
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
}
