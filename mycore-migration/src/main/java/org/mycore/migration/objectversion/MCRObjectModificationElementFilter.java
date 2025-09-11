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

package org.mycore.migration.objectversion;

import java.io.Serial;

import org.jdom2.Element;
import org.jdom2.filter.AbstractFilter;
import org.mycore.datamodel.metadata.MCRObjectService;

/**
 * A filter that filters for modification related elements, i.e. <servflag type="modifiedBy"> and
 * <servdate type="modifyDate">.
 *
 * @author Thomas Scheffler (yagee)
 */
class MCRObjectModificationElementFilter extends AbstractFilter<Element> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static boolean isModifiedBy(Element element) {
        return element.getName().equals(MCRObjectService.ELEMENT_SERVFLAG) &&
            MCRObjectService.FLAG_TYPE_MODIFIEDBY.equals(element.getAttributeValue("type"));
    }

    private static boolean isModifiedServDate(Element element) {
        return element.getName().equals(MCRObjectService.ELEMENT_SERVDATE) &&
            MCRObjectService.DATE_TYPE_MODIFYDATE.equals(element.getAttributeValue("type"));
    }

    @Override
    public Element filter(Object content) {
        if (content instanceof Element element && (isModifiedServDate(element) || isModifiedBy(element))) {
            return element;
        }
        return null;
    }
}
