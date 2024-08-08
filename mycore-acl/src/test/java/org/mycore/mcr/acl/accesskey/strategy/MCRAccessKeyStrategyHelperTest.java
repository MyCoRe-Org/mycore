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

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mycore.access.MCRAccessManager;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;

public class MCRAccessKeyStrategyHelperTest {

    private static MCRAccessKeyDto accessKeyDto;

    @Before
    public void setup() {
        accessKeyDto = new MCRAccessKeyDto();
        accessKeyDto.setReference("bla");
        accessKeyDto.setPermission(MCRAccessManager.PERMISSION_READ);
    }

    @Test
    public void testSanitizePermission() {
        assertEquals(MCRAccessManager.PERMISSION_DELETE,
            MCRAccessKeyStrategyHelper.sanitizePermission(MCRAccessManager.PERMISSION_DELETE));
        assertEquals(MCRAccessManager.PERMISSION_READ,
            MCRAccessKeyStrategyHelper.sanitizePermission(MCRAccessManager.PERMISSION_PREVIEW));
        assertEquals(MCRAccessManager.PERMISSION_READ,
            MCRAccessKeyStrategyHelper.sanitizePermission(MCRAccessManager.PERMISSION_READ));
        assertEquals(MCRAccessManager.PERMISSION_READ,
            MCRAccessKeyStrategyHelper.sanitizePermission(MCRAccessManager.PERMISSION_VIEW));
        assertEquals(MCRAccessManager.PERMISSION_WRITE,
            MCRAccessKeyStrategyHelper.sanitizePermission(MCRAccessManager.PERMISSION_WRITE));
    }

    @Test
    public void testVerifyAccessKey() {
        assertTrue(MCRAccessKeyStrategyHelper.verifyAccessKey(MCRAccessManager.PERMISSION_READ, accessKeyDto));
        assertFalse(MCRAccessKeyStrategyHelper.verifyAccessKey(MCRAccessManager.PERMISSION_WRITE, accessKeyDto));
        accessKeyDto.setPermission(MCRAccessManager.PERMISSION_WRITE);
        assertTrue(MCRAccessKeyStrategyHelper.verifyAccessKey(MCRAccessManager.PERMISSION_READ, accessKeyDto));
        assertTrue(MCRAccessKeyStrategyHelper.verifyAccessKey(MCRAccessManager.PERMISSION_WRITE, accessKeyDto));
        assertFalse(MCRAccessKeyStrategyHelper.verifyAccessKey(MCRAccessManager.PERMISSION_DELETE, accessKeyDto));
    }

    @Test
    public void testVerifyAccessKeyIsActive() {
        assertTrue(MCRAccessKeyStrategyHelper.verifyAccessKey(MCRAccessManager.PERMISSION_READ, accessKeyDto));
        accessKeyDto.setActive(false);
        assertFalse(MCRAccessKeyStrategyHelper.verifyAccessKey(MCRAccessManager.PERMISSION_READ, accessKeyDto));
        accessKeyDto.setActive(true);
        assertTrue(MCRAccessKeyStrategyHelper.verifyAccessKey(MCRAccessManager.PERMISSION_READ, accessKeyDto));
    }

    @Test
    public void testVerifyAccessKeyExpiration() {
        assertTrue(MCRAccessKeyStrategyHelper.verifyAccessKey(MCRAccessManager.PERMISSION_READ, accessKeyDto));
        accessKeyDto.setExpiration(new Date(new Date().getTime() - (1000 * 60 * 60 * 24))); // yesterday
        assertFalse(MCRAccessKeyStrategyHelper.verifyAccessKey(MCRAccessManager.PERMISSION_READ, accessKeyDto));
        accessKeyDto.setExpiration(new Date(new Date().getTime() + (1000 * 60 * 60 * 24))); //tomorrow
        assertTrue(MCRAccessKeyStrategyHelper.verifyAccessKey(MCRAccessManager.PERMISSION_READ, accessKeyDto));
    }
}
