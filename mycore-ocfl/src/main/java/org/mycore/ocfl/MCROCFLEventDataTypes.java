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

package org.mycore.ocfl;

/**
 * List of all Data Types returned by {@link MCROCFLEventHandler#getEventData}
 * @author Tobias Lenhardt [Hammer1279]
 */
// This is a definition class, not a Util, Helper, Manager or Command Class...
// How even is the "MCROCFLMetadataVersion" Valid, but this isn't?
@SuppressWarnings("PMD")
public class MCROCFLEventDataTypes {
    
    /**
     * The MCRCategory Object of the Category/Classification
     */
    public static final String CATEGORY = "ctg";

    /**
     * The MCRContent Document of the Root Classification of the Category, or the Classification itself.
     * It can be used to always have the Root Element.
     */
    public static final String CATEGORY_ROOT_CONTENT = "rtx";

    /**
     * The MCRContent Element of the Category/Classification
     */
    public static final String CATEGORY_CONTENT = "cgx";

    /**
     * The MyCoRe ID for this Category/Classification
     */
    public static final String CATEGORY_ID = "mid";
}
