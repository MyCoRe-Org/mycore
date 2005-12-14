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

// package
package org.mycore.frontend.workflow;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;

/**
 * This class holds all EventHandler methods to manage the access part of the
 * simple workflow.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRSimpleWorkflowAccessEventHandler extends MCREventHandlerBase {

    // the logger
    private static Logger LOGGER = Logger.getLogger(MCRSimpleWorkflowAccessEventHandler.class);

    /**
     * This method will be used to create the access rules for SWF for a MCRObject. 
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // create
        MCRObjectID mcr_id = obj.getId();
        LOGGER.debug("MCRSimpleWorkflowAccessEventHandler create: MCRObjectID : " + mcr_id.getId());
        MCRObjectService serv = obj.getService();
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.READWEB_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.WRITEDB_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.WRITEWF_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.DELETEDB_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.DELETEWF_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.COMMITDB_POOL,serv);
        
        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (double) (t2 - t1) / 1000.0;
        LOGGER.debug("MCRSimpleWorkflowAccessEventHandler create: done in " + diff + " sec.");
    }

    /**
     * This method will be used to update the access rules for SWF for a MCRObject. 
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // update
        MCRObjectID mcr_id = obj.getId();
        LOGGER.debug("MCRSimpleWorkflowAccessEventHandler update: MCRObjectID : " + mcr_id.getId());
        MCRObjectService serv = obj.getService();
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.READWEB_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.WRITEDB_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.WRITEWF_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.DELETEDB_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.DELETEWF_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.COMMITDB_POOL,serv);

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (double) (t2 - t1) / 1000.0;
        LOGGER.debug("MCRSimpleWorkflowAccessEventHandler update: done in " + diff + " sec.");
    }

    /**
     * This method will be used to delete the access rules for SWF for a MCRObject. 
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // delete
        MCRObjectID mcr_id = obj.getId();
        LOGGER.debug("MCRSimpleWorkflowAccessEventHandler delete: MCRObjectID : " + mcr_id.getId());
        MCRObjectService serv = obj.getService();
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.READWEB_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.WRITEDB_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.WRITEWF_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.DELETEDB_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.DELETEWF_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.COMMITDB_POOL,serv);

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (double) (t2 - t1) / 1000.0;
        LOGGER.debug("MCRSimpleWorkflowAccessEventHandler delete: done in " + diff + " sec.");
    }

    /**
     * This method will be used to repair the access rules for SWF for a MCRObject. 
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        // save the start time
        long t1 = System.currentTimeMillis();

        // repair
        MCRObjectID mcr_id = obj.getId();
        LOGGER.debug("MCRSimpleWorkflowAccessEventHandler repair: MCRObjectID : " + mcr_id.getId());
        MCRObjectService serv = obj.getService();
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.READWEB_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.WRITEDB_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.WRITEWF_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.DELETEDB_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.DELETEWF_POOL,serv);
        MCRSimpleWorkflowAccess.getAccessRule(MCRSimpleWorkflowAccess.COMMITDB_POOL,serv);

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (double) (t2 - t1) / 1000.0;
        LOGGER.debug("MCRSimpleWorkflowAccessEventHandler repair: done in " + diff + " sec.");
    }

}
