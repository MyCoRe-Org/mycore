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

import java.nio.file.Path;
import java.util.Optional;

import org.mycore.imagetiler.MCRImage;
import org.mycore.iview2.services.MCRIView2Tools;

public class MCRDefaultTileFileProvider implements MCRTileFileProvider {

    public Optional<Path> getTileFile(MCRTileInfo tileInfo) {
        return Optional
            .of(MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), tileInfo.getDerivate(), tileInfo.getImagePath()));
    }
}
