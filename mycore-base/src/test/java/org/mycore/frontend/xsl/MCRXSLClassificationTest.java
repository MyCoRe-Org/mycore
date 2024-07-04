/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.frontend.xsl;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.util.MCRTestCaseClassificationUtil;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mycore.common.util.MCRTestCaseXSLTUtil.prepareTestDocument;
import static org.mycore.common.util.MCRTestCaseXSLTUtil.transform;

public class MCRXSLClassificationTest extends MCRJPATestCase {
    private static final String XSL = "/xslt/functions/classificationTest.xsl";

    @Test
    public void testCurrentLabelText() throws Exception {
        MCRTestCaseClassificationUtil.addClassification("/classification/TestClassification.xml");
        final Document testDoc = prepareTestDocument("test-current-label-text");

        // we have a current lang label (de) and a default lang label (en) entry
        Element result = transform(testDoc, XSL,
            Map.of("classid", "TestClassification", "categid", "junit_1"))
            .getRootElement();
        assertEquals("junit_1 (de)", result.getText());

        result = transform(testDoc, XSL,
            Map.of("classid", "TestClassification", "categid", "junit_1", "CurrentLang", "en"))
            .getRootElement();
        assertEquals("junit_1 (en)", result.getText());

        // we do not have a current lang label (de) entry, but default lang label (en) entry
        result = transform(testDoc, XSL,
            Map.of("classid", "TestClassification", "categid", "junit_2"))
            .getRootElement();
        assertEquals("junit_2 (en)", result.getText());

        // we neither have current lang nor default lang label
        result = transform(testDoc, XSL,
            Map.of("classid", "TestClassification", "categid", "junit_3"))
            .getRootElement();
        assertEquals("??junit_3@de??", result.getText());
    }
}
