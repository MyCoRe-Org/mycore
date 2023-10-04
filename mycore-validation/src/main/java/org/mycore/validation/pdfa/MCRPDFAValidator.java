package org.mycore.validation.pdfa;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.verapdf.core.EncryptedPdfException;
import org.verapdf.core.ModelParsingException;
import org.verapdf.core.ValidationException;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.results.ValidationResult;

/**
 * Pdf/A Validator.
 *
 * @author Matthias Eichner
 */
public class MCRPDFAValidator {

    private static volatile Boolean INITIALIZED = false;
    private static final Object MUTEX = new Object();

    private static void initialise() {
        if (!INITIALIZED) {
            synchronized (MUTEX) {
                if (INITIALIZED) {
                    return;
                }
                INITIALIZED = true;
                VeraGreenfieldFoundryProvider.initialise();
            }
        }
    }

    /**
     * Checks if the given input stream is a valid pdf/a document.
     *
     * @param inputStream the input stream
     * @return result of the validation
     * @throws MCRPDFAValidationException something went wrong while parsing or validating
     * @throws IOException                i/o exception
     */
    public ValidationResult validate(InputStream inputStream) throws MCRPDFAValidationException, IOException {
        initialise();
        try (PDFAParser parser = Foundries.defaultInstance().createParser(inputStream);
             PDFAValidator validator = Foundries.defaultInstance().createValidator(parser.getFlavour(), false)) {
            return validator.validate(parser);
        } catch (ValidationException | ModelParsingException | EncryptedPdfException e) {
            throw new MCRPDFAValidationException("unable to validate pdf", e);
        }
    }

    /**
     * Checks if the given path is a valid pdf/a document.
     *
     * @param path path to the pdf document
     * @return result of the validation
     * @throws MCRPDFAValidationException something went wrong while parsing or validating
     * @throws IOException                i/o exception
     */
    public ValidationResult validate(Path path) throws MCRPDFAValidationException, IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return validate(inputStream);
        }
    }

}
