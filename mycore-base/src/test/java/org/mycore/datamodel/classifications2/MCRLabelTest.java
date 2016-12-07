package org.mycore.datamodel.classifications2;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRLabelTest extends MCRTestCase {

    @Test
    public final void testMCRLabelStringStringString() {
        MCRLabel de = new MCRLabel("de", "test", null);
        try {
            MCRLabel de2 = new MCRLabel("de", null, null);
            fail("MCRLabel should not allow 'null' as 'text'.");
        } catch (NullPointerException e) {
        }
        try {
            MCRLabel de3 = new MCRLabel("de", "", null);
            fail("MCRLabel should not allow empty 'text'.");
        } catch (IllegalArgumentException e) {
        }
        try {
            MCRLabel de4 = new MCRLabel("de", " ", null);
        } catch (IllegalArgumentException e) {
        }
        MCRLabel xUri = new MCRLabel("x-uri", "http://...", null);
        try {
            MCRLabel xxl = new MCRLabel("x-toolong", "http://...", null);
        } catch (IllegalArgumentException e) {
        }
    }

}
