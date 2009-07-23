package org.mycore.services.imaging.JAI;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;

public class MCRJAIJPEGEnc implements MCRJAIEncoder {
    protected JPEGEncodeParam param;
    
    public MCRJAIJPEGEnc() {
        this.param = new JPEGEncodeParam();
    }
    
    public MCRJAIJPEGEnc(JPEGEncodeParam param) {
        this.param = param;
    }

    public void encode(RenderedImage image, OutputStream out) throws IOException {
        ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG", out, param);
        encoder.encode(image);
        out.close();
    }
}
