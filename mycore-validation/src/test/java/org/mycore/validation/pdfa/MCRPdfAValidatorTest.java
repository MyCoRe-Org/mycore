package org.mycore.validation.pdfa;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class MCRPdfAValidatorTest {

    @Test
    public void isCompliant() throws Exception {
        MCRPdfAValidator validator = new MCRPdfAValidator();
        Assert.assertFalse("shouldn't be a valid pdf/a document", isCompliant(validator, "/noPdfA.pdf"));
        Assert.assertTrue("should be a valid pdf/a document", isCompliant(validator, "/pdfA-1b.pdf"));
        Assert.assertTrue("should be a valid pdf/a document", isCompliant(validator, "/pdfA-2b.pdf"));
        Assert.assertTrue("should be a valid pdf/a document", isCompliant(validator, "/pdfA-3b.pdf"));
    }

    private static boolean isCompliant(MCRPdfAValidator validator, String resourceName)
        throws IOException, MCRPdfAValidationException {
        return validator.validate(MCRPdfAValidatorTest.class.getResourceAsStream(resourceName)).isCompliant();
    }

}
