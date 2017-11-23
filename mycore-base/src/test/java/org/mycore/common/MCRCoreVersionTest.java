/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRCoreVersionTest extends MCRTestCase {

    /**
     * Test method for {@link org.mycore.common.MCRCoreVersion#getVersion()}.
     */
    @Test
    public void getVersion() {
        assertTrue("Length of version string is zero.", MCRCoreVersion.getVersion().length() > 0);
    }

    /**
     * Test method for {@link org.mycore.common.MCRCoreVersion#getRevision()}.
     */
    @Test
    public void getRevision() {
        assertTrue("Revision is not a SHA1 hash: " + MCRCoreVersion.getRevision(),
            MCRCoreVersion.getRevision().matches("[a-fA-F0-9]{40}"));
    }

}
