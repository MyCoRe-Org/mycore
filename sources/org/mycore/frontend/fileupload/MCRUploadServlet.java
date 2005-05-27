/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * Copyright (C) 2000 University of Essen, Germany
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
 * along with this program, normally in the file documentation/license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.frontend.fileupload;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
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
public final class MCRUploadServlet extends MCRServlet {

    static Logger LOGGER = Logger.getLogger(MCRUploadServlet.class);

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
        } else if (method.equals("startDerivateSession String")) {
            String uploadId = req.getParameter("uploadId");
            uploadId = MCRUploadHandlerManager.instance().getHandle(uploadId)
                    .startUpload();
            LOGGER.info("MCRUploadServlet start session " + uploadId);
            sendResponse(res, uploadId);
        } else if (method.equals("createFile String")) {
            final String path = req.getParameter("path");

            LOGGER.info("PATH: " + path);
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

            String servername = req.getServerName();

            LOGGER.debug("Applet wants to send content of file " + path);
            LOGGER
                    .debug("Next trying to create a server socket for file transfer...");

            final ServerSocket server = new ServerSocket(0, 1, InetAddress
                    .getByName(servername));
            LOGGER.debug("Server socket successfully created.");

            final int port = server.getLocalPort();
            final String host = server.getInetAddress().getHostAddress();

            LOGGER.debug("Informing applet that server socket is ready.");

            sendResponse(res, host + ":" + port);

            // Define a separate Thread that accepts the file content,
            // otherwise Tomcat will not finish the HTTP response correctly
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    Socket socket = null;

                    try {
                        LOGGER.debug("Listening on " + host + ":" + port
                                + " for incoming data...");
                        socket = server.accept();

                        LOGGER.debug("Client applet connected to socket now.");

                        DataOutputStream dos = new DataOutputStream(socket
                                .getOutputStream());
                        ZipInputStream zis = new ZipInputStream(socket
                                .getInputStream());

                        LOGGER
                                .debug("Constructed ZipInputStream and DataOutputStream, receiving data soon.");

                        zis.getNextEntry();

                        String erg = MCRUploadHandlerManager.instance()
                                .getHandle(uploadId).receiveFile(path, zis);
                        LOGGER.debug("Stored incoming file content under "
                                + erg);
                        LOGGER
                                .debug("Informing applet about location where content was stored...");

                        dos.writeUTF(erg);

                        LOGGER.debug("Sended acknowledgement to applet.");
                        LOGGER.debug("File transfer completed successfully.");
                    } catch (Exception ex) {
                        LOGGER
                                .error(
                                        "Exception while receiving and storing file content from applet:",
                                        ex);
                    } finally {
                        try {
                            if (socket != null)
                                socket.close();
                            if (server != null)
                                server.close();
                        } catch (Exception ignored) {
                        }

                        LOGGER.debug("Socket closed.");
                    }
                }
            });

            thread.start(); // Starts separate thread that will receive and
                            // store file content
        } else if (method.equals("endDerivateSession String")) {
            String uploadId = req.getParameter("uploadId");
            MCRUploadHandlerInterface uploadHandler = MCRUploadHandlerManager
                    .instance().getHandle(uploadId);
            uploadHandler.finishUpload();
            sendResponse(res, "upload finished.");
        }
    }

    protected void sendException(HttpServletResponse res, Exception ex)
            throws Exception {
        HashMap response = new HashMap();
        response.put("clname", ex.getClass().getName());
        response.put("strace", getStackTrace(ex));
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

    protected String getStackTrace(Exception ex) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            pw.close();
            return sw.toString();
        } catch (Exception ex2) {
        }
        return null;
    }
}