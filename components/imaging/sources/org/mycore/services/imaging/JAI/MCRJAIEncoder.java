package org.mycore.services.imaging.JAI;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

public interface MCRJAIEncoder {
    public void encode(RenderedImage image, OutputStream out) throws IOException;
}
