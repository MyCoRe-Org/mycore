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

package org.mycore.mcr.acl.accesskey.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mycore.access.MCRAccessManager.PERMISSION_DELETE;
import static org.mycore.access.MCRAccessManager.PERMISSION_PREVIEW;
import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.access.MCRAccessManager.PERMISSION_VIEW;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

public class MCRAccessKeyStrategyHelperTest extends MCRTestCase {

    private static MCRAccessKey accessKey;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        accessKey = new MCRAccessKey("bla", PERMISSION_READ);
    }

    @Test
    public void testSanitizePermission() {
        assertEquals(PERMISSION_DELETE, MCRAccessKeyStrategyHelper.sanitizePermission(PERMISSION_DELETE));
        assertEquals(PERMISSION_READ, MCRAccessKeyStrategyHelper.sanitizePermission(PERMISSION_PREVIEW));
        assertEquals(PERMISSION_READ, MCRAccessKeyStrategyHelper.sanitizePermission(PERMISSION_READ));
        assertEquals(PERMISSION_READ, MCRAccessKeyStrategyHelper.sanitizePermission(PERMISSION_VIEW));
        assertEquals(PERMISSION_WRITE, MCRAccessKeyStrategyHelper.sanitizePermission(PERMISSION_WRITE));
    }

    @Test
    public void testVerifyAccessKey() {
        assertTrue(MCRAccessKeyStrategyHelper.verifyAccessKey(PERMISSION_READ, accessKey));
        assertFalse(MCRAccessKeyStrategyHelper.verifyAccessKey(PERMISSION_WRITE, accessKey));
        accessKey.setType(PERMISSION_WRITE);
        assertTrue(MCRAccessKeyStrategyHelper.verifyAccessKey(PERMISSION_READ, accessKey));
        assertTrue(MCRAccessKeyStrategyHelper.verifyAccessKey(PERMISSION_WRITE, accessKey));
        assertFalse(MCRAccessKeyStrategyHelper.verifyAccessKey(PERMISSION_DELETE, accessKey));
    }

    public void testVerifyAccessKeyIsActive() {
        assertTrue(MCRAccessKeyStrategyHelper.verifyAccessKey(PERMISSION_READ, accessKey));
        accessKey.setIsActive(false);
        assertFalse(MCRAccessKeyStrategyHelper.verifyAccessKey(PERMISSION_READ, accessKey));
        accessKey.setIsActive(true);
        assertTrue(MCRAccessKeyStrategyHelper.verifyAccessKey(PERMISSION_READ, accessKey));
    }

    public void testVerifyAccessKeyExpiration() {
        assertTrue(MCRAccessKeyStrategyHelper.verifyAccessKey(PERMISSION_READ, accessKey));
        accessKey.setExpiration(new Date());
        assertFalse(MCRAccessKeyStrategyHelper.verifyAccessKey(PERMISSION_READ, accessKey));
        accessKey.setExpiration(new Date(new Date().getTime() + (1000 * 60 * 60 * 24))); //tomorrow
        assertTrue(MCRAccessKeyStrategyHelper.verifyAccessKey(PERMISSION_READ, accessKey));
    }
}
