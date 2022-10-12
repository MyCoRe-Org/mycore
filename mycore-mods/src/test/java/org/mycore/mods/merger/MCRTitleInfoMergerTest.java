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

import java.io.IOException;

import org.jaxen.JaxenException;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRTitleInfoMergerTest extends MCRTestCase {

    @Test
    public void testMerge() throws Exception {
        String a = "[mods:titleInfo[mods:title='Testing'][mods:subTitle='All You have to know about']]";
        String b = "[mods:titleInfo[mods:title='testing: all you have to know about']]";
        String e = "[mods:titleInfo[mods:title='Testing'][mods:subTitle='All You have to know about']]";
        MCRMergerTest.test(a, b, e);
    }

    @Test
    public void testLongerWins() throws Exception {
        String a = "[mods:titleInfo[mods:title='Applied Physics A']]";
        String b = "[mods:titleInfo[mods:title='Applied Physics A : Materials Science & Processing']]";
        MCRMergerTest.test(a, b, b);
    }

    @Test
    public void testMergingTitleSubtitle() throws JaxenException, IOException {
        String a = "[mods:titleInfo[mods:title='testing: all you have to know about']]";
        String b = "[mods:titleInfo[mods:title='Testing'][mods:subTitle='All You have to know about']]";
        MCRMergerTest.test(a, b, b);
    }

    @Test
    public void testMergingAttributes() throws JaxenException, IOException {
        String a = "[mods:titleInfo[mods:title='first'][@xml:lang='de']]" + "[mods:titleInfo[mods:title='second']]";
        String b = "[mods:titleInfo[mods:title='first']"
            + "][mods:titleInfo[mods:title='second'][@xml:lang='en'][@type='alternative']]";
        String e = "[mods:titleInfo[mods:title='first'][@xml:lang='de']]"
            + "[mods:titleInfo[mods:title='second']]"
            + "[mods:titleInfo[mods:title='second'][@xml:lang='en'][@type='alternative']]";
        MCRMergerTest.test(a, b, e);
    }

    @Test
    public void testMergingDifferent() throws JaxenException, IOException {
        String a = "[mods:titleInfo[mods:title='a']]";
        String b = "[mods:titleInfo[mods:title='b']]";
        String e = "[mods:titleInfo[mods:title='a']][mods:titleInfo[mods:title='b']]";
        MCRMergerTest.test(a, b, e);
    }

    @Test
    public void testMergingIdentical() throws JaxenException, IOException {
        String a = "[mods:titleInfo[mods:title='test']]";
        MCRMergerTest.test(a, a, a);
    }

    @Test
    public void testMergingSameAttribute() throws JaxenException, IOException {
        String a = "[mods:titleInfo[mods:title='Chemistry - A European Journal'][@type='abbreviated']]";
        String b = "[mods:titleInfo[mods:title='Chemistry'][@type='abbreviated']]";
        String e = "[mods:titleInfo[mods:title='Chemistry - A European Journal'][@type='abbreviated']]";
        MCRMergerTest.test(a, b, e);
    }

    @Test
    public void testMergingOneAttribute() throws JaxenException, IOException {
        String a = "[mods:titleInfo[mods:title='Chemistry - A European Journal']]";
        String b = "[mods:titleInfo[mods:title='Chemistry'][@type='abbreviated']]";
        String e = "[mods:titleInfo[mods:title='Chemistry - A European Journal']]"
            + "[mods:titleInfo[mods:title='Chemistry'][@type='abbreviated']]";
        MCRMergerTest.test(a, b, e);
    }
}
