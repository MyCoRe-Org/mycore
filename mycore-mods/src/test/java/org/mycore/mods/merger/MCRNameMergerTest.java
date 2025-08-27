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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.test.MyCoReTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@MyCoReTest
public class MCRNameMergerTest {

    @Test
    public void testIsProbablySameAs() throws Exception {
        MCRNameMerger a = buildNameEntry("[mods:namePart='Thomas Müller']");
        MCRNameMerger b = buildNameEntry("[mods:namePart='thomas Mueller']");
        assertTrue(a.isProbablySameAs(b));

        MCRNameMerger c = buildNameEntry("[mods:namePart='Muller, T.']");
        assertTrue(a.isProbablySameAs(c));

        MCRNameMerger d = buildNameEntry("[mods:namePart='Mueller, T']");
        assertTrue(a.isProbablySameAs(d));

        MCRNameMerger e = buildNameEntry("[mods:namePart='Müller, Egon']");
        assertFalse(a.isProbablySameAs(e));

        MCRNameMerger f = buildNameEntry("[mods:namePart='Thorsten Mueller']");
        assertTrue(c.isProbablySameAs(f));
        assertFalse(a.isProbablySameAs(f));

        MCRNameMerger g = buildNameEntry("[mods:namePart='Thorsten Egon Mueller']");
        assertTrue(e.isProbablySameAs(g));
        assertTrue(f.isProbablySameAs(g));

        MCRNameMerger h = buildNameEntry(
            "[mods:namePart[@type='given']='Thomas'][mods:namePart[@type='family']='Müller']");
        assertTrue(h.isProbablySameAs(a));
        assertTrue(h.isProbablySameAs(d));

        MCRNameMerger i = buildNameEntry("[mods:namePart[@type='given']='T.'][mods:namePart[@type='family']='Müller']"
            + "[mods:namePart[@type='termsOfAddress']='Jun.']");
        assertTrue(i.isProbablySameAs(h));
        assertTrue(i.isProbablySameAs(a));
        assertTrue(i.isProbablySameAs(d));

        MCRNameMerger j = buildNameEntry("[mods:namePart='Thorsten Müller-Doppelname']");
        assertFalse(j.isProbablySameAs(a));
        assertFalse(j.isProbablySameAs(g));
        assertFalse(j.isProbablySameAs(i));
        assertTrue(j.isProbablySameAs(f));

        MCRNameMerger k = buildNameEntry("[mods:namePart='Thomas Müller-Doppelname']");
        assertTrue(k.isProbablySameAs(a));
        assertTrue(k.isProbablySameAs(b));
        assertTrue(k.isProbablySameAs(h));
        assertFalse(k.isProbablySameAs(c));
        assertFalse(k.isProbablySameAs(d));
        assertFalse(k.isProbablySameAs(i));

        try {
            new MCRNameMerger().setElement(null);
            fail("No name should result in NPE while creating a MCRNameMerger");
        } catch (NullPointerException ex) {
            // exception excepted
        }
    }

    @Test
    public void testEmptyGiven() throws JaxenException {
        Element a = new MCRNodeBuilder()
            .buildElement("mods:mods[mods:name[@type='personal'][mods:namePart[@type='given']]]", null, null);
        Element a2 = new MCRNodeBuilder()
            .buildElement("mods:mods[mods:name[@type='personal'][mods:namePart[@type='given']]]", null, null);
        Element b = new MCRNodeBuilder()
            .buildElement("mods:mods[mods:name[@type='personal'][mods:namePart[@type='given']='T.']]", null, null);
        MCRMergeTool.merge(a, b);
        assertEquals(2, a.getChildren("name", MCRConstants.MODS_NAMESPACE).size(),
                "Exactly two mods:name element expected");
        MCRMergeTool.merge(b, a2);
        assertEquals(2, b.getChildren("name", MCRConstants.MODS_NAMESPACE).size(),
                "Exactly two mods:name element expected");
    }

    @Test
    public void testCompareBasedOnDisplayForm() throws Exception {
        MCRNameMerger a = buildNameEntry("[mods:displayForm='Thomas Müller']");
        MCRNameMerger b = buildNameEntry(
            "[mods:namePart[@type='given']='Thomas'][mods:namePart[@type='family']='Müller']");
        assertTrue(a.isProbablySameAs(b));
    }

