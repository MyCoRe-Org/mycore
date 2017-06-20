package org.mycore.services.packaging;

import java.util.Hashtable;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.services.queuedjob.MCRJob;

public class MCRPackerManagerTest extends MCRJPATestCase {

    public static final String TEST_PACKER_ID = "testPacker";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected Map<String, String> getTestProperties() {
        // Set up example configuration
        Map<String, String> testProperties = super.getTestProperties();

        String testPackerPrefix = MCRPacker.PACKER_CONFIGURATION_PREFIX + TEST_PACKER_ID + ".";

        testProperties.put(testPackerPrefix + "Class", MCRPackerMock.class.getName());
        testProperties.put(testPackerPrefix + MCRPackerMock.TEST_CONFIGURATION_KEY, MCRPackerMock.TEST_VALUE);

        return testProperties;
    }

    @Test
    public void testPackerConfigurationStart() throws Exception {
        MCRConfiguration mcrConfiguration = MCRConfiguration.instance();

        String testPackerPrefix = MCRPacker.PACKER_CONFIGURATION_PREFIX + TEST_PACKER_ID + ".";

        Map<String, String> parameterMap = new Hashtable<>();

        // add packer parameter
        parameterMap.put("packer", TEST_PACKER_ID);

        // add test parameter
        parameterMap.put(MCRPackerMock.TEST_PARAMETER_KEY, MCRPackerMock.TEST_VALUE);

        MCRJob packerJob = MCRPackerManager.startPacking(parameterMap);

        Assert.assertNotNull("The Packer job is not present!", packerJob);

        endTransaction();
        Thread.sleep(1000);
        startNewTransaction();

        int waitTime = 10;
        while (waitTime > 0 && !mcrConfiguration.getBoolean(MCRPackerMock.FINISHED_PROPERTY, false)
            && !mcrConfiguration.getBoolean(MCRPackerMock.SETUP_CHECKED_PROPERTY, false)) {
            Thread.sleep(1000);
            waitTime -= 1;
        }
        Assert.assertTrue("PackerJob did not finish in time!", waitTime > 0);
    }

}
