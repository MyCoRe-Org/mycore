/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.classifications2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mycore.common.MCRException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRCategoryIDTest {
    private static final String invalidID = "identifier:.sub";

    private static final String validRootID = "rootID";

    private static final String validCategID = "categID";

    private static final String toLongRootID = "012345678901234567890123456789012";

    private static final String toLongCategID =
        "012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678";

    /**
     * Test method for {@link org.mycore.datamodel.classifications2.MCRCategoryID#MCRCategoryID(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testMCRCategoryIDStringString() {
        MCRCategoryID categID;
        categID = new MCRCategoryID(validRootID, validCategID);
        assertEquals("RootIDs do not match", validRootID, categID.getRootID());
        assertEquals("CategIDs do not match", validCategID, categID.getId());
    }

    @Test
    public void testRootID() {
        assertEquals("RootIds do not match", validRootID, MCRCategoryID.rootID(validRootID).getRootID());
    }

    @Test
    public void testInvalidRootID() {
        assertThrows(MCRException.class, () -> new MCRCategoryID(invalidID, validCategID));
    }

    @Test
    public void testInvalidCategID() {
        assertThrows(MCRException.class, () -> new MCRCategoryID(validRootID, invalidID));
    }

    @Test
    public void testLongCategID() {
        assertThrows(MCRException.class, () -> new MCRCategoryID(validRootID, toLongCategID));
    }

    @Test
    public void testLongRootID() {
        assertThrows(MCRException.class, () -> new MCRCategoryID(toLongRootID, validCategID));
    }

    /**
     * @see <a href="https://mycore.atlassian.net/browse/MCR-3302">MCR-3302</a>
     */
    @ParameterizedTest
    @Tag("MCR-3302")
    @ValueSource(strings = {
        "", "foo:bar:baz", ":bar", ":bar:", ":bar:baz", "foo::bar", "foo::bar::", "foo::bar::baz", "::bar", "::bar::baz"
    })
    public void testInvalidEdgeCases(String invalidCategoryId) {
        assertThrows(IllegalArgumentException.class, () -> MCRCategoryID.fromString(invalidCategoryId));
    }

}
