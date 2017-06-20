package org.mycore.common.events.test;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.events.MCREventManager;

public class MCREventManagerTest extends MCRTestCase {
    static String defaultProperties;

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Store.BaseDir", "tmp");
        testProperties.put("MCR.Metadata.Store.SVNBase", "/tmp/versions");
        testProperties.put("MCR.EventHandler.MCRObject.1.Class",
            "org.mycore.datamodel.common.MCRXMLMetadataEventHandler");
        testProperties.put("MCR.EventHandler.MCRObject.4.Indexer", "lucene-metadata");
        testProperties.put("MCR.EventHandler.MCRObject.4.Foo", "fooProp");
        testProperties.put("MCR.EventHandler.MCRDerivate.2.Class",
            "org.mycore.datamodel.common.MCRXMLMetadataEventHandler");
        testProperties.put("MCR.Searcher.lucene-metadata.Class",
            "org.mycore.common.events.test.MCREventManagerTest$FakeLuceneSearcher");
        testProperties.put("MCR.Searcher.lucene-metadata.Index", "metadata");
        testProperties.put("MCR.Searcher.lucene-metadata.IndexDir", "%MCR.datadir%/lucene-index4metadata");
        testProperties.put("MCR.Searcher.lucene-metadata.StoreQueryFields", "true");
        return testProperties;
    }

    @Test
    public void instance() throws Exception {
        try {
            MCREventManager.instance();
        } catch (MCRConfigurationException e) {
            assertEquals("Configuration property MCR.EventHandler.Mode.Foo is not set", e.getMessage());
        }
    }
}
