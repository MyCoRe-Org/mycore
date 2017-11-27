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

package org.mycore.webcli.cli;

import java.io.IOException;
import java.util.List;

import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.cli.MCRCommandLineInterface;
import org.mycore.frontend.cli.MCRCommandStatistics;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.webcli.container.MCRWebCLIContainer;

@MCRCommandGroup(name = "Basic commands")
public class MCRBasicWebCLICommands {
    @MCRCommand(syntax = "process {0}", help = "Execute the commands listed in the text file {0}.")
    public static List<String> readCommandsFile(String file) throws IOException {
        return MCRCommandLineInterface.readCommandsFile(file);
    }

    @MCRCommand(syntax = "show command statistics",
        help = "Show statistics on number of commands processed and execution time needed per command")
    public static void showCommandStatistics() {
        MCRCommandStatistics.showCommandStatistics();
    }

    @MCRCommand(syntax = "cancel on error", help = "Cancel execution of further commands in case of error")
    public static void cancelonError() {
        setContinueIfOneFails(false);
        MCRCommandLineInterface.cancelOnError();
    }

    @MCRCommand(syntax = "skip on error", help = "Skip execution of failed command in case of error")
    public static void skipOnError() {
        setContinueIfOneFails(true);
        MCRCommandLineInterface.skipOnError();
    }

    private static void setContinueIfOneFails(boolean value) {
        Object sessionValue;
        synchronized (MCRSessionMgr.getCurrentSession()) {
            sessionValue = MCRSessionMgr.getCurrentSession().get("MCRWebCLI");
            ((MCRWebCLIContainer) sessionValue).setContinueIfOneFails(value, true);
        }
    }
}
