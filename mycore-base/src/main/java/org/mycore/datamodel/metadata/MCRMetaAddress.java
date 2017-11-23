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
 * This class implements all methods for handling with the MCRMetaAddress part
 * of a metadata object. The MCRMetaAddress class represents a natural address
 * specified by a list of names.
 * 
 * @author J. Vogler
 * @version $Revision$ $Date$
 */
public final class MCRMetaAddress extends MCRMetaDefault {
    // MetaAddress data
    private String country;

    private String state;

    private String zipCode;

    private String city;

    private String street;

    private String number;

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. All other elemnts are set to
     * an empty string.
     */
    public MCRMetaAddress() {
        super();
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>set_subtag</em>. If the value of <em>set_subtag</em>
     * is null or empty an exception was throwed. The type element was set to
     * the value of <em>set_type</em>, if it is null, an empty string was set
     * to the type element. The country, state, zipCode, city, street and
     * number element was set to the value of <em>set_...</em>, if they are null,
     * an empty string was set to this element.
     * @param set_subtag      the name of the subtag
     * @param default_lang    the default language
     * @param set_type        the optional type string
     * @param set_inherted    a value &gt;= 0
     * @param set_country     the country name
     * @param set_state       the state name
     * @param set_zipcode     the zipCode string
     * @param set_city        the city name
     * @param set_street      the street name
     * @param set_number      the number string
     *
     * @exception MCRException if the parameter values are invalid
     */
    public MCRMetaAddress(final String set_subtag, final String default_lang, final String set_type,
        final int set_inherted, final String set_country,
        final String set_state, final String set_zipcode, final String set_city, final String set_street,
        final String set_number) throws MCRException {
        super(set_subtag, default_lang, set_type, set_inherted);
        country = set_country;
        state = set_state;
        zipCode = set_zipcode;
        city = set_city;
        street = set_street;
        number = set_number;
    }

    /**
     * This method make a clone of this class.
     */
    @Override
    public MCRMetaAddress clone() {
        return new MCRMetaAddress(subtag, DEFAULT_LANGUAGE, type, inherited, country, state, zipCode, city, street,
            number);
    }

    /**
     * This method creates a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRMetaAddress definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMetaAddress part
     */
    @Override
    public final org.jdom2.Element createXML() throws MCRException {
        final Element elm = super.createXML();
        if (getCountry() != null) {
            elm.addContent(new Element("country").addContent(getCountry()));
        }
        if (getState() != null) {
            elm.addContent(new Element("state").addContent(getState()));
        }
        if (getZipCode() != null) {
            elm.addContent(new Element("zipcode").addContent(getZipCode()));
        }
        if (getCity() != null) {
            elm.addContent(new Element("city").addContent(getCity()));
        }
        if (getStreet() != null) {
            elm.addContent(new Element("street").addContent(getStreet()));
        }
        if (getNumber() != null) {
            elm.addContent(new Element("number").addContent(getNumber()));
        }

        return elm;
    }

    /**
     * Creates the JSON representation. Extends the {@link MCRMetaDefault#createJSON()} method
     * with the following data.
     * 
     * <pre>
     *   {
     *     "country": "Deutschland",
     *     "state": "Thüringen",
     *     "zipcode": "07743",
     *     "city": "Jena",
     *     "street": "Bibliothekspl.",
     *     "number": "2"
     *   }
     * </pre>
     * 
     */
    @Override
    public JsonObject createJSON() {
        JsonObject obj = super.createJSON();
        if (getCountry() != null) {
            obj.addProperty("country", getCountry());
        }
        if (getState() != null) {
            obj.addProperty("state", getState());
        }
        if (getZipCode() != null) {
            obj.addProperty("zipcode", getZipCode());
        }
        if (getCity() != null) {
            obj.addProperty("city", getCity());
        }
        if (getStreet() != null) {
            obj.addProperty("street", getStreet());
        }
        if (getNumber() != null) {
            obj.addProperty("number", getNumber());
        }
        return obj;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public final void debug() {
        if (LOGGER.isDebugEnabled()) {
            super.debugDefault();
            LOGGER.debug("Country            = {}", country);
            LOGGER.debug("State              = {}", state);
            LOGGER.debug("Zipcode            = {}", zipCode);
            LOGGER.debug("City               = {}", city);
            LOGGER.debug("Street             = {}", street);
            LOGGER.debug("Number             = {}", number);
            LOGGER.debug(" ");
        }
    }

    /**
     * @return the city
     */
    public String getCity() {
        return city;
    }

    /**
     * @return the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * @return the number
     */
    public String getNumber() {
        return number;
    }

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @return the street
     */
    public String getStreet() {
        return street;
    }

    /**
     * @return the zipCode
     */
    public String getZipCode() {
        return zipCode;
    }

    /**
     * Validates this MCRMetaAddress. This method throws an exception if:
     * <ul>
     * <li>the subtag is not null or empty</li>
     * <li>the lang value was supported</li>
     * <li>the inherited value is lower than zero</li>
     * <li>all of country, state, zip, city, street and number is empty</li>
     * </ul>
     * 
     * @throws MCRException the MCRMetaAddress is invalid
     */
    public void validate() throws MCRException {
        super.validate();
        if (getCountry() == null && getState() == null && getZipCode() == null && getCity() == null
            && getStreet() == null && getNumber() == null) {
            throw new MCRException(getSubTag() + ": address is empty");
        }
    }

    /**
     * @param city the city to set
     */
    public void setCity(final String city) {
        this.city = city;
    }

    /**
     * @param country the country to set
     */
    public void setCountry(final String country) {
        this.country = country;
    }

    /**
     * This method reads the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    @Override
    public final void setFromDOM(final org.jdom2.Element element) {
        super.setFromDOM(element);
        country = element.getChildTextTrim("country");
        state = element.getChildTextTrim("state");
        zipCode = element.getChildTextTrim("zipcode");
        city = element.getChildTextTrim("city");
        street = element.getChildTextTrim("street");
        number = element.getChildTextTrim("number");
    }

    /**
     * @param number the number to set
     */
    public void setNumber(final String number) {
        this.number = number;
    }

    /**
     * @param state the state to set
     */
    public void setState(final String state) {
        this.state = state;
    }

    /**
     * @param street the street to set
     */
    public void setStreet(final String street) {
        this.street = street;
    }

    /**
     * @param zipCode the zipCode to set
     */
    public void setZipCode(final String zipCode) {
        this.zipCode = zipCode;
    }
}
