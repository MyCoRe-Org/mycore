package org.mycore.mods.merger;

import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.xml.MCRNodeBuilder;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MCRModspersonNameMergerTest {

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
