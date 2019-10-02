package org.mycore.media.services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

import org.mycore.datamodel.niofs.MCRPath;

public interface MCRThumbnailGenerator {

    boolean matchesFileType(String mimeType, MCRPath path);

    Optional<BufferedImage> getThumbnail(MCRPath path, int size) throws IOException;
}
