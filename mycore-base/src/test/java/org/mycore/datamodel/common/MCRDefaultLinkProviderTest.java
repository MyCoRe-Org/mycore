package org.mycore.datamodel.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.naming.OperationNotSupportedException;

import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.resource.MCRResourceHelper;

public class MCRDefaultLinkProviderTest extends MCRStoreTestCase {

    MCRDefaultLinkProvider provider;
    private MCRObject object;
    private MCRDerivate derivate;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        provider = new MCRDefaultLinkProvider();
        setUpTestObject();
        setUpTestDerivate();
    }

    private void setUpTestDerivate() throws IOException, JDOMException {
        try (InputStream resourceAsStream =
                     MCRResourceHelper.getResourceAsStream("MCRDefaultLinkProviderTest/HisBest_derivate_00035540.xml")) {
            byte[] bytes = resourceAsStream.readAllBytes();
            derivate = new MCRDerivate(bytes, false);
        }
    }

    private void setUpTestObject() throws IOException, JDOMException {
        try (InputStream resourceAsStream =
            MCRResourceHelper.getResourceAsStream("MCRDefaultLinkProviderTest/HisBest_cbu_00149763.xml")) {
            byte[] bytes = resourceAsStream.readAllBytes();
            object = new MCRObject(bytes, false);
        }
    }

    @Test
    public void getCategoriesOfObject() throws OperationNotSupportedException {
        Collection<MCRCategoryID> categoriesOfObject = provider.getCategoriesOfObject(object);

        Set<MCRCategoryID> expectedCategories =
            Stream.of("genre:letter", "ArchFile_class_001:eb78cfc6-eca3-47a6-baae-205f0943448d", "state:published")
                .map(MCRCategoryID::fromString)
                .collect(Collectors.toSet());

        for (MCRCategoryID expectedCategory : expectedCategories) {
            assertTrue("Expected category not found: " + expectedCategory,
                categoriesOfObject.contains(expectedCategory));
        }

        assertEquals("Unexpected category found", categoriesOfObject.size(), expectedCategories.size());
    }

    @Test
    public void getCategoriesOfDerivate() throws OperationNotSupportedException {
        Collection<MCRCategoryID> categoriesOfDerivate = provider.getCategoriesOfDerivate(derivate);

        Set<MCRCategoryID> expectedCategories =
                Stream.of("derivate_types:content", "state:published")
                        .map(MCRCategoryID::fromString)
                        .collect(Collectors.toSet());

        assertEquals("Unexpected category found", categoriesOfDerivate.size(), expectedCategories.size());
    }

    @Test
    public void getLinksOfObject() throws OperationNotSupportedException {
        Collection<MCRLinkTableManager.MCRLinkReference> linksOfObject = provider.getLinksOfObject(object);

        boolean containsParent = linksOfObject.stream().anyMatch(lo -> lo.from().equals(object.getId())
            && lo.to().equals(MCRObjectID.getInstance("HisBest_cbu_00116987")) && lo.type().equals(MCRLinkType.PARENT));

        boolean containsCorporation = linksOfObject.stream().anyMatch(lo -> lo.from().equals(object.getId())
            && lo.to().equals(MCRObjectID.getInstance("HisBest_corporation_00000415")) && lo.type().equals(MCRLinkType.REFERENCE));

        assertTrue("Expected link not found: Parent", containsParent);
        assertTrue("Expected link not found: Corporation", containsCorporation);
        assertEquals("Unexpected number of links", 2, linksOfObject.size());

    }

    @Test
    public void getLinksOfDerivate() throws OperationNotSupportedException {
        Collection<MCRLinkTableManager.MCRLinkReference> linksOfDerivate = provider.getLinksOfDerivate(derivate);

        boolean containsDerivateLink = linksOfDerivate.stream().anyMatch(lo -> lo.from().equals(derivate.getId())
            && lo.to().equals(MCRObjectID.getInstance("HisBest_cbu_00149763")) && lo.type().equals(MCRLinkType.DERIVATE));

        assertTrue("Expected link not found: Parent", containsDerivateLink);
        assertEquals("Unexpected number of links", 1, linksOfDerivate.size());
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();

        testProperties.put("MCR.Metadata.Type.cbu", "true");
        testProperties.put("MCR.Metadata.Type.corporation", "true");

        return testProperties;
    }
}
