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

package org.mycore.iiif.image.model;

import java.util.Locale;

public class MCRIIIFImageTargetSize {

    private final int width;

    private final int height;

    public MCRIIIFImageTargetSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MCRIIIFImageTargetSize
            && ((MCRIIIFImageTargetSize) obj).width == this.width
            && ((MCRIIIFImageTargetSize) obj).height == this.height;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "[%d,%d]", width, height);
    }
}
