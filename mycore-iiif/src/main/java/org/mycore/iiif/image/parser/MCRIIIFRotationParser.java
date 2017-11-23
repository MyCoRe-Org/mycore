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

package org.mycore.iiif.image.parser;

import org.mycore.iiif.image.model.MCRIIIFImageTargetRotation;

public class MCRIIIFRotationParser {

    private final String rotation;

    public MCRIIIFRotationParser(String rotation) {
        this.rotation = rotation;
    }

    public MCRIIIFImageTargetRotation parse() {
        boolean mirror = this.rotation.startsWith("!");

        String rotationNumberString = mirror ? this.rotation.substring(1) : this.rotation;
        Double rotationNumber;
        try {
            rotationNumber = Double.parseDouble(rotationNumberString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(rotationNumberString + " cannot parsed to a rotation value!");
        }

        if (rotationNumber < 0 || rotationNumber > 360) {
            throw new IllegalArgumentException(rotationNumber + " is not a valid rotation value!");
        }

        return new MCRIIIFImageTargetRotation(mirror, rotationNumber);
    }
}
