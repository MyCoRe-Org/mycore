/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Ignore;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.common.xml.MCRXMLHelper;

public class MCRMergerTest extends MCRTestCase {

    @Test
    public void testAddingNew() throws Exception {
        String a = "[mods:note[@xml:lang='de']='deutsch']";
        String b = "[mods:note[@xml:lang='de']='deutsch'][mods:note[@xml:lang='en']='english']";
        test(a, b, b);
    }

    @Test
    public void testJoiningDifferent() throws JaxenException, IOException {
        String a = "[mods:titleInfo[mods:title='test']]";
        String b = "[mods:abstract='abstract']";
        String e = "[mods:titleInfo[mods:title='test']][mods:abstract='abstract']";
        test(a, b, e);
    }

    @Ignore
    static void test(String xPathA, String xPathB, String xPathExpected) throws JaxenException, IOException {
       test(new String[]{xPathA, xPathB}, xPathExpected);
    }

    @Ignore
    static void test(String[] xPaths, String xPathExpected) throws JaxenException, IOException {
        Element[] elements = new Element[xPaths.length];
        for (int i=0, n=xPaths.length; i<n; i++) {
            elements[i] = new MCRNodeBuilder().buildElement("mods:mods" + xPaths[i], null, null);
        }
        Element e = new MCRNodeBuilder().buildElement("mods:mods" + xPathExpected, null, null);

       for (int i=1, n=xPaths.length; i<n; i++) {
            MCRMergeTool.merge(elements[0], elements[i]);
        }
 
        boolean asExpected = MCRXMLHelper.deepEqual(e, elements[0]);

        if (!asExpected) {
            System.out.println("actual result:");
            logXML(elements[0]);
            System.out.println("expected result:");
            logXML(e);
        }

        assertTrue(asExpected);
    }

    @Ignore
    private static void logXML(Element r) throws IOException {
        System.out.println();
        new XMLOutputter(Format.getPrettyFormat()).output(r, System.out);
        System.out.println();
    }
}
