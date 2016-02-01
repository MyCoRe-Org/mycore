package org.mycore.iiif;

import java.awt.image.BufferedImage;

import org.mycore.iiif.model.MCRIIIFImageInformation;
import org.mycore.iiif.model.MCRIIIFImageQuality;
import org.mycore.iiif.model.MCRIIIFImageSourceRegion;
import org.mycore.iiif.model.MCRIIIFImageTargetRotation;
import org.mycore.iiif.model.MCRIIIFImageTargetSize;
import org.mycore.iiif.model.MCRIIIFProfile;

public interface MCRIIIFImageImpl {

    BufferedImage provide(String identifier,
                          MCRIIIFImageSourceRegion region,
                          MCRIIIFImageTargetSize targetSize,
                          MCRIIIFImageTargetRotation rotation,
                          MCRIIIFImageQuality imageQuality,
                          String format) throws ImageNotFoundException, ProvidingException, UnsupportedFormatException;

    MCRIIIFImageInformation getInformation(String identifier) throws ImageNotFoundException, ProvidingException;

    MCRIIIFProfile getProfile();

    class ImageNotFoundException extends Exception {

        private String identifier;

        public ImageNotFoundException(String identifier) {
            super("Invalid image-identifier " + identifier + "!");
            this.identifier = identifier;
        }

        public String getIdentifier() {
            return identifier;
        }
    }

    class ProvidingException extends Exception {
        public ProvidingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }

        public ProvidingException(Throwable cause) {
            super(cause);
        }

        public ProvidingException(String message, Throwable cause) {
            super(message, cause);
        }

        public ProvidingException(String message) {
            super(message);
        }

        public ProvidingException() {
        }
    }

    class UnsupportedFormatException extends Exception {

        public UnsupportedFormatException(String unsupportedFormat) {
            super("The format " + unsupportedFormat + " is not supported!");
        }
    }
}
