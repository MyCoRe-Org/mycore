package org.mycore.mcr.neo4j.utils;

import org.junit.Test;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.common.util.MCRTestCaseClassificationUtil;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jtojson.Neo4JMetaData;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MCRNeo4JQueryRunnerTest extends MCRStoreTestCase {
    @Test
    public void testTranslateAndMapProperties() {
        final List<Neo4JMetaData> metaDataList = new ArrayList<>();

        MCRNeo4JQueryRunner.translateAndMapProperties(metaDataList, "key", List.of("value1", "value2"), "de");
        assertFalse(metaDataList.isEmpty());
        assertEquals(1, metaDataList.size());
        assertEquals("key", metaDataList.get(0).title());
        assertEquals("value1", metaDataList.get(0).content().get(0));
        assertEquals("value2", metaDataList.get(0).content().get(1));
        assertEquals(2, metaDataList.get(0).content().size());

        metaDataList.clear();
        MCRNeo4JQueryRunner.translateAndMapProperties(metaDataList, "key", List.of("value1"), "de");
        assertFalse(metaDataList.isEmpty());
        assertEquals(1, metaDataList.size());
        assertEquals("key", metaDataList.get(0).title());
        assertEquals("value1", metaDataList.get(0).content().get(0));
        assertEquals(1, metaDataList.get(0).content().size());

        metaDataList.clear();
        MCRNeo4JQueryRunner.translateAndMapProperties(metaDataList, "key", List.of("TestClassification_-_junit_1"),
            "de");
        assertFalse(metaDataList.isEmpty());
        assertEquals(1, metaDataList.size());
        assertEquals("key", metaDataList.get(0).title());
        assertEquals("junit_1 (de)", metaDataList.get(0).content().get(0));
        assertEquals(1, metaDataList.get(0).content().size());

        metaDataList.clear();
        MCRNeo4JQueryRunner.translateAndMapProperties(metaDataList, "key",
            List.of("TestClassification_-_junit_1", "TestClassification_-_junit_2"),
            "de");
        assertFalse(metaDataList.isEmpty());
        assertEquals(1, metaDataList.size());
        assertEquals("key", metaDataList.get(0).title());
        assertEquals("junit_1 (de)", metaDataList.get(0).content().get(0));
        // second category has no de label
        assertEquals("", metaDataList.get(0).content().get(1));
        assertEquals(2, metaDataList.get(0).content().size());

        // check for English labels
        metaDataList.clear();
        MCRNeo4JQueryRunner.translateAndMapProperties(metaDataList, "key",
            List.of("TestClassification_-_junit_1", "TestClassification_-_junit_2"),
            "en");
        assertFalse(metaDataList.isEmpty());
        assertEquals(1, metaDataList.size());
        assertEquals("key", metaDataList.get(0).title());
        assertEquals("junit_1 (en)", metaDataList.get(0).content().get(0));
        assertEquals("junit_2 (en)", metaDataList.get(0).content().get(1));
        assertEquals(2, metaDataList.get(0).content().size());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        MCRTestCaseClassificationUtil.addClassification("/classification/TestClassification.xml");
    }
}
