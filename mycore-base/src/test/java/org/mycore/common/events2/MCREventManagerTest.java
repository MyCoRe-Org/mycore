package org.mycore.common.events2;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mycore.common.events2.MCREventManager.EVENT_HANDLER_PROPERTY_PREFIX;

public class MCREventManagerTest extends MCRTestCase {

    @Test
    public void testMCREventHandler(){
        final MCREventManager manager = MCREventManager.getInstance();

        manager.trigger(new MCRCreateObjectEvent(null));
        manager.trigger(new MCRUpdateObjectEvent(null));
    }

    @Override
    protected Map<String, String> getTestProperties() {
        final Map<String, String> testProperties = super.getTestProperties();

        testProperties.put(EVENT_HANDLER_PROPERTY_PREFIX+MCRTestEventHandler1.class.getName(), "true" );
        testProperties.put(EVENT_HANDLER_PROPERTY_PREFIX+MCRTestEventHandler2.class.getName(), "true" );
        testProperties.put(EVENT_HANDLER_PROPERTY_PREFIX+MCRTestEventHandler3.class.getName(), "true" );

        return testProperties;
    }
}
