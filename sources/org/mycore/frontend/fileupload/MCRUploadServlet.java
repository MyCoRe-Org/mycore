/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.frontend.fileupload;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.*;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet implements the server side of communication with the upload
 * applet The content of the uploaded files is handled by an upload handler
 * derived from AppletCommunicator
 * 
 * @author Frank Lützenkirchen
 * @author Harald Richter
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 * @see org.mycore.frontend.fileupload.MCRMCRUploadHandlerInterface
 */
public final class MCRUploadServlet extends MCRServlet implements Runnable {

    static String serverIP;

    static int serverPort;

    static ServerSocket server;

    static Logger LOGGER = Logger.getLogger(MCRUploadServlet.class);

    public void init() throws ServletException {
        super.init();
        try {
            String host = new java.net.URL(getBaseURL()).getHost();
            String defIP = InetAddress.getByName(host).getHostAddress();
            int defPort = 22471; // my birthday is the default upload port
            serverIP = CONFIG.getString("MCR.FileUpload.IP", defIP);
            serverPort = CONFIG.getInt("MCR.FileUpload.Port", defPort);

            LOGGER.info("Opening server socket: ip=" + serverIP + " port="
                    + serverPort);
            server = new ServerSocket(serverPort, 1, InetAddress
                    .getByName(serverIP));
            LOGGER.debug("Server socket successfully created.");

            // Starts separate thread that will receive and store file content
            new Thread(this).start();
        } catch (Exception ex) {
            if (ex instanceof MCRException)
                throw (MCRException) ex;

            String msg = "Exception while opening file upload server port";
            throw new MCRConfigurationException(msg, ex);
        }
    }

    public void finalize() throws Throwable {
        try {
            if (server != null)
                server.close();
        } catch (Exception ignored) {
        }
        super.finalize();
    }

    public void run() {
        LOGGER.debug("Server socket thread startet.");
        while (true) {
            Socket socket = null;

            try {
                LOGGER.debug("Listening on " + serverIP + ":" + serverPort
                        + " for incoming data...");
                socket = server.accept();

                LOGGER.info("Client applet connected to socket now.");

                DataOutputStream dos = new DataOutputStream(socket
                        .getOutputStream());
                ZipInputStream zis = new ZipInputStream(socket.getInputStream());

                LOGGER
                        .debug("Constructed ZipInputStream and DataOutputStream, receiving data soon.");

                ZipEntry ze = zis.getNextEntry();
                String path = URLDecoder.decode(ze.getName(), "UTF-8");
                String uploadId = ze.getComment();

                MCRUploadHandlerManager.instance().getHandle(uploadId)
                        .receiveFile(path, zis);
                LOGGER.debug("Stored incoming file content");

                dos.writeUTF("OK");
                dos.flush();

                LOGGER.info("File transfer completed successfully.");
            } catch (Exception ex) {
                LOGGER
                        .error(
                                "Exception while receiving and storing file content from applet:",
                                ex);
            } finally {
                try {
                    if (socket != null)
                        socket.close();
                } catch (Exception ignored) {
                }

                LOGGER.debug("Socket closed.");
            }
        }
    }

    public void doGetPost(MCRServletJob job) throws Exception {
        try {
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

        String method = req.getParameter("method");

        if (method.equals("redirecturl")) {
            String uploadId = req.getParameter("uploadId");
            String url = MCRUploadHandlerManager.instance().getHandle(uploadId)
                    .getRedirectURL();
            LOGGER.info("REDIRECT " + url);
            res.sendRedirect(url);
            return;
        } else if (method.equals("startDerivateSession String int")) {
            String uploadId = req.getParameter("uploadId");
            int numFiles = Integer.parseInt(req.getParameter("numFiles"));
            MCRUploadHandlerManager.instance().getHandle(uploadId).startUpload(
                    numFiles);
            LOGGER.info("MCRUploadServlet start session " + uploadId);
            sendResponse(res, "OK");
        } else if (method.equals("createFile String")) {
            final String path = req.getParameter("path");

            LOGGER.info("UploadServlet uploading " + path);
            final String uploadId = req.getParameter("uploadId");
            final String md5 = req.getParameter("md5");

            LOGGER.debug("MCRUploadServlet receives file " + path
                    + " with md5 " + md5);
            if (!MCRUploadHandlerManager.instance().getHandle(uploadId)
                    .acceptFile(path, md5)) {
                LOGGER.debug("Skip file " + path);
                sendResponse(res, "skip file");
                return;
            }

            LOGGER.debug("Applet wants to send content of file " + path);
            sendResponse(res, serverIP + ":" + serverPort);

        } else if (method.equals("endDerivateSession String")) {
            String uploadId = req.getParameter("uploadId");
            MCRUploadHandlerInterface uploadHandler = MCRUploadHandlerManager
                    .instance().getHandle(uploadId);
            uploadHandler.finishUpload();
            sendResponse(res, "OK");
        }
    }

    protected void sendException(HttpServletResponse res, Exception ex)
            throws Exception {
        HashMap response = new HashMap();
        response.put("clname", ex.getClass().getName());
        response.put("strace", MCRException.getStackTraceAsString(ex));
        if (ex.getLocalizedMessage() != null)
            response.put("message", ex.getLocalizedMessage());
        sendResponse(res, "upload/exception", response);
    }

    protected void sendResponse(HttpServletResponse res, Object value)
            throws Exception {
        HashMap parameters = new HashMap();
        if (value != null)
            parameters.put("return", value);
        sendResponse(res, "upload/response", parameters);
    }

    protected void sendResponse(HttpServletResponse res, String mime,
            Map parameters) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        DataOutputStream dos = new DataOutputStream(baos);
        Set entries = parameters.entrySet();

        dos.writeUTF(mime);
        Iterator it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            //write out key
            dos.writeUTF(entry.getKey().toString());
            //write out class
            dos.writeUTF(entry.getValue().getClass().getName());

            if (entry.getValue().getClass() == Integer.class)
                dos.writeInt(((Integer) (entry.getValue())).intValue());
            else
                dos.writeUTF(entry.getValue().toString());
        }
        dos.close();

        byte[] response = baos.toByteArray();
        res.setContentType(mime);
        res.setContentLength(response.length);
        OutputStream out = res.getOutputStream();
        out.write(response, 0, response.length);
        out.close();
        res.flushBuffer();
    }
}