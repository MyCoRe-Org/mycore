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

package org.mycore.access.mcrimpl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * This class provides a set of commands for the org.mycore.access package which
 * can be used by the command line interface. (creates sql tables, run queries
 *
 * @author Arne Seifert
 */
@MCRCommandGroup(name = "Access Control Commands")
public class MCRAccessCtrlCommands extends MCRAbstractCommands {
    public static Logger logger = LogManager.getLogger(MCRAccessCtrlCommands.class.getName());

    /**
     * validates access for given object and given permission
     *
     * @param objid
     *            internal database ruleid
     * @param permission
     *            the access permission for the rule
     */
    @MCRCommand(syntax = "validate objectid {0} in pool {1}",
        help = "Validates access for given object and given permission",
        order = 10)
    public static void validate(String objid, String permission) {
        System.out.println("current user has access: " + MCRAccessManager.checkPermission(objid, permission));
    }
}
