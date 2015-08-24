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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.jdom2.Element;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.streams.MCRNotClosingInputStream;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet implements the server side of communication with the upload applet. The content of the uploaded files
 * are handled by a MCRUploadHandler subclass.
 * 
 * @author Frank Lützenkirchen
 * @author Harald Richter
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 * @see org.mycore.frontend.fileupload.MCRUploadHandler
 */
public final class MCRUploadServlet extends MCRServlet implements Runnable {
    private static final long serialVersionUID = -1452027276006825044L;

    static String serverIP;

    static int serverPort;

    static ServerSocket server;

    static Logger LOGGER = Logger.getLogger(MCRUploadServlet.class);

    final static int bufferSize = 65536; // 64 KByte

    /*
     * reserved URI characters should not be in uploaded filenames see RFC3986,
     * Section 2.2
     */
    static Pattern genDelims = Pattern.compile("[^:?%#\\[\\]@]*");

    static Pattern subDelims = Pattern.compile("[^!%$&'()*+,;=]*");

    static boolean doRun = true;

    private synchronized void initServer(String baseURL) throws ServletException {
        if (server != null) {
            return; // already inited?
        }

        doRun = true;

        try {
            // query property directly (not via getBaseURL()), saves a stalled
            // MCRSession
            String host = new java.net.URL(baseURL).getHost();
            String defIP = InetAddress.getByName(host).getHostAddress();
            serverIP = MCRConfiguration.instance().getString("MCR.FileUpload.IP", defIP);
            serverPort = MCRConfiguration.instance().getInt("MCR.FileUpload.Port");

            LOGGER.info("Opening server socket: ip=" + serverIP + " port=" + serverPort);
            server = new ServerSocket();
            server.setReceiveBufferSize(Math.max(server.getReceiveBufferSize(), bufferSize));
            server.bind(new InetSocketAddress(serverIP, serverPort));
            LOGGER.debug("Server socket successfully created.");
            LOGGER.debug("Server receive buffer size is " + server.getReceiveBufferSize());

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

    @Override
    public void destroy() {
        try {
            if (server != null) {
                server.close();
                doRun = false;
            }
        } catch (Exception ignored) {
            LOGGER.warn("Exception while destroy() in MCRUploadServlet !!!");
        }
        super.destroy();
    }

    public void handleUpload(Socket socket) {
        LOGGER.info("Client applet connected to socket now.");

        try {
            PrintWriter pwOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
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

            // start transaction after MCRSession is initialized
            MCRUploadHandler uploadHandler = MCRUploadHandlerManager.getHandler(uploadId);
            Transaction tx = startTransaction();
            try {
                long numBytesStored = uploadHandler.receiveFile(path, zis, length, md5);
                LOGGER.debug("Stored incoming file content with " + numBytesStored + " bytes");
                pwOut.println(numBytesStored);
                pwOut.flush();
                commitTransaction(tx);
                LOGGER.info("File transfer completed successfully.");
            } catch (Exception exc) {
                LOGGER.error("Error while uploading file: " + path, exc);
                rollbackAnRethrow(tx, exc);
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while receiving and storing file content from applet:", ex);
        } finally {
            if (MCRSessionMgr.hasCurrentSession()) {
                //bug fix for: http://sourceforge.net/p/mycore/bugs/643/
                MCRSessionMgr.releaseCurrentSession();
            }
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

        while (doRun) {
            LOGGER.debug("Listening on " + serverIP + ":" + serverPort + " for incoming data...");

            try {
                final Socket socket = server.accept();
                socket.setReceiveBufferSize(bufferSize);
                socket.setSendBufferSize(bufferSize);
                LOGGER.debug("Socket receive buffer size is " + socket.getReceiveBufferSize());

                Thread handlerThread = new Thread(() -> handleUpload(socket));
                handlerThread.start();
            } catch (Exception ex) {
                LOGGER.error("Exception while waiting for client connect to socket", ex);
            }
        }
    }

    @Override
    public void doGetPost(MCRServletJob job) throws Exception {
        try {
            initServer(MCRFrontendUtil.getBaseURL());
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

        MCREditorSubmission sub = (MCREditorSubmission) req.getAttribute("MCREditorSubmission");
        MCRRequestParameters parms = sub == null ? new MCRRequestParameters(req) : sub.getParameters();

        String method = parms.getParameter("method");

        if (method.equals("redirecturl")) {
            String uploadId = req.getParameter("uploadId");
            MCRUploadHandler handler = MCRUploadHandlerManager.getHandler(uploadId);
            String url = handler.getRedirectURL();
            LOGGER.info("UploadServlet redirect to " + url);
            res.sendRedirect(res.encodeRedirectURL(url));
            handler.unregister();

            return;
        }

        if (MCRWebsiteWriteProtection.isActive()) {
            sendException(res, new MCRException("System is currently in read-only mode"));
            return;
        }

        MCRSession session = MCRSessionMgr.getCurrentSession();
        if (method.equals("startUploadSession")) {
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
        } else if (method.equals("uploadFile")) {
            final String path = req.getParameter("path");

            LOGGER.info("UploadServlet uploading " + path);
            checkPathName(path);

            String uploadId = req.getParameter("uploadId");
            String md5 = req.getParameter("md5");
            long length = Long.parseLong(req.getParameter("length"));

            LOGGER.debug("UploadServlet receives file " + path + " (" + length + " bytes)" + " with md5 " + md5);

            if (!MCRUploadHandlerManager.getHandler(uploadId).acceptFile(path, md5, length)) {
                LOGGER.debug("Skipping file " + path);
                sendResponse(res, "skip file");

                return;
            }

            LOGGER.debug("Applet wants to send content of file " + path);
            sendResponse(res, serverIP + ":" + serverPort);
        } else if (method.equals("ping")) {
            sendResponse(res, "pong");
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

            // look up filenames of editor submission or request parameters
            LinkedHashMap<String, FileItem> paths = sub != null ? getFileItems(sub) : getFileItems(parms);

            if (paths != null && !paths.isEmpty()) {
                int numFiles = paths.size();
                LOGGER.info("UploadHandler uploading " + numFiles + " file(s)");
                handler.startUpload(numFiles);
                session.commitTransaction();

                for (Map.Entry<String, FileItem> entry : paths.entrySet()) {

                    FileItem item = entry.getValue();
                    String path = entry.getKey().trim();

                    InputStream in = item.getInputStream();
                    path = getFileName(path);

                    LOGGER.info("UploadServlet uploading " + path);
                    if (path.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                        uploadZipFile(handler, in);
                    } else {
                        Transaction tx = startTransaction();
                        try {
                            handler.receiveFile(path, in, item.getSize(), null);
                            commitTransaction(tx);
                        } catch (Exception exc) {
                            rollbackAnRethrow(tx, exc);
                        }
                    }
                }
                session.beginTransaction();

                handler.finishUpload();
            }

            String url = handler.getRedirectURL();
            LOGGER.info("UploadServlet redirect to " + url);
            res.sendRedirect(res.encodeRedirectURL(url));
            handler.unregister();

            return;
        }
    }

    /**
     * Extracts all filenames and fileitems of target {@link MCREditorSubmission}.
     * 
     * @param sub
     *            editor submission where are file <code>path</code> elements are submitted
     * @return a new {@link LinkedHashMap} with all extracted filenames and file items
     */
    private LinkedHashMap<String, FileItem> getFileItems(MCREditorSubmission sub) {
        LinkedHashMap<String, FileItem> result = new LinkedHashMap<String, FileItem>();
        Element uploads = sub.getXML().getRootElement();
        List<Element> paths = uploads.getChildren("path");
        for (Element path : paths) {
            result.put(path.getTextTrim(), sub.getFile(path));
        }
        return result;
    }

    /**
     * Extracts all filenames and fileitems of target {@link MCRRequestParameters}.
     * 
     * @param params
     *            request parameters where file <code>path</code> elements are submitted
     * @return a new {@link LinkedHashMap} with all extracted filenames and file items
     */
    private LinkedHashMap<String, FileItem> getFileItems(MCRRequestParameters params) {
        LinkedHashMap<String, FileItem> result = new LinkedHashMap<String, FileItem>();
        Enumeration<String> parameterNames = params.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String path = parameterNames.nextElement().toString();
            String prefix = "/upload/path/";
            if (path.startsWith(prefix)) {
                String filename = path.substring(prefix.length());
                result.put(filename, params.getFileItem(path));
            }
        }
        return result;
    }

    private void uploadZipFile(MCRUploadHandler handler, InputStream in) throws IOException, Exception {
        ZipInputStream zis = new ZipInputStream(in);
        MCRNotClosingInputStream nis = new MCRNotClosingInputStream(zis);
        ZipEntry entry = null;
        while ((entry = zis.getNextEntry()) != null) {
            String path = entry.getName();

            // Convert absolute paths to relative paths:
            int pos = path.indexOf(":");
            if (pos >= 0) {
                path = path.substring(pos + 1);
            }
            while (path.startsWith("\\") || path.startsWith("/")) {
                path = path.substring(1);
            }

            if (entry.isDirectory()) {
                LOGGER.debug("UploadServlet skipping ZIP entry " + path + ", is a directory");
            } else {
                checkPathName(path);
                LOGGER.info("UploadServlet unpacking ZIP entry " + path);
                Transaction tx = startTransaction();
                try {
                    handler.receiveFile(path, nis, entry.getSize(), null);
                    commitTransaction(tx);
                } catch (Exception exc) {
                    rollbackAnRethrow(tx, exc);
                }
            }
        }
        nis.reallyClose();
    }

    /**
     * checks if path contains reserved URI characters or path starts or ends with whitespace. There are some characters
     * that are maybe allowed in file names but are reserved in URIs.
     * 
     * @see <a href="http://tools.ietf.org/html/rfc3986#section-2.2">RFC3986, Section 2.2</a>
     * @param path
     *            complete path name
     * @throws MCRException
     *             if path contains 'gen-delims' or 'sub-delims'
     */
    protected static void checkPathName(String path) throws MCRException {
        if (!genDelims.matcher(path).matches()) {
            String delims = "\":\" / \"/\" / \"?\" / \"#\" / \"[\" / \"]\" / \"@\" \"%\"";
            throw new MCRException("Path name " + path + " contains reserved characters from gen-delims: " + delims);
        }
        if (!subDelims.matcher(path).matches()) {
            String delims = "\"!\" / \"$\" / \"&\" / \"'\" / \"(\" / \")\" / \"*\" / \"+\" / \",\" / \";\" / \"=\" \"%\"";
            throw new MCRException("Path name " + path + " contains reserved characters from sub-delims: " + delims);
        }
        if (path.contains("../") || path.contains("..\\")) {
            throw new MCRException("Path name " + path + " may not contain \"../\".");
        }
        String fileName = getFileName(path);
        if (fileName != fileName.trim()) {
            throw new MCRException("File name '" + fileName + "' may not start or end with whitespace character.");
        }
    }

    protected static String getFileName(String path) {
        int pos = Math.max(path.lastIndexOf('\\'), path.lastIndexOf("/"));
        return path.substring(pos + 1);
    }

    protected void sendException(HttpServletResponse res, Exception ex) throws Exception {
        HashMap<String, String> response = new HashMap<>();
        response.put("clname", ex.getClass().getName());
        response.put("strace", MCRException.getStackTraceAsString(ex));

        if (ex.getLocalizedMessage() != null) {
            response.put("message", ex.getLocalizedMessage());
        }

        sendResponse(res, "upload/exception", response);
    }

    protected void sendResponse(HttpServletResponse res, String value) throws Exception {
        HashMap<String, String> parameters = new HashMap<>();

        if (value != null) {
            parameters.put("return", value);
        }

        sendResponse(res, "upload/response", parameters);
    }

    protected void sendResponse(HttpServletResponse res, String mime, Map<String, String> parameters) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        DataOutputStream dos = new DataOutputStream(baos);
        Set<Entry<String, String>> entries = parameters.entrySet();

        dos.writeUTF(mime);

        for (Entry<String, String> entry : entries) {

            // write out key
            dos.writeUTF(entry.getKey().toString());

            // write out class
            dos.writeUTF(entry.getValue().getClass().getName());
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

    protected Transaction startTransaction() {
        LOGGER.debug("Starting transaction");
        return MCRHIBConnection.instance().getSession().beginTransaction();
    }

    protected void commitTransaction(Transaction tx) {
        LOGGER.debug("Committing transaction");
        if (tx != null) {
            tx.commit();
            tx = null;
        } else {
            LOGGER.error("Cannot commit transaction. Transaction is null.");
        }
    }

    protected void rollbackAnRethrow(Transaction tx, Exception e) throws Exception {
        LOGGER.debug("Rolling back transaction");
        if (tx != null) {
            tx.rollback();
            tx = null;
        } else {
            LOGGER.error("Error while rolling back transaction. Transaction is null.");
        }
        throw e;
    }
}
