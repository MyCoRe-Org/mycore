/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.datamodel.metadata;

import java.util.Date;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.common.MCRISO8601Format;

import com.google.gson.JsonObject;

/**
 * provides support for a restricted range of formats, all of which are valid
 * ISO 8601 dates and times.
 * 
 * The range of supported formats is exactly the same range that is suggested by
 * the W3C <a href="http://www.w3.org/TR/NOTE-datetime">datetime profile</a> in
 * its version from 1997-09-15.
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 1.3
 */
public final class MCRMetaISO8601Date extends MCRMetaDefault {

    private Element export;

    private boolean changed = true;

    private static final Namespace DEFAULT_NAMESPACE = Namespace.NO_NAMESPACE;

    private MCRISO8601Date isoDate;

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * constructs a empty instance.
     * 
     * @see MCRMetaDefault#MCRMetaDefault()
     */
    public MCRMetaISO8601Date() {
        super();
        this.isoDate = new MCRISO8601Date();
    }

    /**
     * same as superImplentation but sets lang attribute to "null"
     * 
     * @see MCRMetaDefault#MCRMetaDefault(String, String, String, int)
     */
    public MCRMetaISO8601Date(String set_subtag, String set_type, int set_inherted) {
        super(set_subtag, null, set_type, set_inherted);
        this.isoDate = new MCRISO8601Date();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.metadata.MCRMetaDefault#createXML()
     */
    @Override
    public Element createXML() throws MCRException {
        if (!changed) {
            return (Element) export.clone();
        }
        Element elm = super.createXML();
        if (!(isoDate.getIsoFormat() == null || isoDate.getIsoFormat() == MCRISO8601Format.COMPLETE_HH_MM_SS_SSS)) {
            elm.setAttribute("format", isoDate.getIsoFormat().toString());
        }
        elm.setText(getISOString());
        export = elm;
        changed = false;
        return (Element) export.clone();
    }

    /**
     * Creates the JSON representation. Extends the {@link MCRMetaDefault#createJSON()} method
     * with the following data.
     * 
     * <pre>
     *   {
     *     date: "2016-02-08",
     *     format: "YYYY-MM-DD"
     *   }
     * </pre>
     */
    @Override
    public JsonObject createJSON() {
        JsonObject obj = super.createJSON();
        obj.addProperty("date", getISOString());
        if (isoDate.getIsoFormat() != null) {
            obj.addProperty("format", isoDate.getIsoFormat().toString());
        }
        return obj;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.metadata.MCRMetaDefault#setFromDOM(org.jdom2.Element)
     */
    @Override
    public void setFromDOM(org.jdom2.Element element) {
        super.setFromDOM(element);
        setFormat(element.getAttributeValue("format"));
        setDate(element.getTextTrim());
        export = (Element) element.clone();
    }

    /**
     * returns the namespace of this element
     * 
     * @return Returns the ns.
     */
    protected static Namespace getNs() {
        return DEFAULT_NAMESPACE;
    }

    /**
     * sets the date for this meta data object
     * 
     * @param isoString
     *            Date in any form that is a valid W3C dateTime
     */
    public final void setDate(String isoString) {
        isoDate.setDate(isoString);
    }

    /**
     * returns the Date representing this element.
     * 
     * @return a new Date instance of the time set in this element
     */
    public final Date getDate() {
        return isoDate.getDate();
    }

    /**
     * sets the date for this meta data object
     * 
     * @param dt
     *            Date object representing date String in Element
     */
    public void setDate(Date dt) {
        isoDate.setDate(dt);
    }

    /**
     * returns a ISO 8601 conform String using the current set format.
     * 
     * @return date in ISO 8601 format, or null if date is unset.
     */
    public final String getISOString() {
        return isoDate.getISOString();
    }

    /**
     * sets the input and output format.
     * 
     * please use only the formats defined on the <a
     * href="http://www.w3.org/TR/NOTE-datetime">W3C Page</a>, which are also
     * exported as static fields by this class.
     * 
     * @param format
     *            a format string that is valid conforming to xsd:duration
     *            schema type.
     * 
     */
    public void setFormat(String format) {
        isoDate.setFormat(format);
    }

    public String getFormat() {
        return isoDate == null || isoDate.getIsoFormat() == null ? null : isoDate.getIsoFormat().toString();
    }

    /**
     * Returns the internal date.
     * 
     * @return the base date
     */
    public MCRISO8601Date getMCRISO8601Date() {
        return isoDate;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public void debug() {
        if (LOGGER.isDebugEnabled()) {
            super.debugDefault();
            LOGGER.debug("Date=" + (isoDate != null ? isoDate.getISOString() : "null"));
            if (isoDate != null) {
                MCRISO8601Format isoFormat = isoDate.getIsoFormat();
                LOGGER.debug("Format=" + isoFormat.toString());
            }
        }
    }

    /**
     * clone of this instance
     * 
     * you will get a (deep) clone of this element
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public MCRMetaISO8601Date clone() {
        MCRMetaISO8601Date out = new MCRMetaISO8601Date();
        out.setFromDOM((Element) createXML().clone());
        return out;
    }

    /**
     * Validates this MCRMetaISO8601Date. This method throws an exception if:
     * <ul>
     * <li>the subtag is not null or empty</li>
     * <li>the lang value was supported</li>
     * <li>the inherited value is lower than zero</li>
     * <li>the isoDate or the temporal accessor is null</li>
     * </ul>
     * 
     * @throws MCRException the MCRMetaISO8601Date is invalid
     */
    public void validate() throws MCRException {
        super.validate();
        if (isoDate == null || isoDate.getDt() == null) {
            throw new MCRException(getSubTag() + ": date is invalid");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final MCRMetaISO8601Date other = (MCRMetaISO8601Date) obj;
        return Objects.equals(this.isoDate, other.isoDate);
    }

}
