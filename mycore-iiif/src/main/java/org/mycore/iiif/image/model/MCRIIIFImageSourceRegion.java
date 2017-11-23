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

public class MCRIIIFImageSourceRegion {

    private final int x1, y1, x2, y2;

    public MCRIIIFImageSourceRegion(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof MCRIIIFImageSourceRegion)
            && ((MCRIIIFImageSourceRegion) obj).getX2() == getX2()
            && ((MCRIIIFImageSourceRegion) obj).getY2() == getY2()
            && ((MCRIIIFImageSourceRegion) obj).getX1() == getX1()
            && ((MCRIIIFImageSourceRegion) obj).getY1() == getY1();
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "[%d,%d,%d,%d]", x1, y1, x2, y2);
    }
}
