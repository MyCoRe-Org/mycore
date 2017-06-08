/*
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

package org.mycore.mods.merger;

import org.jdom2.Element;
import org.mycore.common.MCRConstants;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRExtentMerger extends MCRMerger {

    public void setElement(Element element) {
        super.setElement(element);
    }

    @Override
    public boolean isProbablySameAs(MCRMerger other) {
        return (other instanceof MCRExtentMerger);
    }

    private boolean hasStartPage() {
        return element.getChild("start", MCRConstants.MODS_NAMESPACE) != null;
    }

    @Override
    public void mergeFrom(MCRMerger other) {
        if (element.getParentElement().getName().equals("physicalDescription"))
            super.mergeFrom(other);
        else { // parent is "mods:part"
            if ((!this.hasStartPage()) && ((MCRExtentMerger) other).hasStartPage()) {
                mergeAttributes(other);
                this.element.setContent(other.element.cloneContent());
            }
        }
    }
}
