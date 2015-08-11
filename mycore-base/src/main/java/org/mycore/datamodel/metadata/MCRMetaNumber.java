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

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;

/**
 * Implements methods to handle MCRMetaNumber fields of a metadata object. 
 * The MCRMetaNumber class presents a number value in decimal
 * format and optional a type and a measurement. The number can have the format
 * <em>xxxx.xxx</em> or <em>xxxx,xxx</em>. Only three digits after the dot, 
 * and nine before are stored.
 * <p>
 * &lt;tag class="MCRMetaNumber" heritable="..."&gt; <br>
 * &lt;subtag type="..." xml:lang="..." measurement="..."&gt; <br>
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
    private double number;

    private String dimension;

    private String measurement;

    private static final Logger LOGGER = Logger.getLogger(MCRMetaNumber.class);

    /**
     * Sets the language element to <b>en</b>, the number to zero,
     * the measurement and the dimension to an empty string.
     */
    public MCRMetaNumber() {
        super();
        number = 0.;
    }

    /**
     * The language element was set. If the value of <em>default_lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
     * is null or empty an exception was throwed. The dimension element was set to
     * the value of <em>set_dimension<em>, if it is null, an empty string was set
     * to the type element. The measurement element was set to the value of
     * <em>set_measurement<em>, if it is null, an empty string was set
     * to the measurement element.  The number string <em>set_number</em>
     * was set to the number element, if it is null or not a number, a
     * MCRException was thowed.
     * @param set_subtag       the name of the subtag
     * @param set_inherted     a value &gt;= 0
     * @param set_dimension    the optional dimension string
     * @param set_measurement  the optional measurement string
     * @param set_number       the number string
     * @exception MCRException if the set_subtag value is null or empty or if
     *   the number string is not in a number format
     */
    public MCRMetaNumber(String set_subtag, int set_inherted, String set_dimension, String set_measurement, String set_number) throws MCRException {
        this(set_subtag, set_inherted, set_dimension, set_measurement, 0.);
        setNumber(set_number);
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
     * is null or empty an exception was throwed. The dimension element was set to
     * the value of <em>set_dimension<em>, if it is null, an empty string was set
     * to the type element. The measurement element was set to the value of
     * <em>set_measurement<em>, if it is null, an empty string was set
     * to the measurement element.  The number <em>set_number</em>
     * was set to the number element.
     * @param set_subtag       the name of the subtag
     * @param set_inherted     a value &gt;= 0
     * @param set_dimension    the optional dimension string
     * @param set_measurement  the optional measurement string
     * @param set_number       the number value
     * @exception MCRException if the set_subtag value is null or empty
     */
    public MCRMetaNumber(String set_subtag, int set_inherted, String set_dimension, String set_measurement, double set_number) throws MCRException {
        super(set_subtag, null, null, set_inherted);
        number = set_number;
        dimension = set_dimension;
        measurement = set_measurement;
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
     * @param set_dimension
     *            the dimension string
     */
    public final void setDimension(String set_dimension) {
        dimension = set_dimension;
    }

    /**
     * This method set the measurement, if it is null, an empty string was set
     * to the measurement element.
     * 
     * @param set_measurement
     *            the measurement string
     */
    public final void setMeasurement(String set_measurement) {
        measurement = set_measurement;
    }

    /**
     * This method set the number, if it is null or not a number, a MCRException
     * was thowed.
     * 
     * @param set_number
     *            the number string
     * @exception MCRException
     *                if the number string is not in a number format
     */
    public final void setNumber(String set_number) {
        try {
            if (set_number == null) {
                throw new MCRException("Number cannot be null");
            }
            String new_number = set_number.replace(',', '.');
            number = Double.parseDouble(new_number);
        } catch (NumberFormatException e) {
            throw new MCRException("The format of a number is invalid.");
        }
    }

    /**
     * This method set the number.
     * 
     * @param set_number
     *            the number value
     */
    public final void setNumber(double set_number) {
        number = set_number;
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

    public final double getNumber() {
        return number;
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

        elm.addContent(String.valueOf(number));

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
        LOGGER.debug("Value              = " + number);
        LOGGER.debug("");
    }
}
