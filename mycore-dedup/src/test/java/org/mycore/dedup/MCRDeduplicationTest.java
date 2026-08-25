package org.mycore.dedup;

import org.jdom2.Document;

import org.jdom2.input.SAXBuilder;
import org.junit.Assert;

import org.junit.Test;


import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MCRDeduplicationTest {

    private final Set<String> xpaths = Set.of(
            "mods:identifier[@type='doi']",
            "mods:title",
            "mods:namePart[@type='family']"
    );

    private final MCRDeDupCriteriaBuilder builder = new MCRDeDupCriteriaBuilder(xpaths);
    private final MCRDeDupService deDupService = new MCRDeDupService(builder);



    @Test
    public void shouldCalculateDeduplicationKeysCorrectly() throws Exception {
        List<Document> documents = loadTestDocuments();
        Map<MCRDeDupCriterion, Set<String>> actualResults = deDupService.calculateDeduplication(documents, "mods_00000002");
        assertKeyisDuplicate(actualResults, "mods_00000002", "modstitle", "69a5b16d7e7914c05ea2f87054009e02");
        assertKeyisDuplicate(actualResults, "mods_00000002", "modsnameparttypefamily", "7c0d78669fd11ea1f82632ec4f5b77da");
        assertKeyisDuplicate(actualResults, "mods_00000003", "modsnameparttypefamily", "7c0d78669fd11ea1f82632ec4f5b77da");
        assertKeyisDuplicate(actualResults, "mods_00000001", "modstitle", "69a5b16d7e7914c05ea2f87054009e02");
        assertKeyisDuplicate(actualResults, "mods_00000002", "modsidentifiertypedoi", "3173a13f487d5a0da7e695ed61eed463");
        assertKeyisDuplicate(actualResults, "mods_00000003", "modsidentifiertypedoi", "3173a13f487d5a0da7e695ed61eed463");
        assertKeyisDuplicate(actualResults, "mods_00000001", "modsidentifiertypedoi", "3173a13f487d5a0da7e695ed61eed463");
    }


    private void assertKeyisDuplicate(Map<MCRDeDupCriterion, Set<String>> dedupMap, String expectedDocID, String expectedType, String expectedKey) {
        boolean found = dedupMap.entrySet().stream()
                .anyMatch(entry ->
                        entry.getKey().getType().equals(expectedType) &&
                                entry.getKey().getKey().equals(expectedKey) &&
                                entry.getValue().contains(expectedDocID)
                );

        Assert.assertTrue("Expected deduplication key not found: docID=" +
                expectedDocID + ", type=" + expectedType + ", key=" + expectedKey, found);

    }

    private List<Document> loadTestDocuments() throws Exception {
        return List.of(
                loadDocument("src/test/resources/test.xml"),
                loadDocument("src/test/resources/test1.xml"),
                loadDocument("src/test/resources/test2.xml")
        );
    }

    private Document loadDocument(String path) throws Exception {
        return new SAXBuilder().build(new File(path));
    }
}
