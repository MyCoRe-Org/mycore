/*
 * 
 * $Revision: 1.12 $ $Date: 2009/03/06 09:18:23 $
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.mycore.access.MCRAccessManager;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.fileupload.MCRSWFUploadHandlerIFS;
import org.mycore.frontend.metsmods.MetsModsUtil;
import org.mycore.frontend.servlets.MCRServletJob;
import org.hibernate.Transaction;
import org.jdom.*;
import org.jdom.output.XMLOutputter;

/**
 * 
 * 
 * @author Stefan Freitag
 * @version $Revision: 1.12 $ $Date: 2009/03/06 09:18:23 $
 */

public class MCRStartMetsModsServlet extends MCRStartEditorServlet {

    private static String metsfile = MCRConfiguration.instance().getString("MCR.MetsMots.ConfigFile", "mets.xml");

    private Transaction tx;

    /**
     * public void doGetPost(MCRServletJob job) throws Exception {
     * job.getResponse().getWriter().print("<html><head></head><body><h1>Klappt
     * (2)!</h1></body></html>"); }
     **/

    private void addPicturesToList(MCRDirectory dir, ArrayList<String> list) {
        for (int i = 0; i < dir.getChildren().length; i++) {
            try {
                dir = (MCRDirectory) dir.getChildren()[i];
                addPicturesToList(dir, list);
            } catch (Exception ClassCastException) {
                if (!list.contains(dir.getPath() + "/" + ((MCRFile) dir.getChildren()[i]).getName()))
                    list.add(dir.getPath() + "/" + ((MCRFile) dir.getChildren()[i]).getName());
            }
        }
    }

    private boolean searchForMets(MCRDirectory dir) {
        MCRFile mcrfile = (MCRFile) dir.getChild(metsfile);
        if (mcrfile == null) {
            // LOGGER.info("Nichts gefunden!");
            return false;
        } else {
            // LOGGER.info("Sieht gut aus!");
            return true;
        }
    }

    public void seditmets(MCRServletJob job, CommonData cd) throws IOException {
        if (!MCRAccessManager.checkPermission(cd.myremcrid.getId(), "writedb")) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (!cd.mysemcrid.isValid()) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        String language = job.getRequest().getParameter("lang");
        if (language == null) {
            MCRSession session = MCRSessionMgr.getCurrentSession();
            language = session.getCurrentLanguage();
        }

        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(cd.mysemcrid.getId());
        MCRDirectory dir = (MCRDirectory) node;

        String mypath = dir.getName();

        try {
            ArrayList<String> pic_list = new ArrayList<String>();
            addPicturesToList(dir, pic_list);

            if (searchForMets(dir) == false) {
                // build the mets.file

                MetsModsUtil mmu = new MetsModsUtil();

                Element mets = mmu.init_mets(cd.mysemcrid.getId());
                Element amdSec = mmu.init_amdSec(cd.mysemcrid.getId(), "Bach Archiv Leipzig", "LeipzigLogo", "LeipzigLogoURL", "testreference", "testpresentation");

                mets.addContent(amdSec);

                Element mets2 = mmu.createMetsElement(pic_list, mets, getBaseURL() + "servlets/MCRFileNodeServlet");

                XMLOutputter xmlout = new XMLOutputter();
                String full_mets = xmlout.outputString(mets2);

                // save the builded file to IFS
                try {
                    LOGGER.debug("storing new mets file...");
                    // startTransaction();
                    MCRFile file = new MCRFile("mets.xml", dir);
                    // commitTransaction();
                    ByteArrayInputStream bais = new ByteArrayInputStream(full_mets.getBytes());
                    long sizeDiff = file.setContentFrom(bais, false);
                    // startTransaction();
                    file.storeContentChange(sizeDiff);
                    // commitTransaction();

                } catch (Exception e) {
                    LOGGER.error("Error while storing new mets file...", e);
                    // try {
                    // rollbackTransaction();
                    // } catch (Exception e2) {
                    // LOGGER.debug("Error while rolling back transaction",e);
                    // }

                }

            }

        } catch (Exception e) {
            LOGGER.info("Error while accessing mets-mods", e);
            // e.printStackTrace();
        }

        StringBuffer sb = new StringBuffer(getBaseURL()).append("receive/").append(cd.myremcrid.getId());
        MCRSWFUploadHandlerIFS fuh = new MCRSWFUploadHandlerIFS(cd.myremcrid.getId(), cd.mysemcrid.getId(), sb.toString());
        String fuhid = fuh.getID();

        cd.myfile = pagedir + "metsmods_commit.xml";

        Properties params = new Properties();
        params.put("XSL.UploadID", fuhid);
        params.put("XSL.target.param.1", "method=formBasedUpload");
        params.put("XSL.target.param.2", "uploadId=" + fuhid);
        params.put("XSL.UploadPath", mypath + "/");
        params.put("XSL.language", language);
        params.put("mcrid", cd.mysemcrid.getId());
        params.put("type", cd.mytype);
        params.put("step", cd.mystep);
        params.put("remcrid", cd.myremcrid.getId());
        String base = getBaseURL() + cd.myfile;
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));
    }

    protected void startTransaction() {
        LOGGER.debug("Starting transaction");
        if (tx == null || !tx.isActive())
            tx = MCRHIBConnection.instance().getSession().beginTransaction();
        else
            throw new MCRException("Transaction already started");
    }

    protected void commitTransaction() {
        LOGGER.debug("Committing transaction");
        if (tx != null) {
            tx.commit();
            tx = null;
        } else
            throw new NullPointerException("Cannot commit transaction");
    }

    protected void rollbackTransaction() {
        LOGGER.debug("Rolling back transaction");
        if (tx != null) {
            tx.rollback();
            tx = null;
        } else
            throw new NullPointerException("Cannot rollback transaction");
    }
}
