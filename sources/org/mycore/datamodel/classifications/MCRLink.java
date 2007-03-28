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

package org.mycore.datamodel.classifications;

import java.io.Serializable;

/**
 * This class implements all link data based on X3C XLink definition.
 * 
 * @author Thomas Scheffler (yagee)
 * @author jens Kupferschmidt
 * 
 * @version $Revision$ $Date$
 */
public class MCRLink implements Cloneable, Serializable {

    private static final long serialVersionUID = 4724359667520764818L;

    String type, href, title, label;

    /**
     * Construcor
     * 
     * @param type
     *            type of link, default is 'locator'
     * @param href
     *            reference of link
     * @param title
     *            title of link
     * @param label
     *            label if link
     */
    public MCRLink(String type, String href, String title, String label) {
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            this.type = "locator";
        } else {
            this.type = type;
        }
        this.href = href;
        this.title = title;
        this.label = label;
    }

    /**
     * The method get the link reference as String.
     * 
     * @return the link reference as String
     */
    public String getHref() {
        return href;
    }

    /**
     * The method set the link href from String.
     * 
     * @return the link href from String
     */
    public void setHref(String href) {
        this.href = href;
    }

    /**
     * The method get the link label as String.
     * 
     * @return the link label as String
     */
    public String getLabel() {
        return label;
    }

    /**
     * The method set the link label from String.
     * 
     * @return the link label from String
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * The method get the link title as String.
     * 
     * @return the link title as String
     */
    public String getTitle() {
        return title;
    }

    /**
     * The method set the link title from String.
     * 
     * @return the link title from String
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * The method get the link type as String.
     * 
     * @return the link type as String
     */
    public String getType() {
        return type;
    }

    /**
     * The method set the link type from String.
     * 
     * @return the link type from String
     */
    public void setType(String type) {
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            this.type = "locator";
        } else {
            this.type = type;
        }
    }

    @Override
    public MCRLink clone() {
        MCRLink clone = null;
        try {
            clone = (MCRLink) super.clone();
        } catch (CloneNotSupportedException ce) {
            // Can not happen
        }
        return clone;
    }

}
