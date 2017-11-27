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

package org.mycore.iiif.presentation.model.attributes;

import com.google.gson.annotations.SerializedName;

/**
 * The direction that canvases of the resource should be presented when rendered for the user to navigate and/or read.
 * @see <a href="http://iiif.io/api/presentation/2.0/#technical-properties">IIIF-Documentation</a>
 */
public enum MCRIIIFViewingDirection {
    /**
     * The object is read from left to right, and is the default if not specified.
     */
    @SerializedName("left-to-right")
    left_to_right(),

    /**
     * The object is read from right to left.
     */
    @SerializedName("right-to-left")
    right_to_left(),

    /**
     * The object is read from the top to the bottom.
     */
    @SerializedName("top-to-bottom")
    top_to_bottom(),

    /**
     * The object is read from the bottom to the top.
     */
    @SerializedName("bottom-to-top")
    bottom_to_top()

}
