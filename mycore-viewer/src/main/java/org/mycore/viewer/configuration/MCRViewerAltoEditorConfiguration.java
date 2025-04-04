/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.viewer.alto.MCRALTOUtil;
import org.mycore.viewer.alto.model.MCRStoredChangeSet;
import org.mycore.viewer.alto.service.MCRAltoChangeSetStore;

import jakarta.servlet.http.HttpServletRequest;

public class MCRViewerAltoEditorConfiguration extends MCRViewerConfiguration {

    private final MCRAltoChangeSetStore changeSetStore = MCRConfiguration2.getInstanceOfOrThrow(
        MCRAltoChangeSetStore.class, "MCR.Viewer.AltoChangeSetStore.Class");

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        String derivate = getDerivate(request);
        if (MCRAccessManager.checkPermission(derivate, MCRALTOUtil.EDIT_ALTO_PERMISSION)) {
            this.setProperty("altoEditorPostURL", MCRFrontendUtil.getBaseURL(request) + "rsc/viewer/alto");

            boolean isReviewer = MCRAccessManager.checkPermission(derivate, MCRALTOUtil.REVIEW_ALTO_PERMISSION);
            if (isReviewer) {
                this.setProperty("altoReviewer", true);
            }

            String[] altoChangeIDS = request.getParameterMap().getOrDefault("altoChangeID", new String[0]);

            if (altoChangeIDS.length > 0) {
                String altoChangeID = altoChangeIDS[0];
                MCRStoredChangeSet mcrStoredChangeSet = changeSetStore.get(altoChangeID);
                if (isReviewer || mcrStoredChangeSet.getSessionID().equals(MCRSessionMgr.getCurrentSessionID())) {
                    this.setProperty("altoChangePID", altoChangeID);
                    this.setProperty("altoChanges", mcrStoredChangeSet.getChangeSet());
                    this.setProperty("leftShowOnStart", "altoEditor");
                }
            }
        }

        return this;
    }
}
