/*
 * $Revision: 29635 $ $Date: 2014-04-10 10:55:06 +0200 (Do, 10 Apr 2014) $
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
 * @version $Revision: $ $Date: $
 */
public class MCRRestAPISortObjectComparator implements Comparator<MCRObjectIDDate> {
    private MCRRestAPISortObject _sortObj = null;

    public MCRRestAPISortObjectComparator(MCRRestAPISortObject sortObj) {
        _sortObj = sortObj;
    }

    @Override
    public int compare(MCRObjectIDDate o1, MCRObjectIDDate o2) {
        if ("id".equals(_sortObj.getField().toLowerCase(Locale.GERMAN))) {
            if (_sortObj.getOrder() == SortOrder.ASC) {
                return o1.getId().compareTo(o2.getId());
            }
            if (_sortObj.getOrder() == SortOrder.DESC) {
                return o2.getId().compareTo(o1.getId());
            }
        }
        if ("lastmodified".equals(_sortObj.getField().toLowerCase(Locale.GERMAN))) {
            if (_sortObj.getOrder() == SortOrder.ASC) {
                return o1.getLastModified().compareTo(o2.getLastModified());
            }
            if (_sortObj.getOrder() == SortOrder.DESC) {
                return o2.getLastModified().compareTo(o1.getLastModified());
            }
        }
        return 0;
    }
}
