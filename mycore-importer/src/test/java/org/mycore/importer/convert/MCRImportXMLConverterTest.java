package org.mycore.importer.convert;

import static org.junit.Assert.assertEquals;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.importer.MCRImportRecord;

public class MCRImportXMLConverterTest extends MCRTestCase {

    private Document xmlFile;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        SAXBuilder saxBuilder = new SAXBuilder();
        this.xmlFile = saxBuilder.build("src/test/resources/sample.xml");
    }

    @Test
    public void convert() {
        MCRImportXMLConverter conv = new MCRImportXMLConverter("test");
        MCRImportRecord record = conv.convert(this.xmlFile);
        // test root attributes
        assertEquals("Test", record.getFieldValue("sample/@attr"));
        assertEquals("http://www.w3.org/1999/xlink", record.getFieldValue("sample/@xmlns:xlink"));
        // test text
        assertEquals("Sample text", record.getFieldValue("sample/text"));
        assertEquals(2, record.getFieldsById("sample/parent/child").size());
        assertEquals("Child text", record.getFieldValue("sample/parent/child"));
        assertEquals("Child text 2", record.getFieldsById("sample/parent/child").get(1).getValue());
        // test attributes
        assertEquals(2, record.getFieldsById("sample/parent/child/@attr").size());
        assertEquals("child attribute", record.getFieldValue("sample/parent/child/@attr"));
        assertEquals("child attribute 2", record.getFieldsById("sample/parent/child/@attr").get(1).getValue());
        assertEquals("child attribute with namespace", record.getFieldValue("sample/parent/child/@xlink:href"));
        // test namespace
        assertEquals("Test namespace", record.getFieldValue("sample/namespace"));
        assertEquals("http://www.w3.org/1999/xhtml", record.getFieldValue("sample/namespace/@xmlns"));
        assertEquals("http://www.w3.org/1999/xhtml", record.getFieldValue("sample/namespace2/@xmlns:myPrefix"));
        assertEquals("Namespace text", record.getFieldValue("sample/namespace2/myPrefix:child"));
    }

}
