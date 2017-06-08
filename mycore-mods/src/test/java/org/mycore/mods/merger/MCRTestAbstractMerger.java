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
        String e = b;
        MCRTestMerger.test(a, b, e);
    }

    @Test
    public void testSimilar() throws Exception {
        String a = "[mods:abstract[@xml:lang='de']='Dies ist der deutsche Abstract']";
        String b = "[mods:abstract='Dies ist der deitsche Abstract']";
        String e = a;
        MCRTestMerger.test(a, b, e);

        String a2 = "[mods:abstract[@xml:lang='de']='Dies ist der deutsche Abstract']";
        String b2 = "[mods:abstract='Dieses ist der doitsche Äbschträkt']";
        String e2 = a2 + b2;
        MCRTestMerger.test(a2, b2, e2);
    }
}