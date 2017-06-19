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
        String e = b;
        MCRTestMerger.test(a, b, e);
    }
}
