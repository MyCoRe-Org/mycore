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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRException;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.common.MCRISO8601Format;

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

    private static final Logger LOGGER = Logger.getLogger(MCRMetaISO8601Date.class);

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

    /**
     * sets the input and output format.
     * 
     * please use only the formats defined on the <a
     * href="http://www.w3.org/TR/NOTE-datetime">W3C Page</a>, which are also
     * exported as static fields by this class.
     * 
     */
    @Deprecated
    public void setFormat(MCRISO8601Format isoFormat) {
        isoDate.setFormat(isoFormat);
    }

    /**
     * formats the date to a String
     * @param format as in {@link DateTimeFormat}
     * @param locale used by format process
     * @return null if date is not set yet
     */
    @Deprecated
    public String format(String format, Locale locale) {
        return isoDate.format(format, locale);
    }

    /**
     * gets the input and output format.
     * 
     * this is a String that is also exported as static fields by this class, or
     * null if not defined.
     * 
     * @return a format string that is valid conforming to xsd:duration schema
     *         type, or null if not defined.
     * 
     */
    @Deprecated
    public MCRISO8601Format getFormat() {
        return isoDate.getIsoFormat();
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public void debug() {
        LOGGER.debug("Start Class : MCRMetaISO8601Date");
        super.debugDefault();
        LOGGER.debug("Date=" + isoDate.getISOString());
        LOGGER.debug("Format=" + isoDate.getIsoFormat().toString());
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        StringWriter sw = new StringWriter();
        try {
            xout.output(export, sw);
            LOGGER.debug("JDOM=" + sw.toString());
        } catch (IOException e) {
            //ignore
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
     * checks the formal correctness of this element.
     * 
     * This check includes:
     * <ol>
     * <li>the included date is set</li>
     * <li>the super implementation returns true</li>
     * </ol>
     * 
     * @see MCRMetaDefault#isValid()
     * @return false, if any test fails and the instance should not be used for
     *         persistence purposes
     */
    @Override
    public boolean isValid() {
        if (!super.isValid()) {
            return false;
        }
        if (isoDate.getDt() == null) {
            LOGGER.warn(getSubTag() + ": date is invalid");
            return false;
        }
        return true;
    }
}
