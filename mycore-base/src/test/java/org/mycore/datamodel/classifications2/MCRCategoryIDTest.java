/**
 * 
 */
package org.mycore.datamodel.classifications2;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRCategoryIDTest extends MCRTestCase {
    private static final String invalidID = "identifier:.sub";

    private static final String validRootID = "rootID";

    private static final String validCategID = "categID";

    private static final String toLongRootID = "012345678901234567890123456789012";

    private static final String toLongCategID = "012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678";

    /**
     * Test method for {@link org.mycore.datamodel.classifications2.MCRCategoryID#MCRCategoryID(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testMCRCategoryIDStringString() {
        MCRCategoryID categID;
        categID = new MCRCategoryID(validRootID, validCategID);
        assertEquals("RootIDs do not match", validRootID, categID.getRootID());
        assertEquals("CategIDs do not match", validCategID, categID.getID());
    }

    @Test
    public void testRootID() {
        assertEquals("RootIds do not match", validRootID, MCRCategoryID.rootID(validRootID).getRootID());
    }

    @Test(expected = MCRException.class)
    public void testInvalidRootID() {
        @SuppressWarnings("unused")
        MCRCategoryID categID = new MCRCategoryID(invalidID, validCategID);
    }

    @Test(expected = MCRException.class)
    public void testInvalidCategID() {
        @SuppressWarnings("unused")
        MCRCategoryID categID = new MCRCategoryID(validRootID, invalidID);
    }

    @Test(expected = MCRException.class)
    public void testLongCategID() {
        @SuppressWarnings("unused")
        MCRCategoryID categID = new MCRCategoryID(validRootID, toLongCategID);
    }

    @Test(expected = MCRException.class)
    public void testLongRootID() {
        @SuppressWarnings("unused")
        MCRCategoryID categID = new MCRCategoryID(toLongRootID, validCategID);
    }
}
