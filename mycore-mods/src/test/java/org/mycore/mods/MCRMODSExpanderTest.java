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
package org.mycore.mods;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRExpandedObjectCache;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRXlink;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.metadata.MCRExpandedObject;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.classification.MCRClassMapper;
import org.mycore.mods.classification.mapping.MCRMODSGeneratorClassificationMapper;
import org.mycore.mods.classification.mapping.MCRMODSXPathClassificationGenerator;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
public class MCRMODSExpanderTest {

    private static final String TEST_DIRECTORY = MCRMODSExpanderTest.class.getSimpleName() + "/";

    private static final String CLASSIFICATION_ID = "hostGenreClassification";

    private static final MCRObjectID PARENT_ID = MCRObjectID.getInstance("junit_mods_00000001");

    @BeforeEach
    public void setUp() throws IOException, JDOMException, URISyntaxException {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.SUPER_USER);
        MCRCategory category = MCRXMLTransformer.getCategory(new SAXBuilder()
            .build(getClass().getClassLoader().getResourceAsStream(TEST_DIRECTORY + CLASSIFICATION_ID + ".xml")));
        MCRCategoryDAOFactory.obtainInstance().addCategory(null, category);
        // other tests may have cached an expanded object with the same id
        MCRExpandedObjectCache.getInstance().clear(PARENT_ID);
    }

    /**
     * Tests that XPath classification mappings are evaluated after the metadata of the parent object has been
     * inherited into the host related item, so that mappings depending on the host genre can match.
     */
    @Test
    public void testClassificationMappingConsidersInheritedHostMetadata() throws MCRAccessException {
        MCRObject parent = createModsObject(PARENT_ID, "journal");
        MCRMetadataManager.create(parent);

        MCRObjectID childId = MCRObjectID.getInstance("junit_mods_00000002");
        MCRObject child = createModsObject(childId, "article");
        child.getStructure().setParent(PARENT_ID);
        new MCRMODSWrapper(child).addElement(new Element("relatedItem", MCRConstants.MODS_NAMESPACE)
            .setAttribute("type", "host")
            .setAttribute(MCRXlink.HREF, PARENT_ID.toString(), MCRConstants.XLINK_NAMESPACE));

        MCRMODSGeneratorClassificationMapper mapper = new MCRMODSGeneratorClassificationMapper(
            Map.of("xPath", new MCRMODSXPathClassificationGenerator(CLASSIFICATION_ID)));
        MCRExpandedObject expanded = new MCRMODSExpander(mapper).expand(child);

        List<Element> mappings = new MCRMODSWrapper(expanded)
            .getElements("mods:classification[@generator='xpathmapping2" + CLASSIFICATION_ID + "-mycore']");
        assertEquals(1, mappings.size(), "Exactly one category should be mapped");
        assertEquals(new MCRCategoryID(CLASSIFICATION_ID, "journal-article"),
            MCRClassMapper.getCategoryID(mappings.getFirst()),
            "The mapping depending on the host genre should match instead of the fallback");
    }

    private static MCRObject createModsObject(MCRObjectID id, String genre) {
        MCRObject object = new MCRObject();
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        wrapper.setID(id.getProjectId(), id.getNumberAsInteger());
        wrapper.setMODS(new Element("mods", MCRConstants.MODS_NAMESPACE)
            .addContent(new Element("genre", MCRConstants.MODS_NAMESPACE).setText(genre))
            .addContent(new Element("titleInfo", MCRConstants.MODS_NAMESPACE)
                .addContent(new Element("title", MCRConstants.MODS_NAMESPACE).setText(genre + " title"))));
        return object;
    }

}
