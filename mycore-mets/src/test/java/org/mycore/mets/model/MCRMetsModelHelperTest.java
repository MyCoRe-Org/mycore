package org.mycore.mets.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mycore.common.MCRTestCase;

@RunWith(Parameterized.class)
public class MCRMetsModelHelperTest extends MCRTestCase {

    private final String path;

    private final Optional<String> expected;

    public MCRMetsModelHelperTest(String path, Optional<String> expected) {
        this.path = path;
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "bild.jpg", Optional.of(MCRMetsModelHelper.MASTER_USE) },
            { "alto/datei1.xml", Optional.of(MCRMetsModelHelper.ALTO_USE) },
            { "tei/transcription/page1.xml", Optional.of(MCRMetsModelHelper.TRANSCRIPTION_USE) },
            { "tei/transcription/page2.xml", Optional.of(MCRMetsModelHelper.TRANSCRIPTION_USE) },
            { "tei/translation/page1.xml", Optional.empty() },
            { "tei/text.xml", Optional.of(MCRMetsModelHelper.MASTER_USE) },
            { "tei/translation.de/page1.xml", Optional.of(MCRMetsModelHelper.TRANSLATION_USE + ".DE") },
            { "tei/translation.en/page2.xml", Optional.of(MCRMetsModelHelper.TRANSLATION_USE + ".EN") },
            { "tei/translation.kr/page2.xml", Optional.empty() },
        });
    }

    @Test
    public void getUseForHref() {
        Assert.assertEquals(expected, MCRMetsModelHelper.getUseForHref(path));
    }

    @Override protected Map<String, String> getTestProperties() {
        final Map<String, String> testProperties = super.getTestProperties();

        testProperties.put(MCRMetsModelHelper.ALLOWED_TRANSLATION_PROPERTY, "de,en");

        return testProperties;
    }
}