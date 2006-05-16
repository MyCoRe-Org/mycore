/*
 * $RCSfile$
 * $Revision$ $Date$
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

package org.mycore.frontend.servlets;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;

/**
 * This class implements a MCRServlet based on MCRCheckBase and is a Servlet to
 * store the file upload from the formular of web application they use the HTTP
 * port.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRCheckCommitFileServlet extends MCRCheckBase {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * The method is a dummy and return an URL with the next working step.
     * 
     * @param ID
     *            the MCRObjectID of the MCRObject
     * @param okay
     *            the return value of the store operation
     * @return the next URL as String
     */
    public final String getNextURL(MCRObjectID ID, boolean okay) {
        StringBuffer sb = new StringBuffer(getBaseURL()).append("receive/").append(ID.getId());
        return sb.toString();
    }

    /**
     * The method send a message to the mail address for the MCRObjectType.
     * 
     * @param ID
     *            the MCRObjectID of the MCRObject
     */
    public final void sendMail(MCRObjectID ID) {
    }

    /**
     * This method overrides doGetPost of MCRServlet. It store all files
     * directly in the IFS file system.
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        // read the XML data
        MCREditorSubmission sub = (MCREditorSubmission) (job.getRequest().getAttribute("MCREditorSubmission"));
        List files = sub.getFiles();

        // read the parameter
        MCRRequestParameters parms;

        if (sub == null) {
            parms = new MCRRequestParameters(job.getRequest());
        } else {
            parms = sub.getParameters();
        }

        String se_mcrid = parms.getParameter("mcrid");
        String re_mcrid = parms.getParameter("remcrid");
        String type = parms.getParameter("type");
        String step = parms.getParameter("step");
        LOGGER.debug("XSL.target.param.0 = " + se_mcrid);
        LOGGER.debug("XSL.target.param.1 = " + type);
        LOGGER.debug("XSL.target.param.2 = " + step);
        LOGGER.debug("XSL.target.param.3 = " + re_mcrid);

        if (!AI.checkPermission(re_mcrid, "writedb")) {
            String usererrorpage = CONFIG.getString("MCR.editor_page_dir", "") + CONFIG.getString("MCR.editor_page_error_user", "editor_error_user.xml");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }

        // get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String mylang = mcrSession.getCurrentLanguage();
        LOGGER.info("LANG = " + mylang);

        // prepare the derivate MCRObjectID
        MCRObjectID ID = new MCRObjectID(re_mcrid);
        MCRObjectID DD = new MCRObjectID(se_mcrid);

        // save the files
        ArrayList ffname = new ArrayList();
        String mainfile = "";

        for (int i = 0; i < files.size(); i++) {
            FileItem item = (FileItem) (files.get(i));
            String fname = item.getName().trim();
            int j = 0;
            int l = fname.length();

            while (j < l) {
                int k = fname.indexOf("\\", j);

                if (k == -1) {
                    k = fname.indexOf("/", j);

                    if (k == -1) {
                        fname = fname.substring(j, l);

                        break;
                    }
                    j = k + 1;
                } else {
                    j = k + 1;
                }
            }
            fname.replace(' ', '_');
            ffname.add(fname);

            try {
            MCRDirectory difs = MCRDirectory.getRootDirectory(se_mcrid);
            /*
            MCRUtils.copyStream(item.getInputStream(), fouts);
            */
            } catch (Exception e) {
            e.printStackTrace();
            }
            LOGGER.info("Data object stored under " + se_mcrid + " --> " + fname);
        }

        // add the mainfile entry
        if ((mainfile.length() == 0) && (ffname.size() > 0)) {
            mainfile = (String) ffname.get(0);
        }
        MCRDerivate der = new MCRDerivate();
        try {
            der.receiveFromDatastore(DD);
            if (der.getDerivate().getInternals().getMainDoc().equals("#####")) {
                der.getDerivate().getInternals().setMainDoc(mainfile);
           }
            der.updateXMLInDatastore();
        } catch (Exception e) {
            LOGGER.warn("Can't set main file in derivate " + se_mcrid + ".");
        }

        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + getNextURL(DD, false)));
    }
}
