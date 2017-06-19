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

/**
 * Compares and merges mods:relatedItem elements.
 * Two related items are only probably same
 * if they are of type "host", since there can only be one host per publication.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRRelatedItemMerger extends MCRMerger {

    public void setElement(Element element) {
        super.setElement(element);
    }

    @Override
    public boolean isProbablySameAs(MCRMerger other) {
        if (!(other instanceof MCRRelatedItemMerger)) {
            return false;
        } else {
            return isRelatedItemTypeHost() && ((MCRRelatedItemMerger) other).isRelatedItemTypeHost();
        }
    }

    private boolean isRelatedItemTypeHost() {
        return "host".equals(element.getAttributeValue("type"));
    }
}
