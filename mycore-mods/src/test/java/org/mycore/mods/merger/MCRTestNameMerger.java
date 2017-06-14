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

import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRNodeBuilder;

import static org.junit.Assert.*;

import java.io.IOException;

public class MCRTestNameMerger extends MCRTestCase {

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
        MCRTestMerger.test(a, b, e);
    }

    @Test
    public void testMergeNameIdentifier() throws JaxenException, IOException {
        String a = "[mods:name[@type='personal'][mods:namePart='Thomas Müller']"
                + "[mods:nameIdentifier[@type='gnd']='2']]";
        String b = "[mods:name[@type='personal'][mods:namePart='Mueller, T']"
                + "[mods:nameIdentifier[@type='lsf']='1'][mods:nameIdentifier[@type='gnd']='2']]";
        String e = "[mods:name[@type='personal'][mods:namePart='Thomas Müller']"
                + "[mods:nameIdentifier[@type='gnd']='2'][mods:nameIdentifier[@type='lsf']='1']]";
        MCRTestMerger.test(a, b, e);
    }

    @Test
    public void testMergeDisplayForm() throws JaxenException, IOException {
        String a = "[mods:name[@type='personal'][mods:namePart='Thomas Müller'][mods:displayForm='Tommy']]";
        String b = "[mods:name[@type='personal'][mods:namePart='Mueller, T'][mods:displayForm='Tom']]";
        String e = a;
        MCRTestMerger.test(a, b, e);
    }

    @Test
    public void testMergeSubElements() throws JaxenException, IOException {
        String a = "[mods:name[@type='personal'][mods:namePart='Thomas Müller'][mods:affiliation='UDE']]";
        String b = "[mods:name[@type='personal'][mods:namePart='Mueller, T'][mods:affiliation='UB der UDE'][mods:nameIdentifier[@type='gnd']='2']]";
        String e = "[mods:name[@type='personal'][mods:namePart='Thomas Müller'][mods:affiliation='UDE'][mods:affiliation='UB der UDE'][mods:nameIdentifier[@type='gnd']='2']]";
        MCRTestMerger.test(a, b, e);
    }

    @Test
    public void testMergeFirstLastVsLastFirst() throws JaxenException, IOException {
        String a = "[mods:name[@type='personal'][mods:namePart='Thomas Müller']]";
        String b = "[mods:name[@type='personal'][mods:namePart='Mueller, T']]";
        String e = a;
        MCRTestMerger.test(a, b, e);
    }

    @Test
    public void testPreferFamilyGiven() throws JaxenException, IOException {
        String a = "[mods:name[@type='personal'][mods:namePart='Thomas Müller']]";
        String b = "[mods:name[@type='personal'][mods:namePart[@type='family']='Müller'][mods:namePart[@type='given']='T.']]";
        String e = b;
        MCRTestMerger.test(a, b, e);
    }

    private MCRNameMerger buildNameEntry(String predicates) throws JaxenException {
        Element modsName = new MCRNodeBuilder().buildElement("mods:name[@type='personal']" + predicates, null, null);
        MCRNameMerger ne = new MCRNameMerger();
        ne.setElement(modsName);
        return ne;
    }
}
