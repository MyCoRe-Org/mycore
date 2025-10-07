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

package org.mycore.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@MyCoReTest
public class MCRComponentTest {

    private static final MCRComponent TEST = new MCRComponent("test", getSimpleManifest());

    private static final MCRComponent MIR_MODULE = new MCRComponent("mir-module", getSimpleManifest());

    private static final MCRComponent ACL_EDITOR2 = new MCRComponent("mycore-acl-editor2", getSimpleManifest());

    private static final MCRComponent MYCORE_BASE = new MCRComponent("mycore-base", getSimpleManifest());

    /**
     * Test method for {@link org.mycore.common.config.MCRComponent#getResourceBase()}.
     */
    @Test
    final void testGetResourceBase() {
        assertEquals("components/acl-editor2/config/", ACL_EDITOR2.getResourceBase(),
            "Did not get correct resource base.");
        assertEquals("config/", MYCORE_BASE.getResourceBase(),
            "Did not get correct resource base.");
        assertEquals("config/mir/", MIR_MODULE.getResourceBase(),
            "Did not get correct resource base.");
        assertEquals("config/test/", TEST.getResourceBase(),
            "Did not get correct resource base.");
    }

    /**
     * Test method for {@link org.mycore.common.config.MCRComponent#isMyCoReComponent()}.
     */
    @Test
    final void testIsMyCoReComponent() {
        assertTrue(ACL_EDITOR2.isMyCoReComponent(), "Is mycore component.");
        assertTrue(MYCORE_BASE.isMyCoReComponent(), "Is mycore component.");
        assertFalse(MIR_MODULE.isMyCoReComponent(), "Is not mycore component.");
        assertFalse(TEST.isMyCoReComponent(), "Is not mycore component.");
    }

    /**
     * Test method for {@link org.mycore.common.config.MCRComponent#isAppModule()}.
     */
    @Test
    final void testIsAppModule() {
        assertFalse(ACL_EDITOR2.isAppModule(), "Is not app module.");
        assertFalse(MYCORE_BASE.isAppModule(), "Is not app module.");
        assertTrue(MIR_MODULE.isAppModule(), "Is app module.");
        assertTrue(TEST.isAppModule(), "Is app module.");
    }

    /**
     * Test method for {@link org.mycore.common.config.MCRComponent#getName()}.
     */
    @Test
    final void testGetName() {
        assertEquals("acl-editor2", ACL_EDITOR2.getName(), "Did not name component correctly");
        assertEquals("base", MYCORE_BASE.getName(), "Did not name component correctly");
        assertEquals("mir", MIR_MODULE.getName(), "Did not name app module correctly");
        assertEquals("test", TEST.getName(), "Did not name app module correctly");
    }

    private static Manifest getSimpleManifest() {
        return new Manifest();
    }

}
