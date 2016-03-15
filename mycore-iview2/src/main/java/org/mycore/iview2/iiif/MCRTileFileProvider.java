package org.mycore.iview2.iiif;

import java.nio.file.Path;

import org.mycore.access.MCRAccessException;
import org.mycore.iiif.MCRIIIFImageImpl;

public interface MCRTileFileProvider {
    Path getTiledFile(String identifier) throws MCRIIIFImageImpl.ImageNotFoundException, MCRAccessException;
}
