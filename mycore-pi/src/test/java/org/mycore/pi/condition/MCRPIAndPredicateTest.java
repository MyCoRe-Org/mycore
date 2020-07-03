package org.mycore.pi.condition;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIJobService;

public class MCRPIAndPredicateTest extends MCRTestCase {

    private static final String MOCK_SERVICE_1 = "MOCK1";

    private static final String MOCK_SERVICE_2 = "MOCK2";

    private static final String MOCK_SERVICE_3 = "MOCK3";

    private static final String MOCK_SERVICE_4 = "MOCK4";

    @Test
    public void test() {
        final MCRBase emptyBase = new MCRBase() {
            @Override
            protected String getRootTagName() {
                return "null";
            }
        };
        final boolean mock1Result = MCRPIJobService
            .getPredicateInstance("MCR.PI.Service." + MOCK_SERVICE_1 + ".CreationPredicate").test(emptyBase);
        final boolean mock2Result = MCRPIJobService
            .getPredicateInstance("MCR.PI.Service." + MOCK_SERVICE_2 + ".CreationPredicate").test(emptyBase);
        final boolean mock3Result = MCRPIJobService
            .getPredicateInstance("MCR.PI.Service." + MOCK_SERVICE_3 + ".CreationPredicate").test(emptyBase);
        final boolean mock4Result = MCRPIJobService
            .getPredicateInstance("MCR.PI.Service." + MOCK_SERVICE_4 + ".CreationPredicate").test(emptyBase);

        Assert.assertTrue(mock1Result);
        Assert.assertFalse(mock2Result);
        Assert.assertTrue(mock3Result);
        Assert.assertFalse(mock4Result);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        final Map<String, String> testProperties = super.getTestProperties();

        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_1 + ".CreationPredicate", MCRPIAndPredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_1 + ".CreationPredicate.1", MCRTruePredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_1 + ".CreationPredicate.2", MCRTruePredicate.class.getName());

        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_2 + ".CreationPredicate", MCRPIAndPredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_2 + ".CreationPredicate.1", MCRTruePredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_2 + ".CreationPredicate.2", MCRFalsePredicate.class.getName());

        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_3 + ".CreationPredicate", MCRPIAndPredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_3 + ".CreationPredicate.1", MCRPIAndPredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_3 + ".CreationPredicate.1.1", MCRTruePredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_3 + ".CreationPredicate.1.2", MCRTruePredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_3 + ".CreationPredicate.2", MCRTruePredicate.class.getName());

        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_4 + ".CreationPredicate", MCRPIAndPredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_4 + ".CreationPredicate.1", MCRPIAndPredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_4 + ".CreationPredicate.1.1", MCRFalsePredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_4 + ".CreationPredicate.1.2", MCRTruePredicate.class.getName());
        testProperties
            .put("MCR.PI.Service." + MOCK_SERVICE_4 + ".CreationPredicate.2", MCRTruePredicate.class.getName());

        return testProperties;
    }
}
