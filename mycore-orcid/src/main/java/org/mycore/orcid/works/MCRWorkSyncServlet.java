package org.mycore.orcid.works;

import javax.servlet.http.HttpServletResponse;

import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.orcid.MCRORCIDProfile;
import org.mycore.orcid.user.MCRORCIDPublicationStatus;
import org.mycore.orcid.user.MCRORCIDSession;
import org.mycore.orcid.user.MCRORCIDUser;

/**
 * Publishes a work in the current user's ORCID profile, or
 * updates an existing work there, given the object ID of the local MODS object.
 *
 * The request must contain the parameter "id" which ist the MCRObjectID to publish.
 * The current user must have an ORCID profile and must have authorized this application
 * to add or updated works.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRWorkSyncServlet extends MCRServlet {

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        MCRORCIDUser user = MCRORCIDSession.getORCIDUser();

        if (!user.weAreTrustedParty()) {
            job.getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "Current user did not authorize the application to update his ORCID profile");
            return;
        }

        MCRORCIDProfile profile = user.getORCIDProfile();
        MCRWorksSection works = profile.getWorksSection();

        String[] ids = job.getRequest().getParameterValues("id");

        for (String id : ids) {
            MCRObjectID oid = MCRObjectID.getInstance(id);
            if (!MCRMetadataManager.exists(oid)) {
                job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Publication with ID " + id + " does not exist");
                return;
            }
        }

        for (String id : ids) {
            MCRObjectID oid = MCRObjectID.getInstance(id);
            switch (user.getPublicationStatus(id)) {
                case NO_ORCID_USER:
                    job.getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Current user does not have an ORCID profile we know of");
                    return;
                case NOT_MINE:
                    job.getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Current user is not related to publication " + id);
                    return;
                case NOT_IN_MY_ORCID_PROFILE:
                    works.addWorkFrom(oid);
                    break;
                case IN_MY_ORCID_PROFILE:
                    works.findWork(oid).get().update();
                    break;
            }
        }
        job.getResponse().setStatus(HttpServletResponse.SC_OK);
    }
}
