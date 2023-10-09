package org.mycore.validation.pdfa;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class MCRPDFAValidatorTest {

    @Test
    public void isCompliant() throws Exception {
        MCRPDFAValidator validator = new MCRPDFAValidator();
        Assert.assertFalse("shouldn't be a valid pdf/a document", isCompliant(validator, "/noPdfA.pdf"));
        Assert.assertTrue("should be a valid pdf/a document", isCompliant(validator, "/pdfA-1b.pdf"));
        Assert.assertTrue("should be a valid pdf/a document", isCompliant(validator, "/pdfA-2b.pdf"));
        Assert.assertTrue("should be a valid pdf/a document", isCompliant(validator, "/pdfA-3b.pdf"));
    }

    private static boolean isCompliant(MCRPDFAValidator validator, String resourceName)
        throws IOException, MCRPDFAValidationException {
        return validator.validate(MCRPDFAValidatorTest.class.getResourceAsStream(resourceName)).isCompliant();
    }

}
