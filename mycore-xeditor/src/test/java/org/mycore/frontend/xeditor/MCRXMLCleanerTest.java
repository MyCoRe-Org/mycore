/*
 * $Revision: 26669 $ 
 * $Date: 2013-04-08 09:37:07 +0200 (Mo, 08 Apr 2013) $
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

package org.mycore.frontend.xeditor;

import static org.junit.Assert.*;

import java.text.ParseException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRXMLHelper;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXMLCleanerTest extends MCRTestCase {

    @Test
    public void testUnmodified() throws JDOMException, ParseException {
        String xPath1 = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau']]";
        cleanAndCompareTo(xPath1, xPath1);

        String xPath2 = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau'][mods:namePart[@type='given']]]";
        cleanAndCompareTo(xPath2, xPath2);
    }

    @Test
    public void testRemoveEmptyNodes() throws JDOMException, ParseException {
        String xPath3i = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau'][mods:namePart]]";
        String xPath3o = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau']]";
        cleanAndCompareTo(xPath3i, xPath3o);

        String xPath4i = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau'][mods:role/mods:roleTerm[@type]]]";
        String xPath4o = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau']]";
        cleanAndCompareTo(xPath4i, xPath4o);
    }

    @Test
    public void testRootAlwaysRemains() throws JDOMException, ParseException {
        String xPath5i = "mods:mods[mods:name[@type][mods:namePart[@type]][mods:namePart]]";
        String xPath5o = "mods:mods";
        cleanAndCompareTo(xPath5i, xPath5o);
    }

    @Test
    public void testOverwriteDefaultRules() throws JDOMException, ParseException {
        String xPath2i = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau'][mods:namePart[@type='given']][mods:relatedItem/@xlink:href='test']]";
        String xPath2o = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau'][mods:relatedItem/@xlink:href='test']]";

        MCRCleaningRule removeElementsWithoutChildrenOrText = new MCRCleaningRule("//*", "* or (string-length(text()) > 0)");
        MCRCleaningRule keepRelatedItemWithReference = new MCRCleaningRule("//mods:relatedItem", "* or string-length(@xlink:href > 0)");

        cleanAndCompareTo(xPath2i, xPath2o, removeElementsWithoutChildrenOrText, keepRelatedItemWithReference);
    }

    private void cleanAndCompareTo(String xPathInput, String xPathExpectedOutput, MCRCleaningRule... rules) throws ParseException,
            JDOMException {
        Document xmlToClean = buildTestDocument(xPathInput);
        Document expectedXML = buildTestDocument(xPathExpectedOutput);

        MCRXMLCleaner cleaner = new MCRXMLCleaner(xmlToClean);
        for (MCRCleaningRule rule : rules)
            cleaner.addRule(rule);
        cleaner.clean();

        assertTrue(MCRXMLHelper.deepEqual(expectedXML, xmlToClean));
    }

    private Document buildTestDocument(String xPath) throws ParseException, JDOMException {
        Element root = (Element) (MCRNodeBuilder.build(xPath, null, null, null));
        return new Document(root);
    }
}
