package org.mycore.importer.classification;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRImportClassificationMapTest extends MCRTestCase {

    @Test
    public void getEmptyImportValues() {
        MCRImportClassificationMap cMap = new MCRImportClassificationMap("id");
        cMap.put("a", "1");
        cMap.put("b", null);
        cMap.put("c", "2");
        cMap.put("d", "3");
        cMap.put("e", null);

        ArrayList<String> emptyImportValues = cMap.getEmptyImportValues();
        assertEquals(false, emptyImportValues.contains("a"));
        assertEquals(true, emptyImportValues.contains("b"));
        assertEquals(false, emptyImportValues.contains("c"));
        assertEquals(false, emptyImportValues.contains("d"));
        assertEquals(true, emptyImportValues.contains("e"));
    }

    @Test
    public void getMyCoReValue() {
        MCRImportClassificationMap cMap = new MCRImportClassificationMap("id");
        cMap.put("a", "1");
        cMap.put("b", null);
        assertEquals("1", cMap.getMyCoReValue("a"));
        assertEquals("", cMap.getMyCoReValue("b"));
    }

    @Test
    public void isCompletelyFilled() {
        MCRImportClassificationMap cMap = new MCRImportClassificationMap("id");
        cMap.put("a", "1");
        cMap.put("b", "2");
        assertEquals(true, cMap.isCompletelyFilled());
        cMap.put("c", null);
        assertEquals(false, cMap.isCompletelyFilled());
        cMap.put("c", "3");
        assertEquals(true, cMap.isCompletelyFilled());
    }

    @Test
    public void createXml() {
        MCRImportClassificationMap cMap = new MCRImportClassificationMap("id");
        cMap.put("a", "1");
        cMap.put("b", "2");
        Element e = cMap.createXML();
        assertEquals("classificationMapping", e.getName());
        assertEquals("id", e.getAttributeValue("id"));
        assertEquals(2, e.getChildren().size());
    }
}
