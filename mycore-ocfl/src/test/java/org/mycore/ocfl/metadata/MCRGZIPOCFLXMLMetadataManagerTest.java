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

package org.mycore.ocfl.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;
import org.mycore.test.MyCoReTest;

import io.ocfl.api.model.ObjectVersionId;

@MyCoReTest
@ExtendWith({ MCRPermutationExtension.class, MCROCFLSetupExtension.class })
@MCROCFLSetupExtension.LoadDefaultDerivate(false)
@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.Metadata.Type.object", string = "true")
    })
public class MCRGZIPOCFLXMLMetadataManagerTest {

    private static final String OBJECT_ID = "junit_object_00000001";

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    /**
     * The content is stored compressed, but read uncompressed, from the current and from a specific version.
     */
    @TestTemplate
    public void retrieveContent() throws Exception {
        MCRGZIPOCFLXMLMetadataManager metadataManager = new MCRGZIPOCFLXMLMetadataManager();
        metadataManager.setRepositoryKey(repository.getId());
        MCRObject object = MCROCFLTestCaseHelper.createObject(OBJECT_ID);
        metadataManager.create(object.getId(), new MCRJDOMContent(object.createXML()), new Date());

        assertNotNull(repository.getObject(ObjectVersionId.head(MCROCFLObjectIDPrefixHelper.MCROBJECT + OBJECT_ID))
            .getFile("metadata/" + OBJECT_ID + ".xml.gz"), "the content should be stored compressed");
        assertEquals(OBJECT_ID, metadataManager.retrieveContent(object.getId()).asXML().getRootElement()
            .getAttributeValue("ID"), "the current version should be read uncompressed");
        assertEquals(OBJECT_ID, metadataManager.retrieveContent(object.getId(), "v1").asXML().getRootElement()
            .getAttributeValue("ID"), "a specific version should be read uncompressed");
    }

}
