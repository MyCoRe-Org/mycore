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
package org.mycore.validation.pdfa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.content.MCRDOMContent;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.ifs2.MCRStoreManager;
import org.mycore.datamodel.metadata.MCRObjectID;

@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.derivate", string = "true"),
    @MCRTestProperty(key = "MCR.PDFA.Report.SlotLayout", string = "4-2-2")
})
public class MCRPDFAReportStoreTest extends MCRTestCase {

    private static final String DERIVATE = "mir_derivate_00000123";

    @After
    public void removeStores() {
        MCRStoreManager.removeStore("PDFA_mir_derivate");
        MCRStoreManager.removeStore("PDFA_foo_derivate");
    }

    @Test
    public void reportIsNamedAfterItsPdfFile() {
        MCRPDFAReportStore store = obtainStore();
        Path directory = store.getReportDirectory(derivateId());
        assertEquals("chapter.pdf.xml",
            directory.relativize(store.getReportFile(derivateId(), "chapter.pdf")).toString());
        assertEquals(Path.of("sub", "chapter.pdf.xml"),
            directory.relativize(store.getReportFile(derivateId(), "sub/chapter.pdf")));
        assertEquals(Path.of("other", "chapter.pdf.xml"),
            directory.relativize(store.getReportFile(derivateId(), "/other/chapter.pdf")));
        assertEquals("mcrdata.pdf.xml",
            directory.relativize(store.getReportFile(derivateId(), "mcrdata.pdf")).toString());
    }

    @Test
    public void caseVariantsOfThePdfSuffixGetTheirOwnReport() {
        MCRPDFAReportStore store = obtainStore();
        assertNotEquals(store.getReportFile(derivateId(), "chapter.pdf"),
            store.getReportFile(derivateId(), "chapter.PDF"));
    }

    @Test
    public void reportDirectoryUsesTheSlotLayout() {
        Path directory = obtainStore().getReportDirectory(derivateId());
        assertEquals(Path.of("0000", "01", "mir_derivate_00000123"),
            obtainStore().getBaseDirectory().relativize(directory));
        assertEquals(Path.of("mir", "derivate"),
            Path.of(MCRConfiguration2.getStringOrThrow("MCR.PDFA.Report.BaseDir"))
                .relativize(obtainStore().getBaseDirectory()));
    }

    @Test
    public void storesOfDifferentProjectsAreSeparated() {
        MCRObjectID otherId = MCRObjectID.getInstance("foo_derivate_00000123");
        assertNotEquals(obtainStore().getReportFile(derivateId(), "chapter.pdf"),
            MCRPDFAReportStore.obtainInstance(otherId).getReportFile(otherId, "chapter.pdf"));
    }

    @Test
    public void reportIsWrittenEvenIfNoReportExistsYet() throws Exception {
        MCRPDFAReportStore store = obtainStore();
        Path target = store.getReportFile(derivateId(), "sub/chapter.pdf");
        store.writeReport(derivateId(), "sub/chapter.pdf", new MCRDOMContent(report()));
        assertTrue("report was not written to " + target, Files.isRegularFile(target));
        assertTrue(Files.readString(target).contains("flavour=\"1b\""));
    }

    @Test
    public void reportReplacesAnOlderReportOfTheSameFile() throws Exception {
        MCRPDFAReportStore store = obtainStore();
        Path target = store.getReportFile(derivateId(), "chapter.pdf");
        store.writeReport(derivateId(), "chapter.pdf", new MCRDOMContent(report()));
        store.writeReport(derivateId(), "chapter.pdf", new MCRDOMContent(report()));
        assertTrue(Files.isRegularFile(target));
        try (Stream<Path> files = Files.list(target.getParent())) {
            assertEquals("no temporary files may be left behind", List.of(target), files.toList());
        }
    }

    private static org.w3c.dom.Document report() throws Exception {
        Path pdf = Path.of(MCRPDFAReportStoreTest.class.getResource("/pdfA-1b.pdf").toURI());
        return MCRPDFAFunctions.getResult(pdf, "chapter.pdf");
    }

    @Test
    public void pathsOutsideOfTheDerivateAreRejected() {
        MCRPDFAReportStore store = obtainStore();
        assertThrows(IllegalArgumentException.class, () -> store.getReportFile(derivateId(), "../escaped.pdf"));
    }

    private static MCRObjectID derivateId() {
        return MCRObjectID.getInstance(DERIVATE);
    }

    private MCRPDFAReportStore obtainStore() {
        return MCRPDFAReportStore.obtainInstance(derivateId());
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> properties = super.getTestProperties();
        properties.put("MCR.PDFA.Report.BaseDir", junitFolder.getRoot().toPath().resolve("verapdf").toString());
        return properties;
    }
}
