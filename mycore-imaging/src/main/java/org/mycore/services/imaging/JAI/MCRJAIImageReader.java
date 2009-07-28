package org.mycore.services.imaging.JAI;

import java.io.InputStream;

import javax.media.jai.PlanarImage;

public interface MCRJAIImageReader {
    public PlanarImage readImage(InputStream input);
}
