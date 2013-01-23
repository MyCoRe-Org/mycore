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
import java.util.Collections;
import java.util.Properties;
import java.util.StringTokenizer;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.fileupload.MCRSWFUploadHandlerIFS;
import org.mycore.frontend.metsmods.MCRMetsModsUtil;
import org.mycore.frontend.servlets.MCRServletJob;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * 
 * 
 * @author Stefan Freitag
 * @author Jens Kupferschmidt
 * @version $Revision: 1.12 $ $Date: 2009/03/06 09:18:23 $
 */

public class MCRStartMetsModsServlet extends MCRStartEditorServlet {

    private static final long serialVersionUID = -6409340238736582208L;

    private static String metsfile = CONFIG.getString("MCR.MetsMots.ConfigFile", "mets.xml");

    private static String known_image_list = CONFIG.getString("MCR.Component.MetsMods.allowed", "");

    private static String activated = CONFIG.getString("MCR.Component.MetsMods.activated", "");

    public void seditmets(MCRServletJob job, CommonData cd) throws IOException {

        if (!MCRAccessManager.checkPermission(cd.myremcrid.toString(), "writedb")) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid == null) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        String language = job.getRequest().getParameter("lang");
        if (language == null) {
            MCRSession session = MCRSessionMgr.getCurrentSession();
            language = session.getCurrentLanguage();
        }

        MCRFilesystemNode node = MCRFilesystemNode.getRootNode(cd.mysemcrid.toString());
        MCRDirectory dir = (MCRDirectory) node;
        String mypath = dir.getName();

        if (!metsExist(dir)) {
            ArrayList<String> pic_list = new ArrayList<String>();
            addPicturesToList(dir, pic_list);
            boolean allowed_was_found = getAllowedWasFound(dir, pic_list);
            if (!allowed_was_found) {
                job.getResponse().sendRedirect(
                        job.getResponse().encodeRedirectURL(
                                buildRedirectURL(getBaseURL() + "servlets/MCRFileNodeServlet/" + cd.mysemcrid.toString() + "/?hosts=local", new Properties())));
            }
            Collections.sort(pic_list);

            MCRMetsModsUtil mmu = new MCRMetsModsUtil();
            Element new_mets_file = mmu.createNewMetsFile(cd.mysemcrid.toString());
            if (activated.contains("CONTENTIDS"))
                new_mets_file = mmu.createMetsElement(pic_list, new_mets_file, getBaseURL() + "servlets/MCRFileNodeServlet", getBaseURL() + "receive/"
                        + cd.myremcrid.toString());
            else
                new_mets_file = mmu.createMetsElement(pic_list, new_mets_file, getBaseURL() + "servlets/MCRFileNodeServlet");

            if (LOGGER.isDebugEnabled()) {
                MCRUtils.writeElementToSysout(new_mets_file);
            }
            try {
                XMLOutputter xmlout = new XMLOutputter(Format.getPrettyFormat());
                String full_mets = xmlout.outputString(new_mets_file);
                LOGGER.debug("storing new mets file...");
                MCRFile file = new MCRFile(metsfile, dir);
                ByteArrayInputStream bais = new ByteArrayInputStream(full_mets.getBytes());
                long sizeDiff = file.setContentFrom(bais, false);
                file.storeContentChange(sizeDiff);
            } catch (Exception e) {
                if (LOGGER.isDebugEnabled()) {
                    e.printStackTrace();
                } else {
                    LOGGER.error("Error while storing new mets file...", e);
                }
            }

        }

        String sb = getBaseURL() + "receive/" + cd.myremcrid.toString();
        MCRSWFUploadHandlerIFS fuh = new MCRSWFUploadHandlerIFS(cd.myremcrid.toString(), cd.mysemcrid.toString(), sb.toString());
        String fuhid = fuh.getID();

        cd.myfile = pagedir + "metsmods_commit.xml";

        Properties params = new Properties();
        params.put("XSL.UploadID", fuhid);
        params.put("XSL.target.param.1", "method=formBasedUpload");
        params.put("XSL.target.param.2", "uploadId=" + fuhid);
        params.put("XSL.UploadPath", mypath + "/");
        params.put("XSL.language", language);
        params.put("mcrid", cd.mysemcrid.toString());
        params.put("type", cd.mytype);
        params.put("step", cd.mystep);
        params.put("remcrid", cd.myremcrid.toString());
        String base = getBaseURL() + cd.myfile;
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));

    }

    private boolean metsExist(MCRDirectory dir) {
        MCRFile mcrfile = (MCRFile) dir.getChild(metsfile);
        return mcrfile != null;
    }

    private void addPicturesToList(MCRDirectory dir, ArrayList<String> list) {
        for (int i = 0; i < dir.getChildren().length; i++) {
            try {
                dir = (MCRDirectory) dir.getChildren()[i];
                addPicturesToList(dir, list);
            } catch (Exception ClassCastException) {
                String str = dir.getPath() + "/" + ((MCRFile) dir.getChildren()[i]).getName();
                if (!list.contains(str))
                    if (known_image_list.contains(",")) {
                        StringTokenizer st1 = new StringTokenizer(known_image_list, ",");
                        while (st1.hasMoreTokens())
                            if (searchForAllowed(dir, st1.nextToken())) {
                                list.add(str);
                                break;
                            }
                    } else if (searchForAllowed(dir, known_image_list))
                        list.add(str);
            }
        }
    }

    private boolean getAllowedWasFound(MCRDirectory dir, ArrayList<String> pic_list) {
        boolean allowed_was_found = false;
        if (known_image_list.contains(",")) {
            StringTokenizer st1 = new StringTokenizer(known_image_list, ",");
            while (st1.hasMoreTokens())
                if (searchForAllowed(dir, st1.nextToken())) {
                    allowed_was_found = true;
                    break;
                }
        } else {
            if (searchForAllowed(dir, known_image_list))
                allowed_was_found = true;
        }
        return allowed_was_found;
    }

    private boolean searchForAllowed(MCRDirectory dir, String allowed) {
        if (allowed.compareTo("") == 0)
            return false;
        MCRFilesystemNode liste[] = dir.getChildren();
        for (MCRFilesystemNode aListe : liste) {
            if (aListe instanceof MCRFile) {
                if (aListe.getName().contains(allowed))
                    return true;
            }
        }
        return false;
    }

}
