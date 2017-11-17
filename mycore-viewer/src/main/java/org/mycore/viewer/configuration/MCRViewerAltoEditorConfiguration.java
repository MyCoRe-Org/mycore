package org.mycore.viewer.configuration;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.inject.MCRInjectorConfig;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.viewer.alto.MCRALTOUtil;
import org.mycore.viewer.alto.model.MCRStoredChangeSet;
import org.mycore.viewer.alto.service.MCRAltoChangeSetStore;

public class MCRViewerAltoEditorConfiguration extends MCRViewerConfiguration {

    private MCRAltoChangeSetStore changeSetStore = MCRInjectorConfig.injector()
        .getInstance(MCRAltoChangeSetStore.class);

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        String derivate = getDerivate(request);
        if (MCRAccessManager.checkPermission(derivate, MCRALTOUtil.EDIT_ALTO_PERMISSION)) {
            this.setProperty("altoEditorPostURL", MCRFrontendUtil.getBaseURL(request) + "rsc/viewer/alto");

            Map<String, String[]> parameterMap = request.getParameterMap();
            String[] altoChangeIDS;

            boolean isReviewer = MCRAccessManager.checkPermission(derivate, MCRALTOUtil.REVIEW_ALTO_PERMISSION);
            if (isReviewer) {
                this.setProperty("altoReviewer", true);
            }

            if (parameterMap.containsKey("altoChangeID")
                && (altoChangeIDS = parameterMap.get("altoChangeID")).length > 0) {
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
