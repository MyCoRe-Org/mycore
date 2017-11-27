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

public class MCRTestOriginInfoMerger extends MCRTestCase {

    @Test
    public void testMerge() throws Exception {
        String a = "[mods:originInfo[mods:dateIssued='2017'][mods:publisher='Elsevier']]";
        String b = "[mods:originInfo[mods:dateIssued[@encoding='w3cdtf']='2017']"
            + "[mods:edition='4. Aufl.'][mods:place='Berlin']]";
        String e = "[mods:originInfo[mods:dateIssued[@encoding='w3cdtf']='2017']"
            + "[mods:publisher='Elsevier'][mods:edition='4. Aufl.'][mods:place='Berlin']]";
        MCRTestMerger.test(a, b, e);
    }

    @Test
    public void testDateOther() throws Exception {
        String a = "[mods:originInfo[mods:dateOther[@type='accepted']='2017']]";
        String b = "[mods:originInfo[mods:dateOther[@type='submitted']='2018']]";
        String e = "[mods:originInfo[mods:dateOther[@type='accepted']='2017']"
            + "[mods:dateOther[@type='submitted']='2018']]";
        MCRTestMerger.test(a, b, e);
    }
}
