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

import java.util.List;

import org.mycore.common.MCRException;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.frontend.cli.MCRCommandManager;
import org.mycore.util.concurrent.MCRTransactionableRunnable;

public class MCRCommandCronJob extends MCRCronjob {

    private String command;

    public String getCommand() {
        return command;
    }

    @MCRProperty(name = "Command")
    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public void runJob() {
        new MCRTransactionableRunnable(() -> {
            String command = getCommand();
            MCRCommandManager cmdMgr = new MCRCommandManager();
            invokeCommand(command, cmdMgr);
        }).run();
    }

    private void invokeCommand(String command, MCRCommandManager cmdMgr) {
        try {
            List<String> result = cmdMgr.invokeCommand(command);
            if (result.size() > 0) {
                for (String s : result) {
                    invokeCommand(s, cmdMgr);
                }
            }
        } catch (Exception e) {
            throw new MCRException("Error while invoking command " + command, e);
        }
    }

    @Override
    public String getDescription() {
        return "Runs the command '" + getCommand() + "'";
    }
}
