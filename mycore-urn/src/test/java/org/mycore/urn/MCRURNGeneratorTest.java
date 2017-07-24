package org.mycore.urn;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.urn.services.MCRURN;
import org.mycore.urn.services.MCRURNManager;

@Deprecated
public class MCRURNGeneratorTest extends MCRJPATestCase {
    @Test
    public void testChecksum() {
        assertEquals(true, MCRURN.isValid("urn:nbn:de:gbv:28-diss2015-0237-9"));
        assertEquals("urn:nbn:de:gbv:28-diss2015-0237-9", MCRURN.parse("urn:nbn:de:gbv:28-diss2015-0237-9").toString());
        //urn:nbn:de:gbv:28-diss2015-0237-9
        MCRURN urn = null;
        urn = MCRURN.create("urn:nbn:de:gbv:28-diss2015-0237-");
        assertEquals(9, urn.getChecksum());

        //urn:nbn:de:urmel-72c7b252-be9c-427e-84e2-29dd208a2a3a5-00000599-4616
        urn = MCRURN.create("urn:nbn:de:urmel-72c7b252-be9c-427e-84e2-29dd208a2a3a5-00000599-461");
        assertEquals(6, urn.getChecksum());

        //urn:nbn:de:hbz:464-20150331-150029-3
        urn = MCRURN.parse("urn:nbn:de:hbz:464-20150331-150029-3");
        assertEquals(3, urn.getChecksum());
        //
    }

    @Test
    public void testGenerationWithCounter() {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("MCR.Persistence.URN.Store.Class", "org.mycore.urn.hibernate.MCRHIBURNStore");
        properties.put("MCR.URN.SubNamespace.test.NISSBuilder", "org.mycore.urn.services.MCRNISSBuilderDateCounter");
        properties.put("MCR.URN.SubNamespace.test.Prefix", "urn:nbn:de:test:007-test1-");
        properties.put("MCR.URN.SubNamespace.test.NISSPattern", "0000-");
        MCRConfiguration.instance().initialize(properties, true);

        String urn = null;

        for (int i = 1; i <= 5; i++) {
            urn = MCRURNManager.buildAndAssignURN("Docportal_test_" + String.format("%08d", i), "test");
        }
        assertEquals("urn:nbn:de:test:007-test1-0005-3", urn);
    }

    @Test
    public void testDuplicateURN() {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("MCR.Persistence.URN.Store.Class", "org.mycore.urn.hibernate.MCRHIBURNStore");
        properties.put("MCR.URN.SubNamespace.test2.NISSBuilder", "org.mycore.urn.services.MCRNISSBuilderDateCounter");
        properties.put("MCR.URN.SubNamespace.test2.Prefix", "urn:nbn:de:test:007-test2-");
        properties.put("MCR.URN.SubNamespace.test2.NISSPattern", "yyyy");
        MCRConfiguration.instance().initialize(properties, true);

        MCRURNManager.buildAndAssignURN("Docportal_test_000000001", "test2");
        String errorMsg = "";
        try {
            MCRURNManager.buildAndAssignURN("Docportal_test_000000002", "test2");
        } catch (MCRException me) {
            errorMsg = me.getMessage();
        }
        assertEquals(true, errorMsg.contains("unique"));
    }

    @Test
    public void testDuplicateMCRID() {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("MCR.Persistence.URN.Store.Class", "org.mycore.urn.hibernate.MCRHIBURNStore");
        properties.put("MCR.URN.SubNamespace.test3.NISSBuilder", "org.mycore.urn.services.MCRNISSBuilderDateCounter");
        properties.put("MCR.URN.SubNamespace.test3.Prefix", "urn:nbn:de:test:007-test3-");
        properties.put("MCR.URN.SubNamespace.test3.NISSPattern", "#");
        MCRConfiguration.instance().initialize(properties, true);

        String urn = MCRURNManager.buildAndAssignURN("Docportal_test_000000001", "test3");
        String urn2 = MCRURNManager.buildAndAssignURN("Docportal_test_000000001", "test3");
        System.out.println("urn1 = urn2 -> " + urn + " = " + urn2);
        assertEquals(urn, urn2);
    }

}
