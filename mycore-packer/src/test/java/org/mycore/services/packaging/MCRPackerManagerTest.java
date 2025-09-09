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

package org.mycore.services.packaging;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Hashtable;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.processing.impl.MCRCentralProcessableRegistry;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRJPATestHelper;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = MCRPacker.PACKER_CONFIGURATION_PREFIX + MCRPackerManagerTest.TEST_PACKER_ID + ".Class",
        classNameOf = MCRPackerMock.class),
    @MCRTestProperty(key = MCRPacker.PACKER_CONFIGURATION_PREFIX + MCRPackerManagerTest.TEST_PACKER_ID + "."
        + MCRPackerMock.TEST_CONFIGURATION_KEY, string = MCRPackerMock.TEST_VALUE),
    @MCRTestProperty(key = "MCR.QueuedJob.activated", string = "true"),
    @MCRTestProperty(key = "MCR.QueuedJob.JobThreads", string = "2"),
    @MCRTestProperty(key = "MCR.QueuedJob.TimeTillReset", string = "10"),
    @MCRTestProperty(key = "MCR.Processable.Registry.Class", classNameOf = MCRCentralProcessableRegistry.class)
})
public class MCRPackerManagerTest {

    public static final String TEST_PACKER_ID = "testPacker";

    @Test
    public void testPackerConfigurationStart() throws Exception {
        Map<String, String> parameterMap = new Hashtable<>();

        // add packer parameter
        parameterMap.put("packer", TEST_PACKER_ID);

        // add test parameter
        parameterMap.put(MCRPackerMock.TEST_PARAMETER_KEY, MCRPackerMock.TEST_VALUE);

        MCRJob packerJob = MCRPackerManager.startPacking(parameterMap);

        assertNotNull(packerJob, "The Packer job is not present!");

        MCRJPATestHelper.endTransaction();
        Thread.sleep(1000);
        MCRJPATestHelper.startNewTransaction();

        int waitTime = 10;
        while (waitTime > 0 && !MCRConfiguration2.getBoolean(MCRPackerMock.FINISHED_PROPERTY).orElse(false)
            && !MCRConfiguration2.getBoolean(MCRPackerMock.SETUP_CHECKED_PROPERTY).orElse(false)) {
            Thread.sleep(1000);
            waitTime -= 1;
        }
        assertTrue(waitTime > 0, "PackerJob did not finish in time!");
    }

}
