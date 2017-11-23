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

package org.mycore.mods.merger;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRTestExtentMerger extends MCRTestCase {

    @Test
    public void testPhysicalDescriptionExtent() throws Exception {
        String a = "[mods:physicalDescription[mods:extent='360 pages']]";
        String b = "[mods:physicalDescription[mods:extent='7\" x 9\"']]";
        MCRTestMerger.test(a, b, a);
    }

    @Test
    public void testPartExtentList() throws Exception {
        String a = "[mods:part[mods:extent[@unit='pages'][mods:list='S. 64-67']]]";
        String b = "[mods:part[mods:extent[@unit='pages'][mods:list='pp. 64-67']]]";
        MCRTestMerger.test(a, b, a);
    }

    @Test
    public void testPartExtentStartEnd() throws Exception {
        String a = "[mods:part[mods:extent[@unit='pages'][mods:list='S. 64-67']]]";
        String b = "[mods:part[mods:extent[@unit='pages'][mods:start='64'][mods:end='67']]]";
        MCRTestMerger.test(a, b, b);
        MCRTestMerger.test(b, a, b);
    }
}
