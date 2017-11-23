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
    public static final boolean ASCENDING = true;

    /** Sort this field in descending order */
    public static final boolean DESCENDING = false;

    /** The field to sort by */
    private String fieldName;

    /** Sort order of this field */
    private boolean order = ASCENDING;

    /** 
     * Creates a new sort criteria
     * 
     * @param fieldName the field to sort by
     * @param order the sort order (ascending or descending)
     * 
     * @see #ASCENDING
     * @see #DESCENDING
     */
    public MCRSortBy(String fieldName, boolean order) {
        this.fieldName = fieldName;
        this.order = order;
    }

    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the sort order for this field.
     * 
     * @see #ASCENDING
     * @see #DESCENDING
     * 
     * @return true when order is {@link MCRSortBy#ASCENDING} or false whenorder is {@link MCRSortBy#DESCENDING} 
     */
    public boolean getSortOrder() {
        return order;
    }
}
