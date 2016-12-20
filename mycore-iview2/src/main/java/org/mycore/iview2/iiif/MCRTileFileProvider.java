package org.mycore.iview2.iiif;

import java.nio.file.Path;

import org.mycore.access.MCRAccessException;
import org.mycore.iiif.image.impl.MCRIIIFImageNotFoundException;

public interface MCRTileFileProvider {
    Path getTiledFile(String identifier) throws MCRIIIFImageNotFoundException, MCRAccessException;
}
