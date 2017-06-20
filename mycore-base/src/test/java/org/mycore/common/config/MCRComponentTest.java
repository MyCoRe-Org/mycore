/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 3, 2013 $
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

package org.mycore.common.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.jar.Manifest;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRComponentTest extends MCRTestCase {

    private static final MCRComponent TEST = new MCRComponent("test", getSimpleManifest());

    private static final MCRComponent MIR_MODULE = new MCRComponent("mir-module", getSimpleManifest());

    private static final MCRComponent ACL_EDITOR2 = new MCRComponent("mycore-acl-editor2", getSimpleManifest());

    private static final MCRComponent MYCORE_BASE = new MCRComponent("mycore-base", getSimpleManifest());

    /**
     * Test method for {@link org.mycore.common.config.MCRComponent#getResourceBase()}.
     */
    @Test
    public final void testGetResourceBase() {
        assertEquals("Did not get correct resource base.", "components/acl-editor2/config/",
            ACL_EDITOR2.getResourceBase());
        assertEquals("Did not get correct resource base.", "config/", MYCORE_BASE.getResourceBase());
        assertEquals("Did not get correct resource base.", "config/mir/", MIR_MODULE.getResourceBase());
        assertEquals("Did not get correct resource base.", "config/test/", TEST.getResourceBase());
    }

    /**
     * Test method for {@link org.mycore.common.config.MCRComponent#isMyCoReComponent()}.
     */
    @Test
    public final void testIsMyCoReComponent() {
        assertTrue("Is mycore component.", ACL_EDITOR2.isMyCoReComponent());
        assertTrue("Is mycore component.", MYCORE_BASE.isMyCoReComponent());
        assertFalse("Is not mycore component.", MIR_MODULE.isMyCoReComponent());
        assertFalse("Is not mycore component.", TEST.isMyCoReComponent());
    }

    /**
     * Test method for {@link org.mycore.common.config.MCRComponent#isAppModule()}.
     */
    @Test
    public final void testIsAppModule() {
        assertFalse("Is not app module.", ACL_EDITOR2.isAppModule());
        assertFalse("Is not app module.", MYCORE_BASE.isAppModule());
        assertTrue("Is app module.", MIR_MODULE.isAppModule());
        assertTrue("Is app module.", TEST.isAppModule());
    }

    /**
     * Test method for {@link org.mycore.common.config.MCRComponent#getName()}.
     */
    @Test
    public final void testGetName() {
        assertEquals("Did not name component correctly", "acl-editor2", ACL_EDITOR2.getName());
        assertEquals("Did not name component correctly", "base", MYCORE_BASE.getName());
        assertEquals("Did not name app module correctly", "mir", MIR_MODULE.getName());
        assertEquals("Did not name app module correctly", "test", TEST.getName());
    }

    private static Manifest getSimpleManifest() {
        Manifest m = new Manifest();
        return m;
    }

}
