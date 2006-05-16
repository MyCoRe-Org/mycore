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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;

/**
 * This class implements MCRServlet and extends MCRCheckBase. It read files from upload formular and store them in the workflow..
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRCheckNewFileServlet extends MCRCheckBase {

    private static final long serialVersionUID = 1L;

    /**
     * This method overrides doGetPost of MCRServlet.
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

        if (step.equals("author")) {
            if (!AI.checkPermission("create-" + type)) {
                String usererrorpage = CONFIG.getString("MCR.editor_page_dir", "") + CONFIG.getString("MCR.editor_page_error_user", "editor_error_user.xml");
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }
        }

        // get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String mylang = mcrSession.getCurrentLanguage();
        LOGGER.info("LANG = " + mylang);

        // prepare the derivate MCRObjectID
        MCRObjectID ID = new MCRObjectID(re_mcrid);
        MCRObjectID DD = new MCRObjectID(se_mcrid);
        String workdir = CONFIG.getString("MCR.editor_" + ID.getTypeId() + "_directory", "/");
        String dirname = workdir + NL + se_mcrid;

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

            File fout = new File(dirname, fname);
            FileOutputStream fouts = new FileOutputStream(fout);
            MCRUtils.copyStream(item.getInputStream(), fouts);
            fouts.close();
            LOGGER.info("Data object stored under " + fout.getName());
        }

        if ((mainfile.length() == 0) && (ffname.size() > 0)) {
            mainfile = (String) ffname.get(0);
        }

        // add the mainfile entry
        MCRDerivate der = new MCRDerivate();

        try {
            der.setFromURI(dirname + ".xml");

            if (der.getDerivate().getInternals().getMainDoc().equals("#####")) {
                der.getDerivate().getInternals().setMainDoc(mainfile);

                byte[] outxml = MCRUtils.getByteArray(der.createXML());

                try {
                    FileOutputStream out = new FileOutputStream(dirname + ".xml");
                    out.write(outxml);
                    out.flush();
                } catch (IOException ex) {
                    LOGGER.error(ex.getMessage());
                    LOGGER.error("Exception while store to file " + dirname + ".xml");
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Can't open file " + dirname + ".xml");
        }

        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + getNextURL(ID, DD, step)));
    }

    /**
     * The method return an URL with the next working step. If okay flag is
     * true, the object will present else it shows the error page.
     * 
     * @param ID
     *            the MCRObjectID of the MCRObject
     * @param okay
     *            the return value of the store operation
     * @return the next URL as String
     */
    protected String getNextURL(MCRObjectID ID, boolean okay) throws MCRActiveLinkException {
        StringBuffer sb = new StringBuffer();
        if (okay) {
            sb.append(CONFIG.getString("MCR.editor_page_dir", "")).append("editor_").append(ID.getTypeId()).append("_editor.xml");
        } else {

            sb.append(CONFIG.getString("MCR.editor_page_dir", "")).append(CONFIG.getString("MCR.editor_page_error_store", "editor_error_store.xml"));
        }
        return sb.toString();
    }

	/**
	 * The method is a dummy and return an URL with the next working step.
	 * 
	 * @param ID
	 *            the MCRObjectID of the MCRObject
	 * @param DD
	 *            the MCRObjectID of the MCRDerivate
	 * @param step
	 *            the step text as String
	 * @return the next URL as String
	 */
	public final String getNextURL(MCRObjectID ID, MCRObjectID DD, String step) throws Exception {
		// return all is ready
		StringBuffer sb = new StringBuffer();
		sb.append(CONFIG.getString("MCR.editor_page_dir", "")).append("editor_").append(ID.getTypeId()).append("_editor.xml");

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
}
