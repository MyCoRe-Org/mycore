/*
 *
 * $Revision: 25642 $ $Date: 2012-12-21 11:37:10 +0100 (Fr, 21 Dez 2012) $
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

package org.mycore.datamodel.common;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectService;

/**
 * This event handler sets the service flags "createdby" and "modifiedby" for users who created / modified a
 * MyCoReObject and also added a state service flag using classification defined in
 * "MCR.Metadata.Service.State.Classification.ID" (default "state") and category defined in
 * "MCR.Metadata.Service.State.Category.Default" (default "submitted").
 *
 * @author Robert Stephan
 * @author Thomas Scheffler (yagee)
 */
public class MCRServiceFlagEventHandler extends MCREventHandlerBase {

    @Override
    protected final void handleObjectCreated(MCREvent evt, MCRObject obj) {
        if (!obj.isImportMode()) {
            handleCreated(obj.getService());
            setDefaultState(obj.getService());
        }
    }

    @Override
    protected final void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        if (!obj.isImportMode()) {
            handleUpdated(obj.getService());
        }
    }

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        if (!der.isImportMode()) {
            handleCreated(der.getService());
        }
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        if (!der.isImportMode()) {
            handleUpdated(der.getService());
        }
    }

    private static void handleCreated(MCRObjectService objService) {
        objService.removeFlags(MCRObjectService.FLAG_TYPE_CREATEDBY);
        objService.addFlag(MCRObjectService.FLAG_TYPE_CREATEDBY, MCRSessionMgr.getCurrentSession()
            .getUserInformation().getUserID());
        handleUpdated(objService);
    }

    private static void setDefaultState(MCRObjectService objService) {
        if (objService.getState() == null) {
            MCRCategoryID defaultState = new MCRCategoryID(
                MCRConfiguration.instance().getString("MCR.Metadata.Service.State.Classification.ID"),
                MCRConfiguration.instance().getString("MCR.Metadata.Service.State.Category.Default"));
            objService.setState(defaultState);
        }
    }

    private static void handleUpdated(MCRObjectService objectService) {
        objectService.removeFlags(MCRObjectService.FLAG_TYPE_MODIFIEDBY);
        objectService.addFlag(MCRObjectService.FLAG_TYPE_MODIFIEDBY,
            MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
    }

}
