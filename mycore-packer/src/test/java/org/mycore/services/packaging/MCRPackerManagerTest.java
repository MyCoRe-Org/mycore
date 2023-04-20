/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.services.packaging;

import java.util.Hashtable;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.processing.impl.MCRCentralProcessableRegistry;
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
        testProperties.put("MCR.QueuedJob.activated", "true");
        testProperties.put("MCR.QueuedJob.autostart", "true");
        testProperties.put("MCR.QueuedJob.JobThreads", "2");
        testProperties.put("MCR.QueuedJob.TimeTillReset", "10");
        testProperties.put("MCR.Processable.Registry.Class", MCRCentralProcessableRegistry.class.getName());
        return testProperties;
    }

    @Test
    public void testPackerConfigurationStart() throws Exception {
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
        while (waitTime > 0 && !MCRConfiguration2.getBoolean(MCRPackerMock.FINISHED_PROPERTY).orElse(false)
            && !MCRConfiguration2.getBoolean(MCRPackerMock.SETUP_CHECKED_PROPERTY).orElse(false)) {
            Thread.sleep(1000);
            waitTime -= 1;
        }
        Assert.assertTrue("PackerJob did not finish in time!", waitTime > 0);
    }

}
