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

package org.mycore.frontend.xeditor;

import static org.junit.Assert.assertTrue;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.common.xml.MCRXMLHelper;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXMLCleanerTest extends MCRTestCase {

    @Test
    public void testUnmodified() throws JDOMException, JaxenException {
        String xPath1 = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau']]";
        cleanAndCompareTo(xPath1, xPath1);

        String xPath2 = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau'][mods:namePart[@type='given']]]";
        cleanAndCompareTo(xPath2, xPath2);
    }

    @Test
    public void testRemoveEmptyNodes() throws JDOMException, JaxenException {
        String xPath1i = "root[child]";
        String xPath1o = "root";
        cleanAndCompareTo(xPath1i, xPath1o);

        String xPath2i = "root[child='a']";
        cleanAndCompareTo(xPath2i, xPath2i);

        String xPath3i = "root[child[@foo='bar']]";
        cleanAndCompareTo(xPath3i, xPath3i);

        String xPath7i = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau'][mods:namePart]]";
        String xPath7o = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau']]";
        cleanAndCompareTo(xPath7i, xPath7o);

        String xPath8i = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau'][mods:role/mods:roleTerm[@type]]]";
        String xPath8o = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau']]";
        cleanAndCompareTo(xPath8i, xPath8o);
    }

    @Test
    public void testRootAlwaysRemains() throws JDOMException, JaxenException {
        String xPath5i = "mods:mods[mods:name[@type][mods:namePart[@type]][mods:namePart]]";
        String xPath5o = "mods:mods";
        cleanAndCompareTo(xPath5i, xPath5o);
    }

    @Test
    public void testPreserveStructureAndService() throws JDOMException, JaxenException {
        String xPathInput = "mycoreobject[structure][metadata/field.title/title][service]";
        String xPathExpected = "mycoreobject[structure][service]";
        cleanAndCompareTo(xPathInput, xPathExpected);
    }

    @Test
    public void testOverwriteDefaultRules() throws JDOMException, JaxenException {
        String xPath2i = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau'][mods:namePart[@type='given']][mods:relatedItem/@xlink:href='test']]";
        String xPath2o = "mods:mods[mods:name[@type='personal'][mods:namePart[@type='family']='Musterfrau'][mods:relatedItem/@xlink:href='test']]";

        MCRCleaningRule removeElementsWithoutChildrenOrText = new MCRCleaningRule("//*",
            "* or (string-length(text()) > 0)");
        MCRCleaningRule keepRelatedItemWithReference = new MCRCleaningRule("//mods:relatedItem",
            "* or string-length(@xlink:href > 0)");

        cleanAndCompareTo(xPath2i, xPath2o, removeElementsWithoutChildrenOrText, keepRelatedItemWithReference);
    }

    private void cleanAndCompareTo(String xPathInput, String xPathExpectedOutput, MCRCleaningRule... rules)
        throws JaxenException,
        JDOMException {
        Document xmlToClean = buildTestDocument(xPathInput);
        Document expectedXML = buildTestDocument(xPathExpectedOutput);

        MCRXMLCleaner cleaner = new MCRXMLCleaner();
        for (MCRCleaningRule rule : rules)
            cleaner.addRule(rule);
        Document result = cleaner.clean(xmlToClean);

        assertTrue(MCRXMLHelper.deepEqual(expectedXML, result));
    }

    private Document buildTestDocument(String xPath) throws JaxenException, JDOMException {
        return new Document(new MCRNodeBuilder().buildElement(xPath, null, null));
    }
}
