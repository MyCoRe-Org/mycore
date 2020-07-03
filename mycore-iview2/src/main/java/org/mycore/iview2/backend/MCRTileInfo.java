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

package org.mycore.iview2.backend;

/**
 * Holds all attributes for a specific tile.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRTileInfo {

    private String derivate;

    private String imagePath;

    private String tile;

    public MCRTileInfo(final String derivate, final String imagePath, final String tile) {
        this.derivate = derivate;
        this.imagePath = imagePath;
        this.tile = tile;
    }

    /**
     * returns "TileInfo [derivate=" + derivate + ", imagePath=" + imagePath + ", tile=" + tile + "]"
     */
    @Override
    public String toString() {
        return "TileInfo [derivate=" + derivate + ", imagePath=" + imagePath + ", tile=" + tile + "]";
    }

    public String getDerivate() {
        return derivate;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getTile() {
        return tile;
    }
}
