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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;

/**
 * <p>
 * Implements methods to handle MCRMetaNumber fields of a metadata object.
 * The MCRMetaNumber class presents a number value in decimal
 * format and optional a dimension type and a measurement. The input number can have the format
 * like <em>xxxx</em>, <em>xxxx.x</em> or <em>xxxx,xxx</em>.
 * </p>
 * <p>
 * The length of the dimension type is limited by the property <em>MCR.Metadata.MetaNumber.DimensionLength</em>.
 * The default length is defined in MAX_DIMENSION_LENGTH = 128.
 * </p>
 * <p>
 * The length of the measurement type is limited by the property <em>MCR.Metadata.MetaNumber.MeasurementLength</em>.
 * The default length is defined in MAX_MEASURE_LENGTH = 64.
 * </p>
 * <p>
 * The String output format of the number is determined by the property
 * <em>MCR.Metadata.MetaNumber.FractionDigits</em>, default is DEFAULT_FRACTION_DIGITS = 3, and the ENGLISH Locale.
 * For more digits in fraction as defined in property <em>MCR.Metadata.MetaNumber.FractionDigits</em>
 * will be round the output!
 * </p>
 * <p>
 * For transforming the dot of the ENGLISH locale to other Characters use the tools of your layout process.
 * </p>
 * <pre>
 * &lt;tag class="MCRMetaNumber" heritable="..."&gt;
 *   &lt;subtag type="..." measurement="..." dimension="..."&gt;
 *     xxxx.xxx or xxx
 *   &lt;/subtag&gt;
 * &lt;/tag&gt;
 * </pre>
 *
 * @author Jens Kupferschmidt
 */
public final class MCRMetaNumber extends MCRMetaDefault {

    public static final int MAX_DIMENSION_LENGTH = 128;

    public static final int MAX_MEASUREMENT_LENGTH = 64;

    public static final int DEFAULT_FRACTION_DIGITS = 3;

    private BigDecimal number;

    private String dimension;

    private String measurement;

    private static final Logger LOGGER = LogManager.getLogger();

    private int fractionDigits;

    private int dimensionLength;

    private int measurementLength;

    private void loadProperties() {
        fractionDigits = MCRConfiguration2.getInt("MCR.Metadata.MetaNumber.FractionDigits")
            .orElse(DEFAULT_FRACTION_DIGITS);
        dimensionLength = MCRConfiguration2.getInt("MCR.Metadata.MetaNumber.DimensionLength")
            .orElse(MAX_DIMENSION_LENGTH);
        measurementLength = MCRConfiguration2.getInt("MCR.Metadata.MetaNumber.MeasurementLength")
            .orElse(MAX_MEASUREMENT_LENGTH);
    }

    /**
     * This is the constructor. <br>
     * Sets the number to zero, the measurement and the dimension to an empty string.
     */
    public MCRMetaNumber() {
        super();
        loadProperties();
        number = BigDecimal.ZERO;
        dimension = "";
        measurement = "";
    }

    /**
     * This is the constructor. <br>
     * The subtag element was set to the value of <em>subtag</em>.
     * If the value of <em>subtag</em>  is null or empty a MCRException will be thrown.
     * The dimension element was set to the value of <em>dimension</em>, if it is null,
     * an empty string was set to the dimension element.
     * The measurement element was set to the value of <em>measurement</em>, if it is null,
     * an empty string was set to the measurement element.
     * The number string <em>number</em> was set to the number element, if it is null, empty or not a number, a
     * MCRException will be thown.
     * @param subtag       the name of the subtag
     * @param inherted     a value &gt;= 0
     * @param dimension    the optional dimension string
     * @param measurement  the optional measurement string
     * @param number       the number string
     * @exception MCRException if the subtag value is null or empty or if
     *   the number string is not in a number format
     */
    public MCRMetaNumber(String subtag, int inherted, String dimension, String measurement, String number)
        throws MCRException {
        super(subtag, null, null, inherted);
        loadProperties();
        setDimension(dimension);
        setMeasurement(measurement);
        setNumber(number);
    }

    /**
     * This is the constructor. <br>
     * The subtag element was set to the value of <em>subtag</em>.
     * If the value of <em>subtag</em>  is null or empty a MCRException will be thrown.
     * The dimension element was set to the value of <em>dimension</em>, if it is null,
     * an empty string was set to the dimension element.
     * The measurement element was set to the value of <em>measurement</em>, if it is null,
     * an empty string was set to the measurement element.
     * The number <em>number</em> was set to the number element, if it is null a MCRException will be thrown.
     * @param subtag       the name of the subtag
     * @param inherted     a value &gt;= 0
     * @param dimension    the optional dimension string
     * @param measurement  the optional measurement string
     * @param number       the number string
     * @exception MCRException if the subtag value is null or empty or if
     *   the number string is not in a number format
     */
    public MCRMetaNumber(String subtag, int inherted, String dimension, String measurement, BigDecimal number)
        throws MCRException {
        super(subtag, null, null, inherted);
        loadProperties();
        setDimension(dimension);
        setMeasurement(measurement);
        setNumber(number);
    }

