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

import java.io.IOException;

import org.jaxen.JaxenException;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRTestTitleInfoMerger extends MCRTestCase {

    @Test
    public void testMerge() throws Exception {
        String a = "[mods:titleInfo[mods:title='Testing'][mods:subTitle='All You have to know about']]";
        String b = "[mods:titleInfo[mods:title='testing: all you have to know about']]";
        String e = "[mods:titleInfo[mods:title='Testing'][mods:subTitle='All You have to know about']]";
        MCRTestMerger.test(a, b, e);
    }

    @Test
    public void testLongerWins() throws Exception {
        String a = "[mods:titleInfo[mods:title='Applied Physics A']]";
        String b = "[mods:titleInfo[mods:title='Applied Physics A : Materials Science & Processing']]";
        String e = b;
        MCRTestMerger.test(a, b, e);
    }

    @Test
    public void testMergingTitleSubtitle() throws JaxenException, IOException {
        String a = "[mods:titleInfo[mods:title='testing: all you have to know about']]";
        String b = "[mods:titleInfo[mods:title='Testing'][mods:subTitle='All You have to know about']]";
        String e = b;
        MCRTestMerger.test(a, b, e);
    }

    @Test
    public void testMergingAttributes() throws JaxenException, IOException {
        String a = "[mods:titleInfo[mods:title='first'][@xml:lang='de']][mods:titleInfo[mods:title='second']]";
        String b = "[mods:titleInfo[mods:title='first']][mods:titleInfo[mods:title='second'][@xml:lang='en'][@type='alternative']]";
        String e = "[mods:titleInfo[mods:title='first'][@xml:lang='de']][mods:titleInfo[mods:title='second'][@xml:lang='en'][@type='alternative']]";
        MCRTestMerger.test(a, b, e);
    }

    @Test
    public void testMergingDifferent() throws JaxenException, IOException {
        String a = "[mods:titleInfo[mods:title='a']]";
        String b = "[mods:titleInfo[mods:title='b']]";
        String e = "[mods:titleInfo[mods:title='a']][mods:titleInfo[mods:title='b']]";
        MCRTestMerger.test(a, b, e);
    }

    @Test
    public void testMergingIdentical() throws JaxenException, IOException {
        String a = "[mods:titleInfo[mods:title='test']]";
        String b = a;
        String e = a;
        MCRTestMerger.test(a, b, e);
    }
}
