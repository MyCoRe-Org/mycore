package org.mycore.webtools.upload.exception;

import org.mycore.services.i18n.MCRTranslation;

public class MCRUploadException extends Exception {
    public MCRUploadException(String messageKey) {
        super(MCRTranslation.translate(messageKey));
    }

    public MCRUploadException(String messageKey, String ...translationParams){
        super(MCRTranslation.translate(messageKey, (Object[]) translationParams));
    }

    public MCRUploadException(String messageKey, Throwable throwable) {
        super(MCRTranslation.translate(messageKey), throwable);
    }
}
