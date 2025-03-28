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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRNodeBuilder;

public class MCRNameMergerTest extends MCRTestCase {

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

        try {
            new MCRNameMerger().setElement(null);
            Assert.fail("No name should result in NPE while creating a MCRNameMerger");
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
        assertEquals("Exactly two mods:name element expected", 2,
            a.getChildren("name", MCRConstants.MODS_NAMESPACE).size());
        MCRMergeTool.merge(b, a2);
        assertEquals("Exactly two mods:name element expected", 2,
            b.getChildren("name", MCRConstants.MODS_NAMESPACE).size());
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

    private MCRNameMerger buildNameEntry(String predicates) throws JaxenException {
        Element modsName = new MCRNodeBuilder().buildElement("mods:name[@type='personal']" + predicates, null, null);
        MCRNameMerger ne = new MCRNameMerger();
        ne.setElement(modsName);
        return ne;
    }
}
