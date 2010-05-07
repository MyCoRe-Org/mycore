/*
 * $Id$
 * $Revision: 5697 $ $Date: 27.04.2010 $
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

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.mets.model.MCRMETSGenerator;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRMETSServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRMETSServlet.class);

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        LOGGER.info(job.getRequest().getPathInfo());
        String derivate = getOwnerID(job.getRequest());
        MCRDirectory dir = MCRDirectory.getRootDirectory(derivate);
        MCRFilesystemNode metsFile = dir.getChildByPath("mets.xml");
        job.getRequest().setAttribute("XSL.derivateID", derivate);
        job.getRequest().setAttribute("XSL.objectID", MCRLinkTableManager.instance().getSourceOf(derivate).iterator().next());
        if (metsFile != null) {
            MCRLayoutService.instance().doLayout(job.getRequest(), job.getResponse(), ((MCRFile) metsFile).getContentAsInputStream());
        } else {
            Document mets = MCRMETSGenerator.getMETS(dir);
            MCRLayoutService.instance().doLayout(job.getRequest(), job.getResponse(), mets);
        }
    }

    protected static String getOwnerID(HttpServletRequest request) {
        String pI = request.getPathInfo();
        StringBuilder ownerID = new StringBuilder(request.getPathInfo().length());
        boolean running = true;
        for (int i = (pI.charAt(0) == '/') ? 1 : 0; (i < pI.length() && running); i++) {
            switch (pI.charAt(i)) {
            case '/':
                running = false;
                break;
            default:
                ownerID.append(pI.charAt(i));
                break;
            }
        }
        return ownerID.toString();
    }

}
