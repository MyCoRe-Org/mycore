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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.processing.impl.MCRCentralProcessableRegistry;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.frontend.cli.MCRCommandManager;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Processable.Registry.Class", classNameOf = MCRCentralProcessableRegistry.class),
    @MCRTestProperty(key = MCRCronjobManager.JOBS_CONFIG_PREFIX + "Command", classNameOf = MCRCommandCronJob.class),
    @MCRTestProperty(key = MCRCronjobManager.JOBS_CONFIG_PREFIX + "Command.Contexts", string = "CLI"),
    @MCRTestProperty(key = MCRCronjobManager.JOBS_CONFIG_PREFIX + "Command.CronType", string = "QUARTZ"),
    @MCRTestProperty(key = MCRCronjobManager.JOBS_CONFIG_PREFIX + "Command.Cron", string = "* * * * * ? *"),
    @MCRTestProperty(key = MCRCronjobManager.JOBS_CONFIG_PREFIX + "Command.Command", string = "test command Hallo")
})
public class MCRCommandCronJobTest {

    public static boolean commandRun;

    public static String message;

    @Test
    public void runJob() throws InterruptedException {
        assertFalse(commandRun, "The command should not run yet!");
        MCRCommandManager.getKnownCommands().put("test",
            Stream
                .of(new MCRCommand("test command {0}",
                    "org.mycore.mcr.cronjob.MCRCommandCronJobTest.testCommand String", "just a test command."))
                .collect(Collectors.toList()));
        MCRCronjobManager.getInstance().startUp(null);
        Thread.sleep(2000);
        assertTrue(commandRun, "The command should have been executed!");
        assertNotNull(message, "The message should be set");
        assertEquals("Welt", message, "Message should be the same");
    }

    public static List<String> testCommand(String msg) {
        System.out.println(msg);
        commandRun = true;
        message = msg;

        if (message.equals("Welt")) {
            return null;
        } else {
            return Stream.of("test command Welt").collect(Collectors.toList());
        }
    }

}
