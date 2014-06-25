package org.mycore.frontend.jersey.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.frontend.jersey.config.MCRResourceConfig;


public class MCRClassScannerTest extends MCRTestCase {

    @Test
    public void scan() throws Exception {
        MCRResourceConfig config = new MCRResourceConfig();
        assertEquals(1, config.getPackages().length);
    }

}
