package org.mycore.frontend.xsl;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class XSLClassificationTest extends MCRJPATestCase {
    private static final String XSL = "/xslt/functions/classificationTest.xsl";

    @Test
    public void testCurrentLabelText() throws Exception {
        addClassification("/classification/TestClassification.xml");
        final Document doc = new Document(new Element("empty"));
        final Element empty = doc.getRootElement();

        // we have a current lang label (de) and a default lang label (en) entry
        Element result = parse(Map.of("classid", "TestClassification", "categid", "junit_de"), XSL,
            xmlTest("test-current-label-text", empty)).getRootElement();
        assertEquals("junit_de (de)", result.getText());

        result = parse(Map.of("classid", "TestClassification", "categid", "junit_de", "CurrentLang", "en"), XSL,
            xmlTest("test-current-label-text", empty)).getRootElement();
        assertEquals("junit_de (en)", result.getText());

        // we do not have a current lang label (de) entry, but default lang label (en) entry
        result = parse(Map.of("classid", "TestClassification", "categid", "junit_en"), XSL,
            xmlTest("test-current-label-text", empty)).getRootElement();
        assertEquals("junit_en (en)", result.getText());

        // we neither have current lang nor default lang label
        result = parse(Map.of("classid", "TestClassification", "categid", "junit_none"), XSL,
            xmlTest("test-current-label-text", empty)).getRootElement();
        assertEquals("??junit_none@de??", result.getText());
    }
}
