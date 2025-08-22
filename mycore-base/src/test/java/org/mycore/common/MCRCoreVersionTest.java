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

package org.mycore.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@MyCoReTest
public class MCRCoreVersionTest {

    /**
     * Test method for {@link org.mycore.common.MCRCoreVersion#getVersion()}.
     */
    @Test
    public void getVersion() {
        assertFalse(MCRCoreVersion.getVersion().isEmpty(), "Length of version string is zero.");
    }

    /**
     * Test method for {@link org.mycore.common.MCRCoreVersion#getRevision()}.
     */
    @Test
    public void getRevision() {
        assertTrue(MCRCoreVersion.getRevision().matches("[a-fA-F0-9]{40}"),
            "Revision is not a SHA1 hash: " + MCRCoreVersion.getRevision());
    }

}
