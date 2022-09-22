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
        String debugFile = "testBasicEnrichment-debug.xml";
        assertTrue(test(enricherID, xPath, resultFile, debugFile));
    }

    @Test
    public void testMergePriority() throws JaxenException, IOException {
        String enricherID = "MergePriorityTest";

        String xPath = "mods:mods[mods:identifier[@type='foo']='1']";
        String resultFile = "testMergePriority-result1.xml";
        String debugFile = "testMergePriority-debug1.xml";
        assertTrue(test(enricherID, xPath, resultFile, debugFile));

        xPath = "mods:mods[mods:identifier[@type='foo']='2']";
        resultFile = "testMergePriority-result2.xml";
        debugFile = "testMergePriority-debug2.xml";
        assertTrue(test(enricherID, xPath, resultFile, debugFile));
    }

    @Test
    public void testResolvingIteration() throws JaxenException, IOException {
        String enricherID = "ResolvingIterationTest";
        String xPath = "mods:mods[mods:identifier[@type='issn']='1521-3765']";
        String resultFile = "testResolvingIteration-result.xml";
        String debugFile = "testResolvingIteration-debug.xml";
        assertTrue(test(enricherID, xPath, resultFile, debugFile));
    }

    @Ignore
    public boolean test(String enricherID, String xPath, String resultFile, String debugFile)
        throws JaxenException, IOException {
        Element publication = new MCRNodeBuilder().buildElement(xPath, null, null);
        MCREnricher enricher = new MCREnricher(enricherID);
        MCRToXMLEnrichmentDebugger debugger = new MCRToXMLEnrichmentDebugger();
        enricher.setDebugger(debugger);
        enricher.enrich(publication);

        if (debugFile != null) {
            boolean debugResult = checkXMLResult(debugFile, debugger.getDebugXML());
            assertTrue(debugResult);
        } else {
            logXML(debugger.getDebugXML());
        }

        return checkXMLResult(resultFile, publication);
    }

    private boolean checkXMLResult(String resultFile, Element result) throws IOException {
        String uri = "resource:MCREnrichmentTest/" + resultFile;
        Element expected = MCRURIResolver.instance().resolve(uri);

        boolean asExpected = MCRXMLHelper.deepEqual(result, expected);

        if (!asExpected) {
            System.out.println("actual result:");
            logXML(result);
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
