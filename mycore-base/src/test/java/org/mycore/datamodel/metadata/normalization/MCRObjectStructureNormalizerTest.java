package org.mycore.datamodel.metadata.normalization;

import org.jdom2.Element;
import org.junit.Assert;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCREditableMetaEnrichedLinkID;
import org.mycore.datamodel.metadata.MCRExpandedObject;
import org.mycore.datamodel.metadata.MCRMetaEnrichedLinkIDFactory;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRObjectStructureNormalizerTest extends MCRTestCase {


    // TODO: test parent still exists

    @org.junit.Test
    public void testNormalize() {
        MCRExpandedObject object = new MCRExpandedObject();

        MCRObjectID childID1 = MCRObjectID.getInstance("junit_test_00000002");
        MCRObjectID childID2 = MCRObjectID.getInstance("junit_test_00000003");

        object.setId(MCRObjectID.getInstance("junit_test_00000001"));

        object.setSchema("noSchema");

        MCREditableMetaEnrichedLinkID emptyLinkID = MCRMetaEnrichedLinkIDFactory.obtainInstance().getEmptyLinkID();
        emptyLinkID.setMainDoc("Test.png");
        emptyLinkID.setOrder(1);
        emptyLinkID.setSubTag("derobject");
        emptyLinkID.setReference("junit_derivate_00000001", "junit_derivate_00000001", "title");
        object.getStructure().addDerivate(emptyLinkID);

        object.getStructure().addChild(new MCRMetaLinkID("child", childID1, null, childID1.toString()));
        object.getStructure().addChild(new MCRMetaLinkID("child", childID2, null, childID2.toString()));

        Assert.assertEquals(1, derivateCount(object));
        Assert.assertEquals(2, getChildCount(object));

        MCRObjectStructureNormalizer normalizer = new MCRObjectStructureNormalizer();
        normalizer.normalize(object);

        Assert.assertEquals(0, derivateCount(object));
        Assert.assertEquals(0, getChildCount(object));

    }

    public long derivateCount(MCRObject object) {
        Element structureElement = object.createXML().getRootElement().getChild("structure");
        if (structureElement == null) {
            return 0;
        }

        Element derobjects = structureElement.getChild("derobjects");
        if (derobjects == null) {
            return 0;
        }

        return derobjects.getChildren("derobject").size();
    }

    public long getChildCount(MCRObject object) {
        Element structureElement = object.createXML().getRootElement().getChild("structure");
        if (structureElement == null) {
            return 0;
        }

        Element children = structureElement.getChild("children");
        if (children == null) {
            return 0;
        }

        return children.getChildren("child").size();
    }

}
