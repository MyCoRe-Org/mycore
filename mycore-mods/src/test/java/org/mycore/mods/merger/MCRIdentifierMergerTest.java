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

public class MCRIdentifierMergerTest extends MCRTestCase {

    @Test
    public void testMergeDifferent() throws Exception {
        String a = "[mods:identifier[@type='doi']='10.123/456']";
        String b = "[mods:identifier[@type='issn']='1234-5678']";
        String e = "[mods:identifier[@type='doi']='10.123/456'][mods:identifier[@type='issn']='1234-5678']";
        MCRMergerTest.test(a, b, e);
    }

    @Test
    public void testMergeSame() throws Exception {
        String a = "[mods:identifier[@type='issn']='12345678']";
        String b = "[mods:identifier[@type='issn']='1234-5678']";
        MCRMergerTest.test(a, b, b);
    }

    @Test
    public void testMergeMultiple() throws Exception {
        String a = "[mods:identifier[@type='isbn']='9783836287456'][mods:identifier[@type='isbn']='3836287455']";
        String b = "[mods:identifier[@type='isbn']='978-3-8362-8745-6']";
        String e = "[mods:identifier[@type='isbn']='978-3-8362-8745-6'][mods:identifier[@type='isbn']='3836287455']";
        MCRMergerTest.test(a, b, e);
    }

    @Test
    public void testCaseInsensitiveDOIs() throws Exception {
        String a = "[mods:identifier[@type='doi']='10.1530/EJE-21-1086']";
        String b = "[mods:identifier[@type='doi']='10.1530/eje-21-1086']";
        MCRMergerTest.test(a, b, a);
    }
}