    @Test
    public void testMergeTermsOfAddress() throws JaxenException, IOException {
        String a = "[mods:name[@type='personal'][mods:namePart[@type='given']='Thomas']"
            + "[mods:namePart[@type='family']='Müller']]";
        String b = "[mods:name[@type='personal'][mods:namePart[@type='given']='T.']"
            + "[mods:namePart[@type='family']='Müller'][mods:namePart[@type='termsOfAddress']='Jun.']]";
        String e = "[mods:name[@type='personal'][mods:namePart[@type='given']='Thomas']"
            + "[mods:namePart[@type='family']='Müller'][mods:namePart[@type='termsOfAddress']='Jun.']]";
        MCRMergerTest.test(a, b, e);
    }

    @Test
    public void testMergeNameIdentifier() throws JaxenException, IOException {
        String a = "[mods:name[@type='personal'][mods:namePart='Thomas Müller']"
            + "[mods:nameIdentifier[@type='gnd']='2']]";
        String b = "[mods:name[@type='personal'][mods:namePart='Mueller, T']"
            + "[mods:nameIdentifier[@type='lsf']='1'][mods:nameIdentifier[@type='gnd']='2']]";
        String e = "[mods:name[@type='personal'][mods:namePart='Thomas Müller']"
            + "[mods:nameIdentifier[@type='gnd']='2'][mods:nameIdentifier[@type='lsf']='1']]";
        MCRMergerTest.test(a, b, e);
    }

    @Test
    public void testMergeDisplayForm() throws JaxenException, IOException {
        String a = "[mods:name[@type='personal'][mods:namePart='Thomas Müller'][mods:displayForm='Tommy']]";
        String b = "[mods:name[@type='personal'][mods:namePart='Mueller, T'][mods:displayForm='Tom']]";
        MCRMergerTest.test(a, b, a);
    }

    @Test
    public void testMergeSubElements() throws JaxenException, IOException {
        String a = "[mods:name[@type='personal'][mods:namePart='Thomas Müller'][mods:affiliation='UDE']]";
        String b =
            "[mods:name[@type='personal'][mods:namePart='Mueller, T'][mods:affiliation='UB der UDE'][mods:nameIdentifier[@type='gnd']='2']]";
        String e =
            "[mods:name[@type='personal'][mods:namePart='Thomas Müller'][mods:affiliation='UDE'][mods:affiliation='UB der UDE'][mods:nameIdentifier[@type='gnd']='2']]";
        MCRMergerTest.test(a, b, e);
    }

    @Test
    public void testMergeFirstLastVsLastFirst() throws JaxenException, IOException {
        String a = "[mods:name[@type='personal'][mods:namePart='Thomas Müller']]";
        String b = "[mods:name[@type='personal'][mods:namePart='Mueller, T']]";
        MCRMergerTest.test(a, b, a);
    }

    @Test
    public void testPreferFamilyGiven() throws JaxenException, IOException {
        String a = "[mods:name[@type='personal'][mods:namePart='Thomas Müller']]";
        String b =
            "[mods:name[@type='personal'][mods:namePart[@type='family']='Müller'][mods:namePart[@type='given']='T.']]";
        MCRMergerTest.test(a, b, b);
    }

    @Test
    public void testRetainSame() throws JaxenException, IOException {
        String a = "[mods:name[@type='personal'][mods:namePart='Thomas Müller']"
            + "[mods:nameIdentifier[@type='gnd']='1']][mods:name[@type='personal'][mods:namePart='Thomas Müller']"
            + "[mods:nameIdentifier[@type='gnd']='2']]";
        String b = "[mods:name[@type='personal'][mods:namePart='Thomas Müller']"
            + "[mods:nameIdentifier[@type='scopus']='1']][mods:name[@type='personal'][mods:namePart='Thomas Müller']"
            + "[mods:nameIdentifier[@type='scopus']='2']]";
        String e = "[mods:name[@type='personal'][mods:namePart='Thomas Müller'][mods:nameIdentifier[@type='gnd']='1']"
            + "[mods:nameIdentifier[@type='scopus']='1']][mods:name[@type='personal'][mods:namePart='Thomas Müller']"
            + "[mods:nameIdentifier[@type='gnd']='2'][mods:nameIdentifier[@type='scopus']='2']]";
        MCRMergerTest.test(a, b, e);
    }

    @Test
    public void testDontMergeConflictingIDs() throws JaxenException, IOException {
        String a = "[mods:name[@type='personal'][mods:namePart='Thomas Müller']"
            + "[mods:nameIdentifier[@type='scopus']='1']]";
        String b = "[mods:name[@type='personal'][mods:namePart='Thomas Müller']"
            + "[mods:nameIdentifier[@type='scopus']='2']]";
        String e = "[mods:name[@type='personal'][mods:namePart='Thomas Müller']"
            + "[mods:nameIdentifier[@type='scopus']='1']][mods:name[@type='personal'][mods:namePart='Thomas Müller']"
            + "[mods:nameIdentifier[@type='scopus']='2']]";
        MCRMergerTest.test(a, b, e);
    }

