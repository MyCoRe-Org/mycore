/*
 * $Revision$ $Date$
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
