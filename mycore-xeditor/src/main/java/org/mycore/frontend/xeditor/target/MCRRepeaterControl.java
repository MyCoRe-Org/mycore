/*
 * $Revision: 28114 $ 
 * $Date: 2013-10-11 18:04:09 +0200 (Fr, 11 Okt 2013) $
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

import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCREditorSession;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public abstract class MCRRepeaterControl implements MCREditorTarget {

    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String buttonName)
        throws Exception {
        session.getSubmission().setSubmittedValues(job.getRequest().getParameterMap());
        int posOfAnchor = buttonName.lastIndexOf('|');
        String param = buttonName.substring(0, posOfAnchor);
        String anchor = buttonName.substring(posOfAnchor + 1);
        handleRepeaterControl(context, job, session, param);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(session.getRedirectURL(anchor)));
    }

    protected abstract void handleRepeaterControl(ServletContext context, MCRServletJob job, MCREditorSession session,
        String param)
        throws Exception;
}
