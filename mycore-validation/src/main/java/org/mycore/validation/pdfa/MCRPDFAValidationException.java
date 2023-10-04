package org.mycore.validation.pdfa;
import org.mycore.common.MCRCatchException;

/**
 *
 *
 * @author Matthias Eichner
 */
public class MCRPDFAValidationException extends MCRCatchException {
    public MCRPDFAValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
