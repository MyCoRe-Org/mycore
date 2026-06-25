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

package org.mycore.common.xsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mycore.common.util.MCRTestCaseXSLTUtil;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRLanguageFunctionsTests {

    @Test
    @DisplayName("mcrlanguage:detect-language")
    public void testDetectLanguage() throws Exception {
        assertEquals("en", detectLanguage("This is an english sentence"));
    }

    @Test
    @DisplayName("mcrlanguage:detect-language-by-character")
    public void testDetectLanguageByCharacter() throws Exception {
        assertEquals("ar", detectLanguageByCharacter("هذا نص عربي"));
    }

    @Test
    @DisplayName("mcrlanguage:detect-language returns empty on unknown input")
    public void testDetectLanguageUnknown() throws Exception {
        assertEquals("", detectLanguage("123 456 789"));
    }

    @Test
    @DisplayName("mcrlanguage:detect-language-by-character returns empty for latin input")
    public void testDetectLanguageByCharacterUnknown() throws Exception {
        assertEquals("", detectLanguageByCharacter("This is english"));
    }

    private String detectLanguage(String text) throws TransformerException {
        return callFunction("detect-language", text);
    }

    private String detectLanguageByCharacter(String text) throws TransformerException {
        return callFunction("detect-language-by-character", text);
    }

    private String callFunction(String name, String text) throws TransformerException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("text", text);
        parameters.put("fn-name", name);
        return MCRTestCaseXSLTUtil.transform("/xslt/functions/language-test.xsl", parameters)
            .getRootElement().getText();
    }

}
