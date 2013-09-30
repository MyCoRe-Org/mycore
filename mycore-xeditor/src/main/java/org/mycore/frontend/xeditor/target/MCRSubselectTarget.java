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


import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCRSubselect;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRSubselectTarget extends MCREditorTarget {

    private final static Logger LOGGER = Logger.getLogger(MCRSubselectTarget.class);

    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter) throws Exception {
        session.getSubmission().setSubmittedValues(job.getRequest().getParameterMap());
        session.getValidator().forgetInvalidFields();

        MCRSubselect subselect = new MCRSubselect(session, parameter);
        String url = subselect.getRedirectURL();
        LOGGER.info("Redirecting to subselect " + url);
        job.getResponse().sendRedirect(url);
    }
}
