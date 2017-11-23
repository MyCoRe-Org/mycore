// package
package org.mycore.access;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger LOGGER = LogManager.getLogger(MCRRemoveAclEventHandler.class);

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        handleAddOrModify(obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        handleAddOrModify(obj);
    }

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        handleDelete(obj);
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
    }

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        handleAddOrModify(der);
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        handleAddOrModify(der);
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        handleDelete(der);
    }

    @Override
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
        LOGGER.debug("event handled in {}", diff);
    }

    private void handleDelete(MCRBase base) {
        long start = System.currentTimeMillis();
        MCRAccessManager.removeAllRules(base.getId());
        long diff = System.currentTimeMillis() - start;
        LOGGER.debug("event handled in {}", diff);
    }
}
