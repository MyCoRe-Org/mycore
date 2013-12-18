package org.mycore.common.events.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.events.MCREventManager;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRSortBy;

public class MCREventManagerTest extends MCRTestCase {
    static String defaultProperties;

    @BeforeClass
    public static void initTestProperties() {
        defaultProperties = System.getProperty(MCR_CONFIGURATION_FILE);
        System
            .setProperty(MCR_CONFIGURATION_FILE, "props/" + MCREventManagerTest.class.getSimpleName() + ".properties");
    }

    @AfterClass
    public static void resetTestProperties() {
        if (defaultProperties == null) {
            System.getProperties().remove(MCR_CONFIGURATION_FILE);
        } else {
            System.setProperty(MCR_CONFIGURATION_FILE, defaultProperties);
        }
    }

    @Test
    public void instance() throws Exception {
        try {
            MCREventManager.instance();
        } catch (MCRConfigurationException e) {
            assertEquals("Unsupported mode Foo for event handler.", e.getMessage());
        }
    }

    public static class FakeLuceneSearcher extends MCRSearcher {
        @Override
        public boolean isIndexer() {
            return true;
        }

        @Override
        public MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) {
            return null;
        }
    }
}
