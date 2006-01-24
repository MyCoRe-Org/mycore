/**
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
package org.mycore.access;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * This class holds all EventHandler methods to manage the access part of the
 * simple workflow.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRAccessEventHandler extends MCREventHandlerBase {

    // the logger
    private static Logger LOGGER = Logger.getLogger(MCRAccessEventHandler.class);

    // the access interface
    private MCRAccessManagerBase AI = (MCRAccessManagerBase) MCRConfiguration.instance().getInstanceOf("MCR.Access_class_name");

    /**
     * This method will be used to create the access rules for SWF for a
     * MCRObject.
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
        int rulesize = obj.getService().getRulesSize();
        for (int i = 0; i < rulesize; i++) {
            org.jdom.Element conditions = obj.getService().getRule(i).getCondition();
            String pool = obj.getService().getRule(i).getPool();
            AI.addRule(obj.getId(), pool, conditions);
            // obj.getService().removeRule(i);
        }

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (double) (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler create: done in " + diff + " sec.");
    }

    /**
     * This method will be used to update the access rules for SWF for a
     * MCRObject.
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
        int rulesize = obj.getService().getRulesSize();
        for (int i = 0; i < rulesize; i++) {
            org.jdom.Element conditions = obj.getService().getRule(i).getCondition();
            String pool = obj.getService().getRule(i).getPool();
            AI.updateRule(obj.getId(), pool, conditions);
            // obj.getService().removeRule(i);
        }

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (double) (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler update: done in " + diff + " sec.");
    }

    /**
     * This method will be used to delete the access rules for SWF for a
     * MCRObject.
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
        AI.removeAllRules(obj.getId());

        // save the stop time
        long t2 = System.currentTimeMillis();
        double diff = (double) (t2 - t1) / 1000.0;
        LOGGER.debug("MCRAccessEventHandler delete: done in " + diff + " sec.");
    }

    /**
     * This method will be used to repair the access rules for SWF for a
     * MCRObject.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        // Do nothing
    }

}
