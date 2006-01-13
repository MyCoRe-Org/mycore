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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;

import org.mycore.common.ISO8601DateFormat;
import org.mycore.common.MCRException;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRMetaISO8601Date extends MCRMetaDefault implements MCRMetaInterface {
    
    protected final static int defaultValue = 0;

    private Element export;

    private boolean changed = true;

    private static final Namespace ns = Namespace.NO_NAMESPACE;
    
    private Date dt;
    private final static DateFormat df=new ISO8601DateFormat();
    private boolean valid=false;
    
    private static final Logger LOGGER=Logger.getLogger(MCRMetaISO8601Date.class);

    /**
     *  
     */
    public MCRMetaISO8601Date() {
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
        export.setAttribute("inherited", (new Integer(inherited)).toString());

        if ((type != null) && ((type = type.trim()).length() != 0)) {
            export.setAttribute("type", type);
        }
        export.setText(df.format(dt));
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
        setDate(element.getTextTrim());
    }

    /**
     * @return Returns the ns.
     */
    protected static Namespace getNs() {
        return ns;
    }

    /**
     * @param String
     *            Date in form YYYY-MM-DDThh:mm:ssTZD
     */
    final void setDate(String isoString) {
        Date dt=null;
        try {
            dt=df.parse(isoString);
        } catch (ParseException e) {
            //no handling of exceptions at this point valid should be set to false;
            LOGGER.warn("Error while parsing String to Date: "+isoString, e);
            System.err.println("Error while parsing String to Date: "+isoString);
            System.err.println(MCRException.getStackTraceAsString(e));
            valid=false;
        }
        setDate(dt);
    }
    
    final Date getDate(){
        return new Date(dt.getTime());
    }

    /**
     * @param dt
     *            Date object representing date String in Element
     */
    private void setDate(Date dt) {
        this.dt=dt;
        if (dt==null){
            valid=false;
        } else {
            valid=true;
        }
        changed = true;
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
        LOGGER.debug("Start Class : MCRMetaTimestamp");
        super.debugDefault();
        LOGGER.debug("Date=" + df.format(dt));
    }

    /**
     * This method make a clone of this class.
     */
    public Object clone() {
        MCRMetaISO8601Date out = null;

        try {
            out = (MCRMetaISO8601Date) super.clone();
        } catch (CloneNotSupportedException e) {
            LOGGER.warn(new StringBuffer(MCRMetaISO8601Date.class.getName()).append(" could not be cloned."), e);

            return null;
        }

        out.changed = true;

        return out;
    }

    public boolean isValid() {
        if (!valid || !super.isValid()){
            return false;
        }
        return true;
    }
}