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
 * <p><b>The user needs the permission packer-MyPackerID to create a package!</b></p>
 *
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRPackerServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;


    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        Map<String, String[]> parameterMap = job.getRequest().getParameterMap();

        String[] packers = parameterMap.get("packer");
        if (packers == null || packers.length < 1) {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "No or invalid 'packer' parameter!");
            return;
        }

        if (!MCRAccessManager.checkPermission("packer-" + packers[0])) {
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, "You don't have the permission to create a Package!");
            return;
        }

        Map<String, String> jobParameters = resolveJobParameters(job);

        MCRPackerManager.startPacking(jobParameters);
        if (jobParameters.containsKey("redirect")) {
            String redirect = jobParameters.get("redirect");
            job.getResponse().sendRedirect(redirect);
        }
    }

    private Map<String, String> resolveJobParameters(MCRServletJob job) {
        return job.getRequest().getParameterMap()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (e.getValue().length >= 1) ? e.getValue()[0] : ""));
    }
}
