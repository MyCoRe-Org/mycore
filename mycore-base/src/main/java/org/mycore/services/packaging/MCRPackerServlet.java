package org.mycore.services.packaging;

import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.mycore.access.MCRAccessManager;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * <p>Servlet for {@link MCRPackerManager}.</p>
 * <p>
 * <p>You can pass a <code>redirect</code> parameter to the servlet!</p>
 * <p><b>The user needs the privilege packer-MyPackerID to create a package!</b></p>
 *
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRPackerServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        String packer = job.getRequest().getParameter("packer");
        if (packer == null || packer.isEmpty()) {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "No or invalid 'packer' parameter!");
            return;
        }

        String privilege = "packer-" + packer;
        if (!MCRAccessManager.checkPermission(privilege)) {
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN,
                "You don't have the privilege '" + privilege + "' to create a Package!");
            return;
        }

        Map<String, String> jobParameters = resolveJobParameters(job);

        MCRPackerManager.startPacking(jobParameters);
        if (jobParameters.containsKey("redirect")) {
            String redirect = jobParameters.get("redirect");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(redirect));
        }
    }

    private Map<String, String> resolveJobParameters(MCRServletJob job) {
        return job.getRequest().getParameterMap()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> (e.getValue().length >= 1) ? e.getValue()[0] : ""));
    }
}
