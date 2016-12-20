/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  This program is free software; you can use it, redistribute it
 *  and / or modify it under the terms of the GNU General Public License
 *  (GPL) as published by the Free Software Foundation; either version 2
 *  of the License or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program, in a file called gpl.txt or license.txt.
 *  If not, write to the Free Software Foundation Inc.,
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */

package org.mycore.iiif.image.model;

public class MCRIIIFImageTargetRotation {

    private final boolean mirrored;

    private final double degrees;

    public MCRIIIFImageTargetRotation(boolean mirrored, double degrees) {
        this.mirrored = mirrored;
        this.degrees = degrees;
    }

    public boolean isMirrored() {
        return mirrored;
    }

    public double getDegrees() {
        return degrees;
    }

    @Override
    public String toString() {
        return "[" + mirrored + "," + degrees + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MCRIIIFImageTargetRotation
            && ((MCRIIIFImageTargetRotation) obj).degrees == degrees
            && ((MCRIIIFImageTargetRotation) obj).mirrored == mirrored;
    }
}
