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

package org.mycore.datamodel.metadata;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;

/**
 * Implements methods to handle MCRMetaNumber fields of a metadata object. 
 * The MCRMetaNumber class presents a number value in decimal
 * format and optional a type and a measurement. The input number can have the format
 * like <em>xxxx.x</em> or <em>xxxx,xxx</em>. <br />
 * The String output format of the numer is determined by the property
 * <em>MCR.Metadata.MetaNumber.FractionDigits</em> and the default Locale.
 * For more digits in fraction as defind for output the system will round the number!
 * <p>
 * &lt;tag class="MCRMetaNumber" heritable="..."&gt; <br>
 * &lt;subtag type="..." measurement="..." dimension="..."&gt; <br>
 * xxxx.xxx or xxx <br>
 * &lt;/subtag&gt; <br>
 * &lt;/tag&gt; <br>
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
final public class MCRMetaNumber extends MCRMetaDefault {
    /** The length of the attributes * */
    public static final int MAX_DIMENSION_LENGTH = 128;

    public static final int MAX_MEASUREMENT_LENGTH = 64;

    // MCRMetaNumber data
    private BigDecimal number;

    private String dimension;

    private String measurement;

    private static final Logger LOGGER = Logger.getLogger(MCRMetaNumber.class);
    
    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();
    private int FRACTION_DIGITS;

    /**
     * This is the constructor. <br>
     * Sets the number to zero, the measurement and the dimension to an empty string.
     */
    public MCRMetaNumber() {
        super();
        FRACTION_DIGITS = CONFIG.getInt("MCR.Metadata.MetaNumber.FractionDigits",3);
        number = new BigDecimal("0");
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
        FRACTION_DIGITS = CONFIG.getInt("MCR.Metadata.MetaNumber.FractionDigits",3);
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
     * The number <em>number</em> was convert to the number element, if it is null a MCRException will be thrown.
     * @param subtag       the name of the subtag
     * @param inherted     a value &gt;= 0
     * @param dimension    the optional dimension string
     * @param measurement  the optional measurement string
     * @param number       the number string
     * @exception MCRException if the subtag value is null or empty or if
     *   the number string is not in a number format
     */
    @Deprecated
    public MCRMetaNumber(String subtag, int inherted, String dimension, String measurement, double number)
        throws MCRException {
        super(subtag, null, null, inherted);
        setDimension(dimension);
        setMeasurement(measurement);
        setNumber(number);
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.metadata.MCRMetaDefault#isValid()
     */
    @Override
    public boolean isValid() {
        if (!super.isValid()) {
            return false;
        }

        if (dimension != null && dimension.length() > MAX_DIMENSION_LENGTH) {
            LOGGER.warn(getSubTag() + ": dimension is too long: " + dimension.length());
            return false;
        }
        if (measurement != null && measurement.length() > MAX_MEASUREMENT_LENGTH) {
            LOGGER.warn(getSubTag() + ": measurement is too long: " + measurement.length());
            return false;
        }
        return true;
    }

    /**
     * This method set the dimension, if it is null, an empty string was set to
     * the dimension element.
     * 
     * @param dimension
     *            the dimension string
     */
    public final void setDimension(String dimension) {
        if (dimension == null) {
            this.dimension = "";
        } else {
            this.dimension = dimension;
        }
    }

    /**
     * This method set the measurement, if it is null, an empty string was set
     * to the measurement element.
     * 
     * @param measurement
     *            the measurement string
     */
    public final void setMeasurement(String measurement) {
        if (measurement == null) {
            this.measurement = "";
        } else {
            this.measurement = measurement;
        }
    }

    /**
     * This method set the number, if it is null or not a number, a MCRException
     * was thowed.
     * 
     * @param number
     *            the number string
     * @exception MCRException
     *                if the number string is not in a number format
     */
    public final void setNumber(String number) {
        try {
            if (number == null) {
                throw new MCRException("Number cannot be null");
            }
            String tmp_number = number.replace(',', '.');
            this.number = new BigDecimal(tmp_number);
        } catch (NumberFormatException e) {
            throw new MCRException("The format of a number is invalid.");
        }
    }

    /**
     * This method set the number.
     * 
     * @param number
     *            the number value as double datatype
     */
    @Deprecated
    public final void setNumber(double number) {
        try {
            this.number = new BigDecimal(number);
        } catch (NumberFormatException e) {
            throw new MCRException("The format of a number is invalid.");
        }
    }

    /**
     * This method set the number, if it is null a MCRException was thowed.
     * 
     * @param number
     *            the number as BigDecimal
     * @exception MCRException
     *                if the number string is null
     */
    public final void setNumber(BigDecimal number) {
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
    public final String getDimension() {
        return dimension;
    }

    /**
     * This method get the measurement element.
     * 
     * @return the measurement String
     */
    public final String getMeasurement() {
        return measurement;
    }

    /**
     * This method get the number element.
     * 
     * @return the number converted to a double
     */
    public final double getNumber() {
        return number.doubleValue();
    }

    /**
     * This method get the number element.
     * 
     * @return the number as BigDecimal
     */
    public final BigDecimal getNumberAsBigDecimal() {
        return number;
    }

    /**
     * This method get the number element as formatted String. The number of
     * fraction digits is defined by property MCR.Metadata.MetaNumber.FractionDigits
     * 
     * @return the number as formatted String
     */
    public final String getNumberAsString() {
        final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        numberFormat.setGroupingUsed(false);
        numberFormat.setMaximumFractionDigits(FRACTION_DIGITS);
        numberFormat.setMinimumFractionDigits(FRACTION_DIGITS);
        return  numberFormat.format(number);
    }
    
    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    @Override
    public final void setFromDOM(org.jdom2.Element element) {
        super.setFromDOM(element);
        measurement = element.getAttributeValue("measurement");
        dimension = element.getAttributeValue("dimension");
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
    public final org.jdom2.Element createXML() throws MCRException {
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
    public final MCRMetaNumber clone() {
        return new MCRMetaNumber(subtag, inherited, dimension, measurement, number);
    }

    /**
     * Logs debug output.
     */
    @Override
    public final void debug() {
        super.debugDefault();
        LOGGER.debug("Measurement        = " + measurement);
        LOGGER.debug("Dimension          = " + dimension);
        LOGGER.debug("Value              = " + number.toPlainString());
        LOGGER.debug("");
    }
}
