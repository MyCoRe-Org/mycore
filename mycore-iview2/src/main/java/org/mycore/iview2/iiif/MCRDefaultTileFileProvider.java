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
