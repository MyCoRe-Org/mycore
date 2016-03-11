package org.mycore.urn;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.urn.services.MCRURN;

import static org.junit.Assert.assertEquals;

public class MCRURNGeneratorTest extends MCRTestCase{
    @Test
    public void testChecksum() {
        assertEquals(true, MCRURN.isValid("urn:nbn:de:gbv:28-diss2015-0237-9"));
        assertEquals("urn:nbn:de:gbv:28-diss2015-0237-9", MCRURN.parse("urn:nbn:de:gbv:28-diss2015-0237-9").toString());
        //urn:nbn:de:gbv:28-diss2015-0237-9
        MCRURN urn=null; 
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
}
