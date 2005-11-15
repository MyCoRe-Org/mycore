/**
 * $RCSfile$
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

import org.jdom.Element;
import org.jdom.Namespace;

import org.mycore.common.MCRException;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRMetaDateFragment extends MCRMetaDefault implements MCRMetaInterface {

    private int year;

    private int month;

    private int day;

    protected final static int defaultValue = 0;

    private Element export;

    private boolean changed = true;

    private static final Namespace ns = Namespace.NO_NAMESPACE;

    private static final String YEAR_TAG = "year";

    private static final String MONTH_TAG = "month";

    private static final String DAY_TAG = "day";

    /**
     *  
     */
    public MCRMetaDateFragment() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.datamodel.metadata.MCRMetaDefault#createXML()
     */
    public Element createXML() throws MCRException {
        if (!changed) {
            return (Element)export.clone();
        }
        if (!isValid()) {
            debug();
            throw new MCRException("The content of MCRMetaXML is not valid.");
        }
        export = new org.jdom.Element(subtag, ns);
        export.setAttribute("lang", lang, Namespace.XML_NAMESPACE);
        export.setAttribute("inherited", (new Integer(inherited)).toString());

        if ((type != null) && ((type = type.trim()).length() != 0)) {
            export.setAttribute("type", type);
        }
        export.addContent(new Element(YEAR_TAG, ns).setText(formatInt(year, 4)));
        if (month != defaultValue) {
            export.addContent(new Element(MONTH_TAG, ns).setText(formatInt(month, 2)));
        }
        if (day != defaultValue) {
            export.addContent(new Element(DAY_TAG, ns).setText(formatInt(day, 2)));
        }
        changed = false;
        return (Element)export.clone();
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    public void setFromDOM(org.jdom.Element element) {
        super.setFromDOM(element);

        String value = element.getChildTextTrim(YEAR_TAG, ns);
        setYear(value != null ? Integer.parseInt(value) : defaultValue);
        value = element.getChildTextTrim(MONTH_TAG, ns);
        setMonth(value != null ? Integer.parseInt(value) : defaultValue);
        value = element.getChildTextTrim(DAY_TAG, ns);
        setDay(value != null ? Integer.parseInt(value) : defaultValue);
    }

    /**
     * @return Returns the ns.
     */
    protected static Namespace getNs() {
        return ns;
    }

    /**
     * @param day
     *            The day to set.
     */
    protected void setDay(int day) {
        this.day = day;
        changed = true;
    }

    /**
     * @param month
     *            The month to set.
     */
    protected void setMonth(int month) {
        this.month = month;
        changed = true;
    }

    /**
     * @param year
     *            The year to set.
     */
    protected void setYear(int year) {
        this.year = year;
        changed = true;
    }

    /**
     * 
     * @see org.mycore.datamodel.metadata.MCRMetaDefault#createTypedContent(boolean)
     */
    public MCRTypedContent createTypedContent(boolean parametric) throws MCRException {
        if (!isValid()) {
            debug();
            throw new MCRException("The content of MCRMetaXML is not valid.");
        }

        MCRTypedContent tc = new MCRTypedContent();

        if (!parametric) {
            return tc;
        }

        tc.addTagElement(MCRTypedContent.TYPE_SUBTAG, subtag);

        if ((type = type.trim()).length() != 0) {
            tc.addStringElement(MCRTypedContent.TYPE_ATTRIBUTE, "type", type);
        }

        return tc;
    }

    /**
     * 
     * @see org.mycore.datamodel.metadata.MCRMetaDefault#createTextSearch(boolean)
     */
    public String createTextSearch(boolean textsearch) throws MCRException {
        return "";
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    public void debug() {
        LOGGER.debug("Start Class : MCRMetaDataFragment");
        super.debugDefault();
        LOGGER.debug("Year=" + year);
        LOGGER.debug("Month=" + month);
        LOGGER.debug("Day=" + day);
    }

    protected String formatInt(int value, int size) {
        StringBuffer returns = new StringBuffer(size);
        for (int i = --size; i > 0; i--) {
            if (value < (pow(10, i))) {
                returns.append('0');
            } else {
                break;
            }
        }
        return returns.append(value).toString();
    }

    private int pow(int basis, int exp) {
        int result = 1;
        for (int i = 0; i < exp; i++) {
            result *= basis;
        }
        return result;
    }

    public boolean isValid() {
        if (!super.isValid()) {
            return false;
        }
        if (year == defaultValue) {
            return false;
        }
        if ((year < 0) || ((month != defaultValue) && ((month < 1) || (month > 12))) || ((day != defaultValue) && ((day < 1) || (day > 31)))) {
            return false;
        }
        if ((month == defaultValue) && (day != defaultValue)) {
            return false;
        }
        return true;
    }

    /**
     * This method make a clone of this class.
     */
    public Object clone() {
        MCRMetaDateFragment out = null;

        try {
            out = (MCRMetaDateFragment) super.clone();
        } catch (CloneNotSupportedException e) {
            LOGGER.warn(new StringBuffer(MCRMetaDateFragment.class.getName()).append(" could not be cloned."), e);

            return null;
        }

        out.changed = true;

        return out;
    }

}
