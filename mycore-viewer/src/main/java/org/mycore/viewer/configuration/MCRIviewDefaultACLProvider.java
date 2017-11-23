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

package org.mycore.viewer.configuration;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRIviewDefaultACLProvider implements MCRIviewACLProvider {

    @Override
    public boolean checkAccess(HttpSession session, MCRObjectID derivateID) {
        if (MCRAccessManager.checkPermission(derivateID, "read")) {
            return true;
        }
        MCRObjectID objectId = MCRMetadataManager.getObjectId(derivateID, 10, TimeUnit.MINUTES);
        return MCRAccessManager.checkPermission(objectId, "view-derivate");
    }

}
