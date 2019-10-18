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

import com.google.gson.JsonObject;

/**
 * This class implements all methods for handling with the MCRMetaAddress part
 * of a metadata object. The MCRMetaAddress class represents a natural address
 * specified by a list of names.
 * 
 * @author J. Vogler
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
     * The language element was set. If the value of <em>defaultLang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>subtag</em>. If the value of <em>subtag</em>
     * is null or empty an exception was thrown. The type element was set to
     * the value of <em>type</em>, if it is null, an empty string was set
     * to the type element. The country, state, zipCode, city, street and
     * number element was set to the value of <em>...</em>, if they are null,
     * an empty string was set to this element.
     * @param subtag      the name of the subtag
     * @param defaultLang    the default language
     * @param type        the optional type string
     * @param inherted    a value &gt;= 0
     * @param country     the country name
     * @param state       the state name
     * @param zipcode     the zipCode string
     * @param city        the city name
     * @param street      the street name
     * @param number      the number string
     *
     * @exception MCRException if the parameter values are invalid
     */
    public MCRMetaAddress(final String subtag, final String defaultLang, final String type,
        final int inherted, final String country,
        final String state, final String zipcode, final String city, final String street,
        final String number) throws MCRException {
        super(subtag, defaultLang, type, inherted);
        setCountry(country);
        setState(state);
        setZipCode(zipcode);
        setCity(city);
        setStreet(street);
        setNumber(number);
    }

    /**
     * clone of this instance
     * 
     * you will get a (deep) clone of this element
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public MCRMetaAddress clone() {
        MCRMetaAddress clone = (MCRMetaAddress) super.clone();

        clone.country = this.country;
        clone.state = this.state;
        clone.zipCode = this.zipCode;
        clone.city = this.city;
        clone.street = this.street;
        clone.number = this.number;

        return clone;
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
    public Element createXML() throws MCRException {
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
    public void debug() {
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
     * Check the equivalence between this instance and the given object.
     * 
     * @param obj the MCRMetaAddress object
     * @return true if its equal
     */
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final MCRMetaAddress other = (MCRMetaAddress) obj;
        return Objects.equals(this.country, other.country) && Objects.equals(this.state, other.state)
            && Objects.equals(this.zipCode, other.zipCode) && Objects.equals(this.city, other.city)
            && Objects.equals(this.street, other.street) && Objects.equals(this.number, other.number);
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
    public void setFromDOM(final Element element) {
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
