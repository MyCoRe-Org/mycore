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

public class MCRTestAbstractMerger extends MCRTestCase {

    @Test
    public void testMerge() throws Exception {
        String a = "[mods:abstract[@xml:lang='de']='deutsch']";
        String b = "[mods:abstract='deutsch'][mods:abstract[@xml:lang='en']='english']";
        String e = "[mods:abstract[@xml:lang='de']='deutsch'][mods:abstract[@xml:lang='en']='english']";
        MCRTestMerger.test(a, b, e);
    }

    @Test
    public void testXLink() throws Exception {
        String a = "[mods:abstract[@xlink:href='foo']]";
        String b = "[mods:abstract[@xml:lang='de'][@xlink:href='foo']][mods:abstract[@xml:lang='en'][@xlink:href='bar']]";
        MCRTestMerger.test(a, b, b);
    }

    @Test
    public void testSimilar() throws Exception {
        String a = "[mods:abstract[@xml:lang='de']='Dies ist der deutsche Abstract']";
        String b = "[mods:abstract='Dies ist der deitsche Abstract']";
        MCRTestMerger.test(a, b, a);

        String a2 = "[mods:abstract[@xml:lang='de']='Dies ist der deutsche Abstract']";
        String b2 = "[mods:abstract='Dieses ist der doitsche Äbschträkt']";
        String e2 = a2 + b2;
        MCRTestMerger.test(a2, b2, e2);
    }
}
