/*
 * 
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

package org.mycore.frontend.fileupload;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet implements the server side of communication with the upload applet. The content of the uploaded files
 * are handled by a MCRUploadHandler subclass.
 * 
 * @author Frank Lützenkirchen
 * @author Harald Richter
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * 
 * @see org.mycore.frontend.fileupload.MCRUploadHandler
 */
public final class MCRUploadViaAppletServlet extends MCRServlet {

    private static final long serialVersionUID = -1452027276006825044L;

    private static final Logger LOGGER = Logger.getLogger(MCRUploadViaAppletServlet.class);

    private static MCRUploadViaAppletServer uploadServer;

    private void initServer() throws ServletException {
        synchronized (MCRUploadViaAppletServlet.class) {
            if (uploadServer == null)
                uploadServer = new MCRUploadViaAppletServer(MCRFrontendUtil.getBaseURL());
        }
    }

    @Override
    public void destroy() {
        try {
            if (uploadServer != null)
                uploadServer.close();
        } catch (Exception ignored) {
            LOGGER.warn("Exception while destroy() in MCRUploadServlet !!!");
        }
        super.destroy();
    }

    @Override
    public void doGetPost(MCRServletJob job) throws Exception {
        try {
            initServer();
            invokeMethod(job);
        } catch (Exception ex) {
            LOGGER.error("Error while handling FileUpload", ex);
            sendException(job.getResponse(), ex);
            throw ex;
        }
    }

    protected void invokeMethod(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        String method = job.getRequest().getParameter("method");

        if (MCRWebsiteWriteProtection.isActive())
            throw new MCRException("System is currently in read-only mode");
        else if (method.equals("redirecturl"))
            redirect(req, res);
        else if (method.equals("startUploadSession"))
            startUploadSession(req, res);
        else if (method.equals("uploadFile"))
            uploadFile(req, res);
        else if (method.equals("ping"))
            sendResponse(res, "pong");
        else if (method.equals("endUploadSession"))
            endUploadSession(req, res);
        else if (method.equals("cancelUploadSession"))
            cancelUploadSession(req, res);
        else
            LOGGER.error("Unknown parameters, don't know what to do: " + job.getRequest().getQueryString());
    }

    private void redirect(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String uploadId = req.getParameter("uploadId");
        MCRUploadHandler handler = MCRUploadHandlerManager.getHandler(uploadId);
        String url = handler.getRedirectURL();
        LOGGER.info("UploadServlet redirect to " + url);
        res.sendRedirect(res.encodeRedirectURL(url));
        handler.unregister();
    }

    private void startUploadSession(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String uploadId = req.getParameter("uploadId");
        if (uploadId == null) {
            StringBuilder sb = new StringBuilder("'uploadId' was not submitted. Request:\n");
            Enumeration<String> headerNames = req.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String header = headerNames.nextElement();
                Enumeration<String> headerValues = req.getHeaders(header);
                while (headerValues.hasMoreElements()) {
                    sb.append(header).append(':').append(headerValues.nextElement()).append('\n');
                }
            }
            throw new MCRException(sb.toString());
        }
        int numFiles = Integer.parseInt(req.getParameter("numFiles"));
        MCRUploadHandlerManager.getHandler(uploadId).startUpload(numFiles);

        LOGGER.info("UploadServlet start session " + uploadId);
        sendResponse(res, "OK");
    }

    private void uploadFile(HttpServletRequest req, HttpServletResponse res) throws Exception {
        final String path = req.getParameter("path");

        LOGGER.info("UploadServlet uploading " + path);
        MCRUploadHelper.checkPathName(path);

        String uploadId = req.getParameter("uploadId");
        String md5 = req.getParameter("md5");
        long length = Long.parseLong(req.getParameter("length"));

        LOGGER.debug("UploadServlet receives file " + path + " (" + length + " bytes)" + " with md5 " + md5);

        if (MCRUploadHandlerManager.getHandler(uploadId).acceptFile(path, md5, length)) {
            LOGGER.debug("Applet wants to send content of file " + path);
            sendResponse(res, uploadServer.getServerIP() + ":" + uploadServer.getServerPort());
        } else {
            LOGGER.debug("Skipping file " + path);
            sendResponse(res, "skip file");
        }
    }

    private void endUploadSession(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String uploadId = req.getParameter("uploadId");
        MCRUploadHandler uploadHandler = MCRUploadHandlerManager.getHandler(uploadId);
        uploadHandler.finishUpload();
        sendResponse(res, "OK");
    }

    private void cancelUploadSession(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String uploadId = req.getParameter("uploadId");
        MCRUploadHandler uploadHandler = MCRUploadHandlerManager.getHandler(uploadId);
        uploadHandler.cancelUpload();
        sendResponse(res, "OK");
    }

    protected void sendException(HttpServletResponse res, Exception ex) throws Exception {
        HashMap<String, String> response = new HashMap<>();
        response.put("clname", ex.getClass().getName());
        response.put("strace", MCRException.getStackTraceAsString(ex));

        if (ex.getLocalizedMessage() != null)
            response.put("message", ex.getLocalizedMessage());

        sendResponse(res, "upload/exception", response);
    }

    protected void sendResponse(HttpServletResponse res, String value) throws Exception {
        HashMap<String, String> parameters = new HashMap<>();

        if (value != null)
            parameters.put("return", value);

        sendResponse(res, "upload/response", parameters);
    }

    protected void sendResponse(HttpServletResponse res, String mimeType, Map<String, String> parameters)
        throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeUTF(mimeType);

        for (Entry<String, String> entry : parameters.entrySet()) {
            dos.writeUTF(entry.getKey().toString());
            dos.writeUTF(entry.getValue().getClass().getName());
            dos.writeUTF(entry.getValue().toString());
        }

        dos.close();

        byte[] response = baos.toByteArray();
        res.setContentType(mimeType);
        res.setContentLength(response.length);

        OutputStream out = res.getOutputStream();
        out.write(response, 0, response.length);
        out.close();
        res.flushBuffer();
    }
}
