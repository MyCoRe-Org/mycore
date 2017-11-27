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

package org.mycore.iview2.iiif;

import java.nio.file.Files;
import java.nio.file.Path;

import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.iiif.image.impl.MCRIIIFImageNotFoundException;
import org.mycore.imagetiler.MCRImage;
import org.mycore.iview2.services.MCRIView2Tools;

public class MCRDefaultTileFileProvider implements MCRTileFileProvider {

    public Path getTiledFile(String identifier) throws MCRIIIFImageNotFoundException, MCRAccessException {
        String[] splittedIdentifier = identifier.split("/", 2);

        if (splittedIdentifier.length < 2) {
            throw new MCRIIIFImageNotFoundException(identifier);
        }

        String derivate = splittedIdentifier[0];
        String imagePath = splittedIdentifier[1];

        if (!Files.exists(MCRPath.getPath(derivate, imagePath))) {
            throw new MCRIIIFImageNotFoundException(identifier);
        }

        if (!MCRAccessManager.checkPermission(derivate, MCRAccessManager.PERMISSION_READ)
            && !MCRAccessManager.checkPermission(derivate, "view-derivate")) {
            throw MCRAccessException.missingPermission("View the file " + imagePath + " in " + derivate, derivate,
                "view-derivate");
        }

        return MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), derivate, imagePath);
    }
}
