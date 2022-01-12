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

package org.mycore.ocfl.layout;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.ocfl.MCROCFLTestUtil;

public class MCRLayoutTest extends MCROCFLTestUtil {

    private static MCRLayoutConfig layoutConfig;

    private static MCRLayoutExtension layoutExtension = new MCRLayoutExtension();

    @BeforeClass
    public static void setUp() {
        layoutConfig = new MCRLayoutConfig();
        layoutExtension.init(layoutConfig);
    }

    @After
    public void resetLayout() {
        layoutConfig = new MCRLayoutConfig();
        layoutExtension.init(layoutConfig);
    }

    @Test
    public void testLayoutShort() {
        assertEquals("ocfl/test/00/ocfl_test_0001",
            layoutExtension.mapObjectId(MCRConfiguration2.getStringOrThrow("MCR.OCFL.TestObject.Id")));
    }

    @Test
    public void testLayout() {
        layoutConfig.setPattern("00000000").setSlotLayout("4-2-2");
        assertEquals("ocfl/test/1234/56/ocfl_test_12345678",
            layoutExtension.mapObjectId("mycoreobject:ocfl_test_12345678"));
    }

    @Test
    public void testLayoutLong() {
        layoutConfig.setPattern("000000000000").setSlotLayout("2-4-3-2-1");
        assertEquals("ocfl/test/12/3456/789/01/ocfl_test_123456789012",
            layoutExtension.mapObjectId("mycoreobject:ocfl_test_123456789012"));
    }
}