    @Test
    public void testPrioritizeMergeNonConflictingIDs() throws JaxenException, IOException {
        String a = "[mods:name[@type='personal'][mods:namePart='Thomas Müller'][mods:nameIdentifier[@type='gnd']='1']"
            + "[mods:nameIdentifier[@type='scopus']='1']]";
        String b = "[mods:name[@type='personal'][mods:namePart='Thomas Müller'][mods:nameIdentifier[@type='gnd']='1']"
            + "[mods:nameIdentifier[@type='scopus']='2']]";
        String e = "[mods:name[@type='personal'][mods:namePart='Thomas Müller'][mods:nameIdentifier[@type='gnd']='1']"
            + "[mods:nameIdentifier[@type='scopus']='1'][mods:nameIdentifier[@type='scopus']='2']]";
        MCRMergerTest.test(a, b, e);
    }

    @Test
    public void testHasAlternativeNameSameAs() throws JaxenException {
        Element modsNameElement = new MCRNodeBuilder()
            .buildElement("mods:name[@type='personal'][mods:namePart[@type='given']='Thomas']"
                + "[mods:namePart[@type='family']='Müller']", null, null);

        Element altNameElement = buildAlternativeNameElement("Thomas", "Meyer");
        modsNameElement.addContent(altNameElement);

        MCRNameMerger a = new MCRNameMerger();
        a.setElement(modsNameElement);
        MCRNameMerger b = buildNameEntry(
            "[mods:namePart[@type='given']='Thomas'][mods:namePart[@type='family']='Meyer']");
        MCRNameMerger c = buildNameEntry(
            "[mods:namePart[@type='given']='T'][mods:namePart[@type='family']='Meyer']");
        MCRNameMerger d = buildNameEntry(
            "[mods:namePart[@type='given']='Thomas'][mods:namePart[@type='family']='Mayer']");

        assertTrue(a.hasAlternativeNameSameAs(b));
        assertTrue(a.hasAlternativeNameSameAs(c));
        assertFalse(a.hasAlternativeNameSameAs(d));
        assertFalse(b.hasAlternativeNameSameAs(c));
    }

    @Test
    public void testMergeAsAlternativeName() throws JaxenException {
        MCRNameMerger a = buildNameEntry(
            "[mods:namePart[@type='given']='Thomas'][mods:namePart[@type='family']='Müller']");
        MCRNameMerger b = buildNameEntry(
            "[mods:namePart[@type='given']='Thomas'][mods:namePart[@type='family']='Meyer']");

        a.mergeAsAlternativeName(b);

        Element mergedModsName = a.element;

        List<Element> nameParts = mergedModsName.getChildren("namePart", MCRConstants.MODS_NAMESPACE);
        assertEquals(2, nameParts.size());

        assertEquals("Thomas", nameParts.getFirst().getText());
        assertEquals("Müller", nameParts.get(1).getText());

        List<Element> alternativeNames = mergedModsName.getChildren("alternativeName", MCRConstants.MODS_NAMESPACE);
        assertEquals(1, alternativeNames.size());
        Element alternativeName = alternativeNames.getFirst();
        nameParts = alternativeName.getChildren("namePart", MCRConstants.MODS_NAMESPACE);
        assertEquals(2, nameParts.size());

        assertEquals("Thomas", nameParts.getFirst().getText());
        assertEquals("Meyer", nameParts.get(1).getText());
    }

    private MCRNameMerger buildNameEntry(String predicates) throws JaxenException {
        Element modsName = new MCRNodeBuilder().buildElement("mods:name[@type='personal']" + predicates, null, null);
        MCRNameMerger ne = new MCRNameMerger();
        ne.setElement(modsName);
        return ne;
    }

    private Element buildAlternativeNameElement(String givenName, String familyName) {
        Element altNameElement = new Element("alternativeName", MCRConstants.MODS_NAMESPACE);

        Element altFamilyNameElement = new Element("namePart", MCRConstants.MODS_NAMESPACE);
        altFamilyNameElement.setAttribute("type", "family");
        altFamilyNameElement.setText(familyName);

        Element altGivenNameElement = new Element("namePart", MCRConstants.MODS_NAMESPACE);
        altGivenNameElement.setAttribute("type", "given");
        altGivenNameElement.setText(givenName);

        altNameElement.setContent(Arrays.asList(altFamilyNameElement, altGivenNameElement));

        return altNameElement;
    }
}
