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

package org.mycore.iview.tests.image.api;

import java.awt.Color;

public class ColorFilter implements PixelFilter {

    public ColorFilter(Color colorToFilter, boolean throwOut, int tolerance) {
        this.colorToFilter = colorToFilter;
        this.throwOut = throwOut;
        this.tolerance = tolerance;
    }

    private final Color colorToFilter;

    private final boolean throwOut;

    private final int tolerance;

    @Override
    public boolean filter(Pixel pixel) {
        Color color = pixel.getColor();
        if (color.equals(colorToFilter)) {
            return true ^ !(throwOut);
        } else {
            int red = color.getRed();
            int blue = color.getBlue();
            int green = color.getGreen();

            int redDiff = Math.abs(red - colorToFilter.getRed());
            int blueDiff = Math.abs(green - colorToFilter.getGreen());
            int greenDiff = Math.abs(blue - colorToFilter.getBlue());

            return (redDiff + blueDiff + greenDiff > tolerance) ^ !(throwOut);
        }
    }
}
