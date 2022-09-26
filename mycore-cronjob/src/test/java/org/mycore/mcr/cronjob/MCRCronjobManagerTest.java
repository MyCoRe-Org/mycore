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

package org.mycore.mcr.cronjob;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestCase;
import org.mycore.common.processing.impl.MCRCentralProcessableRegistry;

public class MCRCronjobManagerTest extends MCRTestCase {

    @Test
    public void startUp() {
        Assert.assertEquals("Count should be 0", MCRTestCronJob.count, 0);
        MCRCronjobManager.getInstance().startUp();
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            throw new MCRException(e);
        }
        Assert.assertEquals("Count should be 2", MCRTestCronJob.count, 2);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();

        testProperties.put("MCR.Processable.Registry.Class", MCRCentralProcessableRegistry.class.getName());
        testProperties.put(MCRCronjobManager.JOBS_CONFIG_PREFIX + "Test2", MCRTestCronJob.class.getName());
        testProperties.put(MCRCronjobManager.JOBS_CONFIG_PREFIX + "Test2.CronType", "QUARTZ");
        testProperties.put(MCRCronjobManager.JOBS_CONFIG_PREFIX + "Test2.Cron", "* * * * * ? *");
        testProperties.put(MCRCronjobManager.JOBS_CONFIG_PREFIX + "Test2.N", "1");

        return testProperties;
    }
}
