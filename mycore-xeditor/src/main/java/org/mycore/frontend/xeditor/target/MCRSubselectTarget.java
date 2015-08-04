/*
 * $Revision: 27994 $ 
 * $Date: 2013-09-27 09:00:49 +0200 (Fr, 27 Sep 2013) $
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

package org.mycore.frontend.xeditor.target;

import java.nio.charset.StandardCharsets;

import javax.servlet.ServletContext;
import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;
import org.mycore.common.MCRUtils;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCRXPathEvaluator;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRSubselectTarget implements MCREditorTarget {

    public static final String PARAM_SUBSELECT_SESSION = "_xed_subselect_session";

    private final static Logger LOGGER = Logger.getLogger(MCRSubselectTarget.class);

    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter)
        throws Exception {
        session.getSubmission().setSubmittedValues(job.getRequest().getParameterMap());

        int pos = parameter.lastIndexOf(":");
        String xPath = parameter.substring(0, pos);
        String href = decode(parameter.substring(pos + 1));

        LOGGER.info("New subselect for " + xPath + " using pattern " + href);

        MCRBinding binding = new MCRBinding(xPath, false, session.getRootBinding());
        href = new MCRXPathEvaluator(binding).replaceXPaths(href, true);
        binding.detach();

        session.setBreakpoint("After starting subselect at " + href + " for " + xPath);

        href += (href.contains("?") ? "&" : "?") + PARAM_SUBSELECT_SESSION + "=" + session.getCombinedSessionStepID();

        LOGGER.info("Redirecting to subselect " + href);
        job.getResponse().sendRedirect(href);
    }

    public static String encode(String href) {
        return MCRUtils.toHexString(href.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String href) {
        return new String(DatatypeConverter.parseHexBinary(href), StandardCharsets.UTF_8);
    }
}
