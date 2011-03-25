package org.mycore.common.events.test;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.events.MCREventManager;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRSortBy;

public class MCREventManagerTest {
    @Test
    public void instance() throws Exception {
        System.setProperty("MCR.Configuration.File", "props/" + getClass().getSimpleName() + ".properties");
        try {
            MCREventManager.instance();
        } catch (NullPointerException e) {
            e.printStackTrace();
            fail("There should be no " + e);
        } catch (MCRConfigurationException e) {
            assertEquals("Unsupported mode Foo for event handler.", e.getMessage());
        }
    }
    
    public static class FakeLuceneSearcher extends MCRSearcher{
        @Override
        public boolean isIndexer() {
            return true;
        }

        @Override
        public MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) {
            return null;
        }}
}
