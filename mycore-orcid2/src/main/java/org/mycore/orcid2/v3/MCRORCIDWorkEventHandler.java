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

package org.mycore.orcid2.v3;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.orcid2.user.MCRORCIDCredentials;

/**
 * Work handler which publishes object to ORCID.
 */
public class MCRORCIDWorkEventHandler extends org.mycore.orcid2.MCRORCIDWorkEventHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void publishToORCID(MCRObject object, List<MCRORCIDCredentials> credentialsList) {
        // TODO maybe rethrow exc
        try {
            MCRORCIDWorkHelper.publishToORCID(object, credentialsList);
        } catch (Exception ex) {
            LOGGER.warn("Could not publish {} to ORCID profile.", object.getId(), ex);
        }
    }
}
