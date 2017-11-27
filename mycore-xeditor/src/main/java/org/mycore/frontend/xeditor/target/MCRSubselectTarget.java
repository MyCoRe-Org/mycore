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

package org.mycore.frontend.xeditor.target;

import java.nio.charset.StandardCharsets;

import javax.servlet.ServletContext;
import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRUtils;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRSubselectTarget implements MCREditorTarget {

    public static final String PARAM_SUBSELECT_SESSION = "_xed_subselect_session";

    private static final Logger LOGGER = LogManager.getLogger(MCRSubselectTarget.class);

    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter)
        throws Exception {
        session.getSubmission().setSubmittedValues(job.getRequest().getParameterMap());

        int pos = parameter.lastIndexOf(":");
        String xPath = parameter.substring(0, pos);
        String href = decode(parameter.substring(pos + 1));

        LOGGER.info("New subselect for {} using pattern {}", xPath, href);

        MCRBinding binding = new MCRBinding(xPath, false, session.getRootBinding());
        href = binding.getXPathEvaluator().replaceXPaths(href, true);
        binding.detach();

        session.setBreakpoint("After starting subselect at " + href + " for " + xPath);

        href += (href.contains("?") ? "&" : "?") + PARAM_SUBSELECT_SESSION + "=" + session.getCombinedSessionStepID();

        LOGGER.info("Redirecting to subselect {}", href);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(href));
    }

    public static String encode(String href) {
        return MCRUtils.toHexString(href.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String href) {
        return new String(DatatypeConverter.parseHexBinary(href), StandardCharsets.UTF_8);
    }
}
