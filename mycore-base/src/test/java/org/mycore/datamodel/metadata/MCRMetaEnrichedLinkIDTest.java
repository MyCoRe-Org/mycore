package org.mycore.datamodel.metadata;

import java.util.List;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRMetaEnrichedLinkIDTest extends MCRTestCase {

    protected static final String TEST_ELEMENT_NAME = "atest";

    protected static final String TEST2_ELEMENT_NAME = "test";

    @Test
    public void testOrdering() {
        final MCREditableMetaEnrichedLinkID mcrMetaEnrichedLinkID = new MCREditableMetaEnrichedLinkID();

        mcrMetaEnrichedLinkID.setReference("mir_derivate_00000001", null, "");
        mcrMetaEnrichedLinkID.setSubTag("derobject");
        mcrMetaEnrichedLinkID.setMainDoc("main");
        mcrMetaEnrichedLinkID.setOrder(1);
        mcrMetaEnrichedLinkID.getContentList().add(new Element(TEST_ELEMENT_NAME));
        mcrMetaEnrichedLinkID.getContentList().add(new Element(TEST2_ELEMENT_NAME));

        final Element xml = mcrMetaEnrichedLinkID.createXML();

        final List<Element> children = xml.getChildren();

        Assert.assertEquals("First Element should be order", MCRMetaEnrichedLinkID.ORDER_ELEMENT_NAME,
            children.get(0).getName());
        Assert.assertEquals("Second Element should be maindoc", MCRMetaEnrichedLinkID.MAIN_DOC_ELEMENT_NAME,
            children.get(1).getName());
        Assert.assertEquals("Third Element should be " + TEST_ELEMENT_NAME, TEST_ELEMENT_NAME,
            children.get(2).getName());

    }

}