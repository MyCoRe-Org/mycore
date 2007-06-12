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
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet implements the server side of communication with the upload
 * applet. The content of the uploaded files are handled by a MCRUploadHandler
 * subclass.
 * 
 * @author Frank Lützenkirchen
 * @author Harald Richter
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 * @see org.mycore.frontend.fileupload.MCRUploadHandler
 */
public final class MCRUploadServlet extends MCRServlet implements Runnable {
    static String serverIP;

    static int serverPort;

    static ServerSocket server;

    static Logger LOGGER = Logger.getLogger(MCRUploadServlet.class);

    static MCRCache sessionIDs = new MCRCache(100, "UploadServlet Upload sessions");

    public synchronized void init() throws ServletException {
        super.init();

        if (server != null)
            return; // already inited?

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
            new Thread(this).start();
        } catch (Exception ex) {
            if (ex instanceof MCRException) {
                throw (MCRException) ex;
            }

            String msg = "Exception while opening file upload server port";
            throw new MCRConfigurationException(msg, ex);
        }
    }

    public void finalize() throws Throwable {
        try {
            if (server != null) {
                server.close();
            }
        } catch (Exception ignored) {
        }

        super.finalize();
    }

    public void handleUpload(Socket socket) {
        LOGGER.info("Client applet connected to socket now.");

        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            ZipInputStream zis = new ZipInputStream(socket.getInputStream());

            LOGGER.debug("Constructed ZipInputStream and DataOutputStream, receiving data soon.");

            ZipEntry ze = zis.getNextEntry();
            String path = URLDecoder.decode(ze.getName(), "UTF-8");
            String extra = new String(ze.getExtra(), "UTF-8");
            String[] parts = extra.split("\\s");
            String md5 = parts[0];
            long length = Long.parseLong(parts[1]);
            String uploadId = parts[2];
            
            LOGGER.debug("Received uploadID = " + uploadId);
            LOGGER.debug("Received path     = " + path);
            LOGGER.debug("Received length   = " + length);
            LOGGER.debug("Received md5      = " + md5);

            // Remember current MCRSession for upload
            String sessionID = (String) (sessionIDs.get(uploadId));
            if (sessionID != null) {
                MCRSession session = MCRSessionMgr.getSession(sessionID);
                if (session != null)
                    MCRSessionMgr.setCurrentSession(session);
            }
            long numBytesStored = MCRUploadHandlerManager.getHandler(uploadId).receiveFile(path, zis, length, md5);

            LOGGER.debug("Stored incoming file content");

            dos.writeLong(numBytesStored);
            dos.flush();

            LOGGER.info("File transfer completed successfully.");
        } catch (Exception ex) {
            LOGGER.error("Exception while receiving and storing file content from applet:", ex);
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception ignored) {
            }

            LOGGER.debug("Socket closed.");
        }
    }

    public void run() {
        LOGGER.debug("Server socket thread startet.");

        while (true) {
            LOGGER.debug("Listening on " + serverIP + ":" + serverPort + " for incoming data...");

            try {
                final Socket socket = server.accept();
                Thread handlerThread = new Thread(new Runnable() {
                    public void run() {
                        handleUpload(socket);
                    }
                });
                handlerThread.start();
            } catch (Exception ex) {
                LOGGER.error("Exception while waiting for client connect to socket", ex);
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
        } else if (method.equals("startUploadSession")) {
            String uploadId = req.getParameter("uploadId");
            int numFiles = Integer.parseInt(req.getParameter("numFiles"));
            MCRUploadHandlerManager.getHandler(uploadId).startUpload(numFiles);

            // Remember current MCRSession for upload
            String sessionID = MCRSessionMgr.getCurrentSession().getID();
            sessionIDs.put(uploadId, sessionID);

            LOGGER.info("UploadServlet start session " + uploadId);
            sendResponse(res, "OK");
        } else if (method.equals("uploadFile")) {
            final String path = req.getParameter("path");

            LOGGER.info("UploadServlet uploading " + path);

            String uploadId = req.getParameter("uploadId");
            String md5 = req.getParameter("md5");
            long length = Long.parseLong(req.getParameter("length"));
            
            LOGGER.debug("UploadServlet receives file " + path + " (" + length + " bytes)" +" with md5 " + md5);

            if (!MCRUploadHandlerManager.getHandler(uploadId).acceptFile(path, md5, length)) {
                LOGGER.debug("Skipping file " + path);
                sendResponse(res, "skip file");

                return;
            }

            LOGGER.debug("Applet wants to send content of file " + path);
            sendResponse(res, serverIP + ":" + serverPort);
        } else if (method.equals("endUploadSession")) {
            String uploadId = req.getParameter("uploadId");
            MCRUploadHandler uploadHandler = MCRUploadHandlerManager.getHandler(uploadId);
            uploadHandler.finishUpload();
            sendResponse(res, "OK");
        } else if (method.equals("cancelUploadSession")) {
            String uploadId = req.getParameter("uploadId");
            MCRUploadHandler uploadHandler = MCRUploadHandlerManager.getHandler(uploadId);
            uploadHandler.cancelUpload();
            sendResponse(res, "OK");
        } else if (method.equals("formBasedUpload")) {
            String uploadId = parms.getParameter("uploadId");
            MCRUploadHandler handler = MCRUploadHandlerManager.getHandler(uploadId);

            LOGGER.info("UploadHandler form based file upload for ID " + uploadId);

            Element uploads = sub.getXML().getRootElement();
            List paths = uploads.getChildren("path");

            if ((paths != null) && (paths.size() >= 0)) {
                int numFiles = paths.size();
                LOGGER.info("UploadHandler uploading " + numFiles + " file(s)");
                handler.startUpload(numFiles);

                for (int i = 0; i < numFiles; i++) {
                    FileItem item = sub.getFile(paths.get(i));
                    
                    InputStream in = item.getInputStream();
                    String path = ((Element) (paths.get(i))).getTextTrim();
                    path = getFileName(path);

                    LOGGER.info("UploadServlet uploading " + path);
                    if (path.toLowerCase().endsWith(".zip")) {
                        uploadZipFile(handler, in);
                    } else
                        handler.receiveFile(path, in, 0, null);
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
            handler.receiveFile(path, zis, 0, null);
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
}
