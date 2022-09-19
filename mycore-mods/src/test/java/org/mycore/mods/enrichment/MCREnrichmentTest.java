package org.mycore.mods.enrichment;

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
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;

public class MCREnrichmentTest extends MCRTestCase {

    @Test
    public void testBasicEnrichment() throws JaxenException, IOException {
        String xPath = "mods:mods[mods:identifier[@type='doi']='10.123/456']";
        Element publication = new MCRNodeBuilder().buildElement(xPath, null, null);
        new MCREnricher("Test").enrich(publication);

        String uri = "resource:MCREnrichmentTest/testBasicEnrichment-result.xml";
        Element expected = MCRURIResolver.instance().resolve(uri);

        boolean asExpected = MCRXMLHelper.deepEqual(publication, expected);

        if (!asExpected) {
            System.out.println("actual result:");
            logXML(publication);
            System.out.println("expected result:");
            logXML(expected);
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
