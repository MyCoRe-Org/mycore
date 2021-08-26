/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mcr.acl.accesskey.cli;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.mcr.acl.accesskey.MCRAccessKeyManager;

@MCRCommandGroup(
    name = "Access keys")
public class MCRAccessKeyCommands {
    
    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "clear access keys",
        help = "Deletes all access keys")
    public static void clearAccessKeys() throws Exception {
        MCRAccessKeyManager.clearAccessKeys();
        LOGGER.info("cleared all access keys");
    }

    @MCRCommand(syntax = "clear all access keys of id {0}",
        help = "Deletes all access keys of given MCRObject/Derivate")
    public static void clearAccessKeys(String objId) throws Exception {
        final MCRObjectID objectId = MCRObjectID.getInstance(objId);
        MCRAccessKeyManager.clearAccessKeys(objectId);
        LOGGER.info("cleared all access keys of {}.", objId);
    }
}
