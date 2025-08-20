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

package org.mycore.mcr.cronjob;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.processing.impl.MCRCentralProcessableRegistry;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Processable.Registry.Class", classNameOf = MCRCentralProcessableRegistry.class),
    @MCRTestProperty(key = MCRCronjobManager.JOBS_CONFIG_PREFIX + "Test2", classNameOf = MCRTestCronJob.class),
    @MCRTestProperty(key = MCRCronjobManager.JOBS_CONFIG_PREFIX + "Test2.Contexts", string = "CLI"),
    @MCRTestProperty(key = MCRCronjobManager.JOBS_CONFIG_PREFIX + "Test2.CronType", string = "QUARTZ"),
    @MCRTestProperty(key = MCRCronjobManager.JOBS_CONFIG_PREFIX + "Test2.Cron", string = "* * * * * ? *"),
    @MCRTestProperty(key = MCRCronjobManager.JOBS_CONFIG_PREFIX + "Test2.N", string = "1")
})
public class MCRCronjobManagerTest {

    @Test
    public void startUp() {
        assertEquals(0, MCRTestCronJob.count, "Count should be 0");
        MCRCronjobManager.getInstance().startUp(null);
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            throw new MCRException(e);
        }
        assertEquals(2, MCRTestCronJob.count, "Count should be 2");
    }

}
