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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MCRPDFAFunctionsTest {

    @Test
    public void singleFileResultUsesTheSameMarkupAsTheDirectoryResult() throws Exception {
        Element file = validate("/pdfA-1b.pdf", "sub/pdfA-1b.pdf");
        assertEquals("file", file.getNodeName());
        assertEquals("sub/pdfA-1b.pdf", file.getAttribute("name"));
        assertEquals("1b", file.getAttribute("flavour"));
        assertEquals(0, file.getElementsByTagName("failed").getLength());
    }

    @Test
    public void singleFileResultReportsFailedRules() throws Exception {
        Element file = validate("/noPdfA.pdf", "noPdfA.pdf");
        assertTrue("expected failed rules", file.getElementsByTagName("failed").getLength() > 0);
        Element failed = (Element) file.getElementsByTagName("failed").item(0);
        assertNotNull(failed.getAttribute("clause"));
        assertNotNull(failed.getAttribute("testNumber"));
    }

    @Test
    public void unreadableFilesAreNotReportedAsValidationErrors() {
        // an I/O failure may be transient, so it must reach the caller instead of being persisted as a result
        Path missing = Path.of("does-not-exist.pdf");
        assertThrows(IOException.class, () -> MCRPDFAFunctions.getResult(missing, "does-not-exist.pdf"));
    }

    @Test
    public void validatorReportsItsVersion() {
        assertTrue("expected a veraPDF version", MCRPDFAValidator.getVersion().matches("\\d+\\.\\d+.*"));
    }

    private static Element validate(String resource, String name) throws Exception {
        Document document = MCRPDFAFunctions.getResult(resource(resource), name);
        return document.getDocumentElement();
    }

    private static Path resource(String resource) throws URISyntaxException {
        return Path.of(MCRPDFAFunctionsTest.class.getResource(resource).toURI());
    }
}
