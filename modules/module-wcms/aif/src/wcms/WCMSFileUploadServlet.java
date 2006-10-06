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

package wcms;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

public class WCMSFileUploadServlet extends WCMSServlet {
    private static final long serialVersionUID = 1L;

    private static final File DOCUMENT_DIR = new File(CONFIG.getString("MCR.WCMS.documentPath").replace('/', File.separatorChar));

    private static final File IMAGE_DIR = new File(CONFIG.getString("MCR.WCMS.imagePath").replace('/', File.separatorChar));

    private static final Logger LOGGER = Logger.getLogger(WCMSFileUploadServlet.class);

    /**
     * Main program called by doGet and doPost.
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean isMultiPart = ServletFileUpload.isMultipartContent(request);

        if (isMultiPart) {
            int fileMaxSize = CONFIG.getInt("MCR.WCMS.maxUploadFileSize");

            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setSizeMax(fileMaxSize);

            // parse the request
            try {
                List items = upload.parseRequest(request);
                for (Iterator it = items.iterator(); it.hasNext();) {
                    FileItem item = (FileItem) it.next();
                    if (item.isFormField()) {
                        processFormField(item);
                    } else {
                        processUploadedFile(item);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error while getting uploaded file.", e);
                generateStatusPage(request, response, false, e.getLocalizedMessage());
            }
            generateStatusPage(request, response, true, null);
        } else {
            generateUploadPage(request, response);
        }
    }

    private void generateStatusPage(HttpServletRequest request, HttpServletResponse response, boolean success, String message) throws ServletException,
            IOException {
        String msg = (message != null) ? message : "";
        String status = (success) ? "done" : "failed";
        forwardPage(request, response, status, msg);
    }

    private void generateUploadPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        forwardPage(request, response, "upload", "2");
    }

    private void forwardPage(HttpServletRequest request, HttpServletResponse response, String status, String error) throws ServletException, IOException {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        Element rootOut = new Element("cms");
        Document jdom = new Document(rootOut);
        rootOut.addContent(new Element("session").setText("fileUpload"));
        rootOut.addContent(new Element("userID").setText(mcrSession.get("userID").toString()));
        rootOut.addContent(new Element("userClass").setText(mcrSession.get("userClass").toString()));
        rootOut.addContent(new Element("status").setText(status));
        rootOut.addContent(new Element("error").setText(error));
        request.setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(request, response);
    }

    private void processFormField(FileItem item) {
        String name = item.getFieldName();
        String value = item.getString();
        LOGGER.info("Got form field " + name + "=" + value);
    }

    private void processUploadedFile(FileItem item) throws Exception {
        String fileName = FilenameUtils.getName(item.getName());
        String contentType = item.getContentType();
        if (contentType.startsWith("image")) {
            File imageFile = new File(IMAGE_DIR, fileName);
            item.write(imageFile);
        } else {
            File docFile = new File(DOCUMENT_DIR, fileName);
            item.write(docFile);
        }
        LOGGER.info("Got file " + fileName);
    }
}
