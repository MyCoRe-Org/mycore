package org.mycore.services.imaging.JAI;

import java.io.InputStream;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import com.sun.media.jai.codec.MemoryCacheSeekableStream;

public class MCRJAIImgMemReader implements MCRJAIImageReader {
    public PlanarImage readImage(InputStream input) {
        MemoryCacheSeekableStream stream = new MemoryCacheSeekableStream(input);
        return JAI.create("stream", stream);
    }
}