    /**
     * This method set the dimension, if it is null, an empty string was set to
     * the dimension element.
     *
     * @param dimension
     *            the dimension string
     */
    public void setDimension(String dimension) {
        if (dimension == null) {
            this.dimension = "";
        } else {
            if (dimension.length() > dimensionLength) {
                this.dimension = dimension.substring(dimensionLength);
                LOGGER.warn("{}: dimension is too long: {}", this::getSubTag, dimension::length);
            } else {
                this.dimension = dimension;
            }
        }
    }

    /**
     * This method set the measurement, if it is null, an empty string was set
     * to the measurement element.
     *
     * @param measurement
     *            the measurement string
     */
    public void setMeasurement(String measurement) {
        if (measurement == null) {
            this.measurement = "";
        } else {
            if (measurement.length() > measurementLength) {
                this.measurement = measurement.substring(measurementLength);
                LOGGER.warn("{}: measurement is too long: {}", this::getSubTag, measurement::length);
            } else {
                this.measurement = measurement;
            }
        }
    }

    /**
     * This method set the number, if it is null or not a number, a MCRException
     * will be throw.
     *
     * @param number
     *            the number string
     * @exception MCRException
     *                if the number string is not in a number format
     */
    public void setNumber(String number) throws MCRException {
        try {
            if (number == null) {
                throw new MCRException("Number cannot be null");
            }
            String tmpNumber = number.replace(',', '.');
            this.number = new BigDecimal(tmpNumber);
        } catch (NumberFormatException e) {
            throw new MCRException("The format of a number is invalid.", e);
        }
    }

    /**
     * This method set the number, if it is null a MCRException will be throw.
     *
     * @param number
     *            the number as BigDecimal
     * @exception MCRException
     *                if the number string is null
     */
    public void setNumber(BigDecimal number) throws MCRException {
        if (number == null) {
            throw new MCRException("Number cannot be null");
        }
        this.number = number;
    }

    /**
     * This method get the dimension element.
     *
     * @return the dimension String
     */
    public String getDimension() {
        return dimension;
    }

    /**
     * This method get the measurement element.
     *
     * @return the measurement String
     */
    public String getMeasurement() {
        return measurement;
    }

    /**
     * This method get the number element.
     *
     * @return the number as BigDecimal
     */
    public BigDecimal getNumberAsBigDecimal() {
        return number;
    }

    /**
     * This method get the number element as formatted String. The number of
     * fraction digits is defined by property <em>MCR.Metadata.MetaNumber.FractionDigits</em>.
     * The default is 3 fraction digits.
     *
     * @return the number as formatted String
     */
    public String getNumberAsString() {
        final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);
        numberFormat.setGroupingUsed(false);
        numberFormat.setMaximumFractionDigits(fractionDigits);
        numberFormat.setMinimumFractionDigits(fractionDigits);
        return numberFormat.format(number);
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
        setMeasurement(element.getAttributeValue("measurement"));
        setDimension(element.getAttributeValue("dimension"));
        setNumber(element.getTextTrim());
    }

    /**
     * This method creates an XML element containing all data in this instance,
     * as defined by the MyCoRe XML MCRNumber definition for the given subtag.
     *
     * @exception MCRException
     *                if the content of this instance is not valid
     * @return a JDOM Element with the XML MCRNumber part
     */
    @Override
    public Element createXML() throws MCRException {
        Element elm = super.createXML();
        if (dimension != null && dimension.length() != 0) {
            elm.setAttribute("dimension", dimension);
        }

        if (measurement != null && measurement.length() != 0) {
            elm.setAttribute("measurement", measurement);
        }
        elm.addContent(getNumberAsString());
        return elm;
    }

    @Override
    public MCRMetaNumber clone() {
        MCRMetaNumber clone = (MCRMetaNumber) super.clone();

        clone.dimension = this.dimension;
        clone.measurement = this.measurement;
        clone.number = this.number;
        clone.dimensionLength = this.dimensionLength;
        clone.measurementLength = this.measurementLength;
        clone.fractionDigits = this.fractionDigits;

        return clone;
    }

    /**
     * Logs debug output.
     */
    @Override
    public void debug() {
        if (LOGGER.isDebugEnabled()) {
            super.debugDefault();
            LOGGER.debug("Measurement        = {}", measurement);
            LOGGER.debug("Dimension          = {}", dimension);
            LOGGER.debug("Value              = {}", number.toPlainString());
            LOGGER.debug("");
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
            + Objects.hash(dimension, dimensionLength, fractionDigits, measurement, measurementLength, number);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final MCRMetaNumber other = (MCRMetaNumber) obj;
        return Objects.equals(dimension, other.dimension)
            && dimensionLength == other.dimensionLength
            && fractionDigits == other.fractionDigits
            && Objects.equals(measurement, other.measurement)
            && measurementLength == other.measurementLength
            && Objects.equals(number, other.number);
    }

}
