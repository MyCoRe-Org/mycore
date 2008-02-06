/*
 * 
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

package org.mycore.services.fieldquery;

/**
 * Represents a single sort criteria for sorting query results.
 * Each MCRSortBy defines one field to sort by, and the order
 * (ascending or descending).
 *  
 * @author Frank Lützenkirchen
 */
public class MCRSortBy {
    /** Sort this field in ascending order */
    public final static boolean ASCENDING = true;

    /** Sort this field in descending order */
    public final static boolean DESCENDING = false;

    /** The field to sort by */ 
    private MCRFieldDef field;

    /** Sort order of this field */
    private boolean order = ASCENDING;

    /** 
     * Creates a new sort criteria
     * 
     * @param field the field to sort by
     * @param order the sort order (ascending or descending)
     * 
     * @see #ASCENDING
     * @see #DESCENDING
     */
    public MCRSortBy(MCRFieldDef field, boolean order) {
        this.field = field;
        this.order = order;
    }

    /** 
     * Returns the field to sort by 
     */
    public MCRFieldDef getField() {
        return field;
    }

    /**
     * Returns the sort order for this field.
     * 
     * @see #ASCENDING
     * @see #DESCENDING
     */
    public boolean getSortOrder() {
        return order;
    }
}
