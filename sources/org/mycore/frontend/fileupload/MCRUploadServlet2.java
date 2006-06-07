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

package org.mycore.frontend.fileupload;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.jdom.Element;

import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet implements the server side of communication with the upload
 * applet. The content of the uploaded files are handled by a MCRUploadHandler
 * subclass. This servlet is - oposed to MCRUploadServlet - manageable and can
 * be cleanly destroyed. As it is not as thouroughly tested as MCRUploadServlet
 * yet, use it at own risk.
 * 
 * @author Thomas Scheffler (yagee)
 * @author Frank Lützenkirchen
 * @author Harald Richter
 * @version $Revision$ $Date$
 * @see org.mycore.frontend.fileupload.MCRUploadHandler
 * @see org.mycore.frontend.fileupload.MCRUploadServlet
 * @see javax.servlet.GenericServlet#destroy()
 */
public final class MCRUploadServlet2 extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRUploadServlet2.class);

    private static String serverIP;

    private static int serverPort;

    private static ServerSocket server;

    private static UploadThread uploadThread;

    public void init() throws ServletException {
        super.init();

        try {
            String host = new java.net.URL(getBaseURL()).getHost();
            String defIP = InetAddress.getByName(host).getHostAddress();
            int defPort = 22471; // my birthday is the default upload port
            serverIP = CONFIG.getString("MCR.FileUpload.IP", defIP);
            serverPort = CONFIG.getInt("MCR.FileUpload.Port", defPort);

            LOGGER.info("Opening server socket: ip=" + serverIP + " port=" + serverPort);
            server = new ServerSocket(serverPort, 1, InetAddress.getByName(serverIP));
            LOGGER.debug("Server socket successfully created.");

            // Starts separate thread that will receive and store file content
            uploadThread = new UploadThread();
            new Thread(uploadThread).start();
        } catch (Exception ex) {
            if (ex instanceof MCRException) {
                throw (MCRException) ex;
            }

            String msg = "Exception while opening file upload server port";
            throw new MCRConfigurationException(msg, ex);
        }
    }

    public void destroy() {
        uploadThread.shutDown();
        try {
            if (server != null) {
                server.close();
            }
        } catch (Exception ignored) {
            LOGGER.info("Server closed.", ignored);
        }
        server = null;
        super.destroy();
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

        MCREditorSubmission sub = (MCREditorSubmission) (req.getAttribute("MCREditorSubmission"));
        MCRRequestParameters parms = (sub == null ? new MCRRequestParameters(req) : sub.getParameters());

        String method = parms.getParameter("method");

        if (method.equals("redirecturl")) {
            String uploadId = req.getParameter("uploadId");
            MCRUploadHandler handler = MCRUploadHandlerManager.getHandler(uploadId);
            String url = handler.getRedirectURL();
            LOGGER.info("UploadServlet redirect to " + url);
            res.sendRedirect(res.encodeRedirectURL(url));
            handler.unregister();

            return;
        } else if (method.equals("startDerivateSession String int")) {
            String uploadId = req.getParameter("uploadId");
            int numFiles = Integer.parseInt(req.getParameter("numFiles"));
            MCRUploadHandlerManager.getHandler(uploadId).startUpload(numFiles);
            LOGGER.info("UploadServlet start session " + uploadId);
            sendResponse(res, "OK");
        } else if (method.equals("createFile String")) {
            final String path = req.getParameter("path");

            LOGGER.info("UploadServlet uploading " + path);

            final String uploadId = req.getParameter("uploadId");
            final String md5 = req.getParameter("md5");
            LOGGER.debug("UploadServlet receives file " + path + " with md5 " + md5);

            if (!MCRUploadHandlerManager.getHandler(uploadId).acceptFile(path, md5)) {
                LOGGER.debug("Skip file " + path);
                sendResponse(res, "skip file");

                return;
            }

            LOGGER.debug("Applet wants to send content of file " + path);
            sendResponse(res, serverIP + ":" + serverPort);
        } else if (method.equals("endDerivateSession String")) {
            String uploadId = req.getParameter("uploadId");
            MCRUploadHandler uploadHandler = MCRUploadHandlerManager.getHandler(uploadId);
            uploadHandler.finishUpload();
            sendResponse(res, "OK");
        } else if (method.equals("formBasedUpload")) {
            String uploadId = parms.getParameter("uploadId");
            MCRUploadHandler handler = MCRUploadHandlerManager.getHandler(uploadId);

            LOGGER.info("UploadHandler form based file upload for ID " + uploadId);

            Element uploads = sub.getXML().getRootElement();
            List paths = uploads.getChildren("path");
            List files = sub.getFiles();

            if ((files != null) && (files.size() >= 0)) {
                int numFiles = files.size();
                LOGGER.info("UploadHandler uploading " + numFiles + " file(s)");
                handler.startUpload(numFiles);

                for (int i = 0; i < numFiles; i++) {
                    FileItem item = (FileItem) (files.get(i));
                    InputStream in = item.getInputStream();
                    String path = ((Element) (paths.get(i))).getTextTrim();
                    path = getFileName(path);

                    LOGGER.info("UploadServlet uploading " + path);
                    if (path.toLowerCase().endsWith(".zip")) {
                        uploadZipFile(handler, in);
                    } else
                        handler.receiveFile(path, in);
                }

                handler.finishUpload();
            }

            String url = handler.getRedirectURL();
            LOGGER.info("UploadServlet redirect to " + url);
            res.sendRedirect(res.encodeRedirectURL(url));
            handler.unregister();

            return;
        }
    }

    private void uploadZipFile(MCRUploadHandler handler, InputStream in) throws IOException, Exception {
        ZipInputStream zis = new ZipInputStream(in);
        ZipEntry entry = null;
        while ((entry = zis.getNextEntry()) != null) {
            String path = entry.getName();

            // Convert absolute paths to relative paths:
            int pos = path.indexOf(":");
            if (pos >= 0)
                path = path.substring(pos + 1);
            while (path.startsWith("\\") || path.startsWith("/"))
                path = path.substring(1);

            if (entry.isDirectory()) {
                LOGGER.debug("UploadServlet skipping ZIP entry " + path + ", is a directory");
                continue;
            }

            LOGGER.info("UploadServlet unpacking ZIP entry " + path);
            handler.receiveFile(path, zis);
        }
    }

    protected String getFileName(String path) {
        int pos = Math.max(path.lastIndexOf('\\'), path.lastIndexOf("/"));
        return path.substring(pos + 1);
    }

    protected void sendException(HttpServletResponse res, Exception ex) throws Exception {
        HashMap response = new HashMap();
        response.put("clname", ex.getClass().getName());
        response.put("strace", MCRException.getStackTraceAsString(ex));

        if (ex.getLocalizedMessage() != null) {
            response.put("message", ex.getLocalizedMessage());
        }

        sendResponse(res, "upload/exception", response);
    }

    protected void sendResponse(HttpServletResponse res, Object value) throws Exception {
        HashMap parameters = new HashMap();

        if (value != null) {
            parameters.put("return", value);
        }

        sendResponse(res, "upload/response", parameters);
    }

    protected void sendResponse(HttpServletResponse res, String mime, Map parameters) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        DataOutputStream dos = new DataOutputStream(baos);
        Set entries = parameters.entrySet();

        dos.writeUTF(mime);

        Iterator it = entries.iterator();

        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            // write out key
            dos.writeUTF(entry.getKey().toString());

            // write out class
            dos.writeUTF(entry.getValue().getClass().getName());

            if (entry.getValue().getClass() == Integer.class) {
                dos.writeInt(((Integer) (entry.getValue())).intValue());
            } else {
                dos.writeUTF(entry.getValue().toString());
            }
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

    private static class UploadThread implements Runnable {
        private HashSet sockets = new HashSet();

        private boolean running = true;

        public void run() {
            LOGGER.debug("Server socket thread startet.");

            while (running) {
                LOGGER.debug("Listening on " + serverIP + ":" + serverPort + " for incoming data...");

                try {
                    final Socket socket = server.accept();
                    sockets.add(socket);
                    Thread handlerThread = new Thread(new ClientCommunicator(socket, this));
                    handlerThread.start();
                } catch (Exception ex) {
                    if (running) {
                        LOGGER.error("Exception while waiting for client connect to socket", ex);
                    } else {
                        LOGGER.info("FileUploadThread cleanly closed");
                    }
                }
            }
        }

        public void shutDown() {
            running = false;// return from run()
            for (Iterator it = sockets.iterator(); it.hasNext();) {
                Socket s = (Socket) it.next();
                try {
                    s.close();
                } catch (IOException e) {
                    LOGGER.info("Exception closing open UploadSocket.", e);
                } finally {
                    it.remove();
                }
            }
        }

        public void closeSocket(Socket socket) {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.info("Exception closing open UploadSocket.", e);
            } finally {
                sockets.remove(socket);
            }
        }
    }

    private static class ClientCommunicator implements Runnable {
        Socket socket;

        UploadThread callback;

        public ClientCommunicator(Socket socket, UploadThread callback) {
            this.socket = socket;
            this.callback = callback;
        }

        public void run() {
            LOGGER.info("Client applet connected to socket now.");

            try {
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                ZipInputStream zis = new ZipInputStream(socket.getInputStream());

                LOGGER.debug("Constructed ZipInputStream and DataOutputStream, receiving data soon.");

                ZipEntry ze = zis.getNextEntry();
                String path = URLDecoder.decode(ze.getName(), "UTF-8");
                String uploadId = new String(ze.getExtra(), "UTF-8");

                LOGGER.debug("Received path = " + path);
                LOGGER.debug("Received uploadID = " + uploadId);

                MCRUploadHandlerManager.getHandler(uploadId).receiveFile(path, zis);

                LOGGER.debug("Stored incoming file content");

                dos.writeUTF("OK");
                dos.flush();

                LOGGER.info("File transfer completed successfully.");
            } catch (Exception ex) {
                LOGGER.error("Exception while receiving and storing file content from applet:", ex);
            } finally {
                try {
                    if (socket != null) {
                        callback.closeSocket(socket);
                    }
                } catch (Exception ignored) {
                }
                socket = null;
                callback = null;
                LOGGER.debug("Socket closed.");
            }
        }
    }
}
