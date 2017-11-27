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

public class MCRTestIdentifierMerger extends MCRTestCase {

    @Test
    public void testMergeDifferent() throws Exception {
        String a = "[mods:identifier[@type='doi']='10.123/456']";
        String b = "[mods:identifier[@type='issn']='1234-5678']";
        String e = "[mods:identifier[@type='doi']='10.123/456'][mods:identifier[@type='issn']='1234-5678']";
        MCRTestMerger.test(a, b, e);
    }

    @Test
    public void testMergeSame() throws Exception {
        String a = "[mods:identifier[@type='issn']='12345678']";
        String b = "[mods:identifier[@type='issn']='1234-5678']";
        MCRTestMerger.test(a, b, b);
    }
}
