/**
 * 
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

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * This class contains EventHandler methods to remove the access part of
 * MCRObjects.
 * 
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRRemoveAclEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = Logger.getLogger(MCRRemoveAclEventHandler.class);

    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        handleAddOrModify(obj);
    }

    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        handleAddOrModify(obj);
    }

    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        handleDelete(obj);
    }

    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
    }

    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        handleAddOrModify(der);
    }

    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        handleAddOrModify(der);
    }

    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        handleDelete(der);
    }

    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate der) {
    }

    private void handleAddOrModify(MCRBase base) {
        long start = System.currentTimeMillis();
        int rulesize = base.getService().getRulesSize();
        while (0 < rulesize) {
            base.getService().removeRule(0);
            rulesize--;
        }
        long diff = System.currentTimeMillis() - start;
        LOGGER.debug("event handled in " + diff);
    }

    private void handleDelete(MCRBase base) {
        long start = System.currentTimeMillis();
        MCRAccessManager.removeAllRules(base.getId());
        long diff = System.currentTimeMillis() - start;
        LOGGER.debug("event handled in " + diff);
    }
}
