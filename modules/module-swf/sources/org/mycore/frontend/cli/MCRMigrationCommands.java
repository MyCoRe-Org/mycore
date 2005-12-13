/*
 * $RCSfile$
 * $Revision$ $Date$
 *
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

package org.mycore.frontend.cli;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRXMLTableManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.user.MCRUserMgr;
import org.mycore.frontend.workflow.MCRSimpleWorkflowAccess;

/**
 * This class implements the commands to migrate data to the next MyCoRe
 * version. This migration works with data from the <b>SimplwWorkflow (SWF)</b>
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
final class MCRMigrationCommands extends MCRAbstractCommands {
    // logger
    private static final Logger LOGGER = Logger.getLogger(MCRMigrationCommands.class);

    /**
     * The constructor.
     */
    public MCRMigrationCommands() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("migrate xml access data of type {0}", "org.mycore.frontend.cli.MCRMigrationCommands.mirgateXMLToAccessSystem String", "The command imigrate all servflag access data to the access system of MyCoRe 1.3.");
        command.add(com);
    }

    /**
     * The method migrate the old SimpleWorkflow-servflag-data to the new
     * MCRObject data structure of the Access System of version 1.3. It put all
     * <ul>
     * <li><em>User:</em> date to the <b>servusers</b>-XML-element.</li>
     * <li><em>IP:</em> date to the <b>servnet</b>-XML-element.</li>
     * <li>and add the group of the <em>Access:</em> to the <b>servgroup</b>-XML-element.</li>
     * 
     * @param type
     *            the type of the MCRObjects
     */
    public static void mirgateXMLToAccessSystem(String type) throws MCRException {
        LOGGER.info("Start the migration for access system for type " + type);

        String typetest = CONFIG.getString("MCR.type_" + type, "");

        if (typetest.length() == 0) {
            LOGGER.error("The type " + type + " was not found.");
            LOGGER.info(" ");

            return;
        }

        // XML table manager
        MCRXMLTableManager mcr_xml = MCRXMLTableManager.instance();
        ArrayList ar = mcr_xml.retrieveAllIDs(type);
        String stid = null;

        MCRObject obj;
        for (int i = 0; i < ar.size(); i++) {
            stid = (String) ar.get(i);

            // read data
            obj = new MCRObject();
            obj.receiveFromDatastore(stid);

            // migrate
            MCRObjectService serv = obj.getService();
            for (int j = 0; j < serv.getFlagSize(); j++) {
                if (serv.getFlag(j).startsWith("Access:")) {
                    String priv = serv.getFlag(j).substring(7, serv.getFlag(j).length());
                    MCRUserMgr umgr = MCRUserMgr.instance();
                    // read access for all groups with the 'Access:'-privileg
                    ArrayList gar = umgr.getAllGroupIDs();
                    for (int k = 0; k < gar.size(); k++) {
                        if (umgr.hasGroupPrivilege((String) gar.get(k), priv)) {
                            serv.addGroup((String) gar.get(k), MCRSimpleWorkflowAccess.READ_POOL);
                        }
                    }
                    // serv.removeFlag(j);
                }
                if (serv.getFlag(j).startsWith("User:")) {
                    String user = serv.getFlag(j).substring(5, serv.getFlag(j).length());
                    MCRUserMgr umgr = MCRUserMgr.instance();
                    // read access for all groups of the user
                    // Write, delete, commit acces for the master group wit editor privilege
                    ArrayList gg = umgr.getGroupsContainingUser(user);
                    for (int k = 0; k < gg.size(); k++) {
                        serv.addGroup((String) gg.get(k), MCRSimpleWorkflowAccess.READ_POOL);
                        ArrayList mg = umgr.retrieveGroup((String)gg.get(k)).getMemberGroupIDs();
                        for (int l = 0; l < mg.size(); l++) {
                            if (umgr.hasGroupPrivilege((String)mg.get(l), "editor")) {
                                serv.addGroup((String)mg.get(l), MCRSimpleWorkflowAccess.WRITE_POOL);
                                serv.addGroup((String)mg.get(l), MCRSimpleWorkflowAccess.WRITEWF_POOL);
                                serv.addGroup((String)mg.get(l), MCRSimpleWorkflowAccess.DELETE_POOL);
                                serv.addGroup((String)mg.get(l), MCRSimpleWorkflowAccess.DELETEWF_POOL);
                                serv.addGroup((String)mg.get(l), MCRSimpleWorkflowAccess.COMMIT_POOL);
                            }
                        }
                    }
                    // write access for the user if he has the modify privilege
                    if (umgr.hasUserPrivilege(user, "modify-" + type)) {
                        serv.addUser(user, MCRSimpleWorkflowAccess.WRITE_POOL);
                        serv.addUser(user, MCRSimpleWorkflowAccess.WRITEWF_POOL);
                    }
                    // delete access for the user if he has the delete privilege
                    if (umgr.hasUserPrivilege(user, "delete-" + type)) {
                        serv.addUser(user, MCRSimpleWorkflowAccess.DELETE_POOL);
                        serv.addUser(user, MCRSimpleWorkflowAccess.DELETEWF_POOL);
                    }
                    // commit access for the user if he has the commit privilege
                    if (umgr.hasUserPrivilege(user, "commit-" + type)) {
                        serv.addUser(user, MCRSimpleWorkflowAccess.COMMIT_POOL);
                    }
                    // serv.removeFlag(j);
                }
                if (serv.getFlag(j).startsWith("IP:")) {
                    serv.addIP(serv.getFlag(j), MCRSimpleWorkflowAccess.READ_POOL);
                    serv.addIP(serv.getFlag(j), MCRSimpleWorkflowAccess.WRITE_POOL);
                    serv.addIP(serv.getFlag(j), MCRSimpleWorkflowAccess.WRITEWF_POOL);
                    serv.addIP(serv.getFlag(j), MCRSimpleWorkflowAccess.DELETE_POOL);
                    serv.addIP(serv.getFlag(j), MCRSimpleWorkflowAccess.DELETEWF_POOL);
                    serv.removeFlag(j);
                }
            }

            // save data
            obj.updateInDatastore();
            LOGGER.info("Migrated " + (String) ar.get(i));

        }

        LOGGER.info(" ");
    }

}