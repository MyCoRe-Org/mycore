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
 *
 *
 */

package org.mycore.mcr.cronjob;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.processing.impl.MCRCentralProcessableRegistry;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.frontend.cli.MCRCommandManager;

public class MCRCommandCronJobTest extends MCRJPATestCase {

    public static boolean commandRun = false;

    public static String message;

    @Test
    public void runJob() throws InterruptedException {
        Assert.assertFalse("The command should not run yet!", commandRun);
        MCRCommandManager.getKnownCommands().put("test",
            Stream
                .of(new MCRCommand("test command {0}",
                    "org.mycore.mcr.cronjob.MCRCommandCronJobTest.testCommand String", "just a test command."))
                .collect(Collectors.toList()));
        MCRCronjobManager.getInstance().startUp();
        Thread.sleep(2000);
        Assert.assertTrue("The command should have been executed!", commandRun);
        Assert.assertNotNull("The message should be set", message);
        Assert.assertEquals("Message should be the same", "Welt", message);
    }

    public static List<String> testCommand(String msg) {
        System.out.println(msg);
        commandRun = true;
        message = msg;

        if(message.equals("Welt")){
            return null;
        } else {
            return Stream.of("test command Welt").collect(Collectors.toList());
        }
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> properties = super.getTestProperties();

        properties.put("MCR.Processable.Registry.Class", MCRCentralProcessableRegistry.class.getName());
        properties.put(MCRCronjobManager.JOBS_CONFIG_PREFIX + "Command", MCRCommandCronJob.class.getName());
        properties.put(MCRCronjobManager.JOBS_CONFIG_PREFIX + "Command.Command", "test command Hallo");
        properties.put(MCRCronjobManager.JOBS_CONFIG_PREFIX + "Command.Cron", "* * * * * ? *");
        properties.put(MCRCronjobManager.JOBS_CONFIG_PREFIX + "Command.CronType", "QUARTZ");

        return properties;
    }
}
