package org.mycore.datamodel.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.naming.OperationNotSupportedException;

import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.resource.MCRResourceHelper;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.cbu", string = "true"),
    @MCRTestProperty(key = "MCR.Metadata.Type.corporation", string = "true")
})
public class MCRDefaultLinkProviderTest {

    MCRDefaultLinkProvider provider;
    private MCRObject object;
    private MCRDerivate derivate;

    @BeforeEach
    public void setUp() throws Exception {
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
                .map(MCRCategoryID::ofString)
                .collect(Collectors.toSet());

        for (MCRCategoryID expectedCategory : expectedCategories) {
            assertTrue(categoriesOfObject.contains(expectedCategory),
                "Expected category not found: " + expectedCategory);
        }

        assertEquals(categoriesOfObject.size(), expectedCategories.size(), "Unexpected category found");
    }

    @Test
    public void getCategoriesOfDerivate() throws OperationNotSupportedException {
        Collection<MCRCategoryID> categoriesOfDerivate = provider.getCategoriesOfDerivate(derivate);

        Set<MCRCategoryID> expectedCategories =
            Stream.of("derivate_types:content", "state:published")
                .map(MCRCategoryID::ofString)
                .collect(Collectors.toSet());

        assertEquals(categoriesOfDerivate.size(), expectedCategories.size(), "Unexpected category found");
    }

    @Test
    public void getLinksOfObject() throws OperationNotSupportedException {
        Collection<MCRLinkTableManager.MCRLinkReference> linksOfObject = provider.getLinksOfObject(object);

        boolean containsParent = linksOfObject.stream().anyMatch(lo -> lo.from().equals(object.getId())
            && lo.to().equals(MCRObjectID.getInstance("HisBest_cbu_00116987")) && lo.type().equals(MCRLinkType.PARENT));

        boolean containsCorporation = linksOfObject.stream().anyMatch(lo -> lo.from().equals(object.getId())
            && lo.to().equals(MCRObjectID.getInstance("HisBest_corporation_00000415"))
            && lo.type().equals(MCRLinkType.REFERENCE));

        assertTrue(containsParent, "Expected link not found: Parent");
        assertTrue(containsCorporation, "Expected link not found: Corporation");
        assertEquals(2, linksOfObject.size(), "Unexpected number of links");

    }

    @Test
    public void getLinksOfDerivate() throws OperationNotSupportedException {
        Collection<MCRLinkTableManager.MCRLinkReference> linksOfDerivate = provider.getLinksOfDerivate(derivate);

        boolean containsDerivateLink = linksOfDerivate.stream().anyMatch(lo -> lo.from().equals(derivate.getId())
            && lo.to().equals(MCRObjectID.getInstance("HisBest_cbu_00149763"))
            && lo.type().equals(MCRLinkType.DERIVATE));

        assertTrue(containsDerivateLink, "Expected link not found: Parent");
        assertEquals(1, linksOfDerivate.size(), "Unexpected number of links");
    }

}
