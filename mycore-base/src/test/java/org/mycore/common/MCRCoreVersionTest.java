/*
 * $RCSfile$
 * $Revision: 1 $ $Date: 17.07.2009 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
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
