package org.mycore.pi.condition;

import java.util.Map;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIJobService;

public class MCRPIXPathPredicateTest extends MCRTestCase {

    private static final String MOCK_SERVICE_1 = "MOCK1";

    private static final String MOCK_SERVICE_2 = "MOCK2";

    private static final String MOCK_SERVICE_3 = "MOCK3";

    @Test
    public void test1() {
        MCRObjectID testID = MCRObjectID.getInstance("my_test_00000001");
        MCRObject mcrObject = new MCRObject();
        mcrObject.setSchema("test");
        mcrObject.setId(testID);
        mcrObject.setId(testID);
        final Element metadata = new Element("metadata");
        final Element testElement = new Element("test1");
        metadata.addContent(testElement);
        testElement.setAttribute("class", "MCRMetaXML");
        testElement.addContent(new Element("test2").setText("result1"));
        testElement.addContent(new Element("test3").setText("result2"));
        mcrObject.getMetadata().setFromDOM(metadata);

        final boolean mock1Result = MCRPIJobService
            .getPredicateInstance("MCR.PI.Service." + MOCK_SERVICE_1 + ".CreationPredicate").test(mcrObject);
        final boolean mock2Result = MCRPIJobService
            .getPredicateInstance("MCR.PI.Service." + MOCK_SERVICE_2 + ".CreationPredicate").test(mcrObject);
        final boolean mock3Result = MCRPIJobService
            .getPredicateInstance("MCR.PI.Service." + MOCK_SERVICE_3 + ".CreationPredicate").test(mcrObject);

        Assert.assertTrue(mock1Result);
        Assert.assertFalse(mock2Result);
        Assert.assertTrue(mock3Result);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        final Map<String, String> testProperties = super.getTestProperties();

        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_1 + ".CreationPredicate", MCRPIXPathPredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_1 + ".CreationPredicate.XPath", "/mycoreobject/metadata/test1");

        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_2 + ".CreationPredicate", MCRPIXPathPredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_2 + ".CreationPredicate.XPath", "/mycoreobject/metadata/test2");

        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_3 + ".CreationPredicate", MCRPIXPathPredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_3 + ".CreationPredicate.XPath", "/mycoreobject/metadata/test1/test2");

        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());

        return testProperties;
    }
}
