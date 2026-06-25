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

package org.mycore.common.uriresolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.jdom2.Element;
import org.jdom2.transform.JDOMResult;
import org.junit.jupiter.api.Test;

class MCRLanguageDetectorURIResolverTest {

    private final MCRLanguageDetectorURIResolver resolver = new MCRLanguageDetectorURIResolver();

    @Test
    void testDetectEnglishFull() throws TransformerException {
        Source result = resolver.resolve("detectLanguage:full:This is an english sentence", null);
        assertNotNull(result);
        assertEquals("en", extractResult(result));
    }

    @Test
    void testDetectArabicByCharacter() throws TransformerException {
        Source result = resolver.resolve("detectLanguage:character:هذا نص عربي", null);
        assertNotNull(result);
        assertEquals("ar", extractResult(result));
    }

    @Test
    void testUnknownLanguageReturnsEmpty() throws TransformerException {
        Source result = resolver.resolve("detectLanguage:full:123 456 789", null);
        assertNotNull(result);
        assertEquals("", extractResult(result));
    }

    @Test
    void testInvalidSyntaxThrowsException() {
        assertThrows(TransformerException.class, () -> resolver.resolve("detectLanguage:textonly", null));
    }

    @Test
    void testInvalidMethodThrowsException() {
        assertThrows(TransformerException.class,
            () -> resolver.resolve("detectLanguage:unknown:This is an english sentence", null));
    }

    @Test
    void testDetectEnglishWithColonInText() throws TransformerException {
        Source result = resolver.resolve("detectLanguage:full:Note: This is an english sentence", null);
        assertNotNull(result);
        assertEquals("en", extractResult(result));
    }

    private String extractResult(Source source) throws TransformerException {
        JDOMResult result = new JDOMResult();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(source, result);
        Element root = result.getDocument().getRootElement();
        assertEquals("string", root.getName());
        return root.getText();
    }

}
