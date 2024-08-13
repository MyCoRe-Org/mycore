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
import org.jdom2.Namespace;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.util.MCRTestCaseClassificationUtil;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mycore.common.util.MCRTestCaseXSLTUtil.prepareTestDocument;
import static org.mycore.common.util.MCRTestCaseXSLTUtil.transform;

public class MCRXSLClassificationTest extends MCRJPATestCase {
    private static final String XSL = "/xslt/functions/classificationTest.xsl";

    @Test
    public void testCategory() throws Exception {
        final Document testDoc = prepareTestDocument("test-category");
        final Map<String, Object> params = Map.of("classid", "TestClassification", "categid", "junit_1");

        Element result = transform(testDoc, XSL, params).getRootElement();

        // existing classification: return category with respective labels
        assertEquals(1, result.getChildren("category").size());
        assertEquals("junit_1", result.getChild("category").getAttributeValue("ID"));
        assertEquals(3, result.getChild("category").getChildren("label").size());
        assertEquals("de",
            result.getChild("category").getChildren("label").get(0).getAttributeValue("lang", Namespace.XML_NAMESPACE));
        assertEquals("junit_1 (de)",
            result.getChild("category").getChildren("label").get(0).getAttributeValue("text"));
        assertEquals("en",
            result.getChild("category").getChildren("label").get(1).getAttributeValue("lang", Namespace.XML_NAMESPACE));
        assertEquals("junit_1 (en)",
            result.getChild("category").getChildren("label").get(1).getAttributeValue("text"));
        assertEquals("x-xxx",
            result.getChild("category").getChildren("label").get(2).getAttributeValue("lang", Namespace.XML_NAMESPACE));
        assertEquals("abc",
            result.getChild("category").getChildren("label").get(2).getAttributeValue("text"));

        // non-existing classification/category: empty result
        result = transform(testDoc, XSL, Map.of("classid", "TestClassification", "categid", "xxx")).getRootElement();
        assertEquals(0, result.getChildren("category").size());
    }

    @Test
    public void testCurrentLabel() throws Exception {
        final Document testDoc = prepareTestDocument("test-current-label");

        // existing category: return label of current lang
        Element result = transform(testDoc, XSL,
            Map.of(
                "classid", "TestClassification",
                "categid", "junit_1")).getRootElement();
        assertEquals(1, result.getChildren("label").size());
        assertEquals("de", result.getChild("label").getAttributeValue("lang", Namespace.XML_NAMESPACE));
        assertEquals("junit_1 (de)", result.getChild("label").getAttributeValue("text"));

        // current Lang does not exist, use default lang
        result = transform(testDoc, XSL,
            Map.of(
                "classid", "TestClassification",
                "categid", "junit_1",
                "CurrentLang", "ar")).getRootElement();
        assertEquals("en", result.getChild("label").getAttributeValue("lang", Namespace.XML_NAMESPACE));
        assertEquals("junit_1 (en)", result.getChild("label").getAttributeValue("text"));

        // current and default lang do not exist, use first label
        result = transform(testDoc, XSL,
            Map.of(
                "classid", "TestClassification",
                "categid", "junit_1",
                "CurrentLang", "ar",
                "DefaultLang", "es")).getRootElement();
        assertEquals("de", result.getChild("label").getAttributeValue("lang", Namespace.XML_NAMESPACE));
        assertEquals("junit_1 (de)", result.getChild("label").getAttributeValue("text"));

        // category does not exist: return empty result
        result = transform(testDoc, XSL,
            Map.of("classid", "TestClassification", "categid", "xxx")).getRootElement();
        assertTrue(result.getChildren("label").isEmpty());
    }

    @Test
    public void testLabel() throws Exception {
        final Document testDoc = prepareTestDocument("test-label");

        // existing label and category
        Element result = transform(testDoc, XSL,
            Map.of(
                "classid", "TestClassification",
                "categid", "junit_1",
                "lang", "x-xxx")).getRootElement();
        assertEquals(1, result.getChildren("label").size());
        assertEquals("x-xxx", result.getChild("label").getAttributeValue("lang", Namespace.XML_NAMESPACE));
        assertEquals("abc", result.getChild("label").getAttributeValue("text"));

        // not-existing label: use current lang label
        result = transform(testDoc, XSL,
            Map.of(
                "classid", "TestClassification",
                "categid", "junit_1",
                "CurrentLang", "en",
                "lang", "es")).getRootElement();
        assertEquals(1, result.getChildren("label").size());
        assertEquals("en", result.getChild("label").getAttributeValue("lang", Namespace.XML_NAMESPACE));
        assertEquals("junit_1 (en)", result.getChild("label").getAttributeValue("text"));

        // not existing label and not existing in current/default lang: use first label
        result = transform(testDoc, XSL,
            Map.of(
                "classid", "TestClassification",
                "categid", "junit_1",
                "CurrentLang", "ar",
                "DefaultLang", "it",
                "lang", "es")).getRootElement();
        assertEquals(1, result.getChildren("label").size());
        assertEquals("de", result.getChild("label").getAttributeValue("lang", Namespace.XML_NAMESPACE));
        assertEquals("junit_1 (de)", result.getChild("label").getAttributeValue("text"));
    }

    @Test
    public void testCurrentLabelText() throws Exception {
        final Document testDoc = prepareTestDocument("test-current-label-text");

        // we have a current lang label (de) and a default lang label (en) entry
        Element result = transform(testDoc, XSL,
            Map.of(
                "classid", "TestClassification",
                "categid", "junit_1")).getRootElement();
        assertEquals("junit_1 (de)", result.getText());

        result = transform(testDoc, XSL,
            Map.of(
                "classid", "TestClassification",
                "categid", "junit_1",
                "CurrentLang", "en")).getRootElement();
        assertEquals("junit_1 (en)", result.getText());

        // we do not have a current lang label (de) entry, but default lang label (en) entry
        result = transform(testDoc, XSL,
            Map.of(
                "classid", "TestClassification",
                "categid", "junit_2")).getRootElement();
        assertEquals("junit_2 (en)", result.getText());

        // we neither have current lang nor default lang label
        result = transform(testDoc, XSL,
            Map.of(
                "classid", "TestClassification",
                "categid", "junit_3")).getRootElement();
        assertEquals("??junit_3@de??", result.getText());
    }

    @Test
    public void testLabelText() throws Exception {
        final Document testDoc = prepareTestDocument("test-label-text");

        // we have an entry for the given language
        Element result = transform(testDoc, XSL,
            Map.of(
                "classid", "TestClassification",
                "categid", "junit_1",
                "lang", "x-xxx")).getRootElement();
        assertEquals("abc", result.getText());

        // not existing language: return current lange entry
        result = transform(testDoc, XSL,
            Map.of(
                "classid", "TestClassification",
                "categid", "junit_1",
                "lang", "ar")).getRootElement();
        assertEquals("junit_1 (de)", result.getText());

        // not existing language, not existing current and default lang: return first label
        result = transform(testDoc, XSL,
            Map.of(
                "classid", "TestClassification",
                "categid", "junit_2",
                "CurrentLang", "ar",
                "DefaultLang", "it",
                "lang", "es")).getRootElement();
        assertEquals("junit_2 (en)", result.getText());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        MCRTestCaseClassificationUtil.addClassification("/classification/TestClassification.xml");
    }
}
