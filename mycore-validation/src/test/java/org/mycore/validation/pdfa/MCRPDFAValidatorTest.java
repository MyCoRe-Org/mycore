/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.validation.pdfa;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

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
