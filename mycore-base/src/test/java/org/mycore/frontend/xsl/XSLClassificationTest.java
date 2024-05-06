package org.mycore.frontend.xsl;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.util.MCRTestCaseClassificationUtil;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mycore.common.util.MCRTestCaseXSLTUtil.transform;
import static org.mycore.common.util.MCRTestCaseXSLTUtil.xmlTest;

public class XSLClassificationTest extends MCRJPATestCase {
    private static final String XSL = "/xslt/functions/classificationTest.xsl";

    @Test
    public void testCurrentLabelText() throws Exception {
        MCRTestCaseClassificationUtil.addClassification("/classification/TestClassification.xml");
        final Document doc = new Document(new Element("empty"));
        final Element empty = doc.getRootElement();

        // we have a current lang label (de) and a default lang label (en) entry
        Element result = transform(xmlTest("test-current-label-text", empty), XSL,
            Map.of("classid", "TestClassification", "categid", "junit_de")).getRootElement();
        assertEquals("junit_de (de)", result.getText());

        result = transform(xmlTest("test-current-label-text", empty), XSL,
            Map.of("classid", "TestClassification", "categid", "junit_de", "CurrentLang", "en")).getRootElement();
        assertEquals("junit_de (en)", result.getText());

        // we do not have a current lang label (de) entry, but default lang label (en) entry
        result = transform(xmlTest("test-current-label-text", empty), XSL,
            Map.of("classid", "TestClassification", "categid", "junit_en")).getRootElement();
        assertEquals("junit_en (en)", result.getText());

        // we neither have current lang nor default lang label
        result = transform(xmlTest("test-current-label-text", empty), XSL,
            Map.of("classid", "TestClassification", "categid", "junit_none")).getRootElement();
        assertEquals("??junit_none@de??", result.getText());
    }
}
