package org.mycore.datamodel.metadata;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
class MCRMetadataManagerTest {

  /**
   * Test to ensure that the MCRMetadataManager does not modify the object passed in the Parameter
   * when updating. It should create a copy of the object and return the updated copy.
   */
  @Test
  @MCRTestConfiguration(
      properties = {
          @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
      }
  )
  void testObjectUpdateCreatesCopy() throws MCRAccessException {
    MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.SUPER_USER);
    MCRObjectID id = MCRObjectID.getInstance("junit_test_00000001");
    MCRObject testObject = new MCRObject();
    testObject.setId(id);
    testObject.setLabel("Label 1");
    testObject.setSchema("noSchema");

    MCRMetaLangText subElement = new MCRMetaLangText("title", "de", null
        , 0, null, "Test Title");
    MCRMetaElement metaElement = new MCRMetaElement(MCRMetaLangText.class, "title", false, true,
        List.of(subElement));
    testObject.getMetadata().setMetadataElement(metaElement);

    MCRMetadataManager.create(testObject);


    MCRObject retrievedObject = MCRMetadataManager.retrieveMCRObject(id);

    MCRMetaLangText retrievedTitle = (MCRMetaLangText) retrievedObject.getMetadata().getMetadataElement("title")
        .getElementByName("title");
    retrievedTitle.setText("Test Title (new)");

    MCRObject updatedObject = MCRMetadataManager.update(retrievedObject);

    Assertions.assertNotSame(retrievedObject, updatedObject);
  }

}
