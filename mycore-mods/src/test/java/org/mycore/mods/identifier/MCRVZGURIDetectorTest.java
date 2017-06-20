package org.mycore.mods.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MCRVZGURIDetectorTest {

    private Hashtable<URI, String> testData;

    @Before
    public void prepareTest() throws URISyntaxException {
        testData = new Hashtable<>();

        testData.put(new URI("http://uri.gbv.de/document/gvk:ppn:834532662"), "gvk:ppn:834532662");
        testData.put(new URI("http://uri.gbv.de/document/opac-de-28:ppn:826913938"), "opac-de-28:ppn:826913938");
        testData.put(new URI("http://gso.gbv.de/DB=2.1/PPNSET?PPN=357619811"), "gvk:ppn:357619811");
    }

    @Test
    public void testDetect() throws Exception {
        MCRGBVURLDetector detector = new MCRGBVURLDetector();
        testData.entrySet().forEach(d -> {
            Optional<Map.Entry<String, String>> maybeDetectedGND = detector.detect(d.getKey());

            Assert.assertTrue("Should have a detected PPN", maybeDetectedGND.isPresent());

            maybeDetectedGND
                .ifPresent(gnd -> Assert.assertEquals("Should have detected the right type!", gnd.getKey(), "ppn"));
            maybeDetectedGND.ifPresent(
                gnd -> Assert.assertEquals("Should have detected the right value!", gnd.getValue(), d.getValue()));
        });
    }
}
