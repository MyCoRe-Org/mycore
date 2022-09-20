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
        String enricherID = "BasicTest";
        String xPath = "mods:mods[mods:identifier[@type='doi']='10.123/456']";
        String resultFile = "testBasicEnrichment-result.xml";
        assertTrue(test(enricherID, xPath, resultFile));
    }

    @Test
    public void testMergePriority() throws JaxenException, IOException {
        String enricherID = "MergePriorityTest";

        String xPath = "mods:mods[mods:identifier[@type='foo']='1']";
        String resultFile = "testMergePriority-result1.xml";
        assertTrue(test(enricherID, xPath, resultFile));
        
        xPath = "mods:mods[mods:identifier[@type='foo']='2']";
        resultFile = "testMergePriority-result2.xml";
        assertTrue(test(enricherID, xPath, resultFile));
    }

    @Ignore
    public boolean test(String enricherID, String xPath, String resultFile) throws JaxenException, IOException {
        Element publication = new MCRNodeBuilder().buildElement(xPath, null, null);
        new MCREnricher(enricherID).enrich(publication);

        String uri = "resource:MCREnrichmentTest/" + resultFile;
        Element expected = MCRURIResolver.instance().resolve(uri);

        boolean asExpected = MCRXMLHelper.deepEqual(publication, expected);

        if (!asExpected) {
            System.out.println("actual result:");
            logXML(publication);
            System.out.println("expected result:");
            logXML(expected);
        }

        return asExpected;
    }

    @Ignore
    private static void logXML(Element r) throws IOException {
        System.out.println();
        new XMLOutputter(Format.getPrettyFormat()).output(r, System.out);
        System.out.println();
    }
}
