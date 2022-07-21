/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  MyCoRe is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MyCoRe is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.backend.jpa;

import java.time.Instant;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;

public class MCRObjectEntityEventHandler extends MCREventHandlerBase {

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        MCRObjectEntityManager.update(obj);
    }


    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        MCRObjectEntityManager.update(obj);
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        MCRObjectEntityManager.remove(obj);
        MCRObjectEntityManager.create(obj);
    }

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        MCRObjectEntityManager.delete(obj, Instant.now(),
                MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
    }

}
