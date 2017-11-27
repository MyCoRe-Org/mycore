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

package org.mycore.mods.merger;

import org.jdom2.Element;

/**
 * Provides a convenience method to merge one MODS element into another.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRMergeTool {

    /**
     * Merges a given MODS element with another.
     *
     * @param modsToMergeInto the MODS element that will be modified by merging data into it
     * @param modsToMergeFrom the MODS element that contains the data to merge from
     */
    public static void merge(Element modsToMergeInto, Element modsToMergeFrom) {
        MCRMerger mBaseToMergeInto = MCRMergerFactory.buildFrom(modsToMergeInto);
        MCRMerger mToMergeFrom = MCRMergerFactory.buildFrom(modsToMergeFrom);
        mBaseToMergeInto.mergeFrom(mToMergeFrom);
    }
}
