/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.restapi.v1.utils;

import java.util.Comparator;
import java.util.Locale;

import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.restapi.v1.utils.MCRRestAPISortObject.SortOrder;

/**
 * Comparator to sort a collection of result objects by id or date
 * 
 * ToDo: Check if this can be replaced with SOLR functionality
 * 
 * @author Robert Stephan
 * 
 */
public class MCRRestAPISortObjectComparator implements Comparator<MCRObjectIDDate> {
    private MCRRestAPISortObject sortObj = null;

    public MCRRestAPISortObjectComparator(MCRRestAPISortObject sortObj) {
        this.sortObj = sortObj;
    }

    @Override
    public int compare(MCRObjectIDDate o1, MCRObjectIDDate o2) {
        if ("id".equals(sortObj.getField().toLowerCase(Locale.ROOT))) {
            if (sortObj.getOrder() == SortOrder.ASC) {
                return o1.getId().compareTo(o2.getId());
            }
            if (sortObj.getOrder() == SortOrder.DESC) {
                return o2.getId().compareTo(o1.getId());
            }
        }
        if ("lastmodified".equals(sortObj.getField().toLowerCase(Locale.ROOT))) {
            if (sortObj.getOrder() == SortOrder.ASC) {
                return o1.getLastModified().compareTo(o2.getLastModified());
            }
            if (sortObj.getOrder() == SortOrder.DESC) {
                return o2.getLastModified().compareTo(o1.getLastModified());
            }
        }
        return 0;
    }
}
