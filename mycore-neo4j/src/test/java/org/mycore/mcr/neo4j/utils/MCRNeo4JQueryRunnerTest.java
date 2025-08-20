package org.mycore.mcr.neo4j.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.util.MCRTestCaseClassificationUtil;
import org.mycore.mcr.neo4j.datamodel.metadata.neo4jtojson.Neo4JMetaData;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
public class MCRNeo4JQueryRunnerTest {
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


    @BeforeEach
    public void setUp() throws Exception {
        MCRTestCaseClassificationUtil.addClassification("/classification/TestClassification.xml");
    }
}
