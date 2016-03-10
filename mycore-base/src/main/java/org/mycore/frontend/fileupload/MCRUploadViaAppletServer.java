/*
 * 
 * $Revision: 34120 $ $Date: 2015-12-02 23:16:17 +0100 (Mi, 02 Dez 2015) $
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

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;

/**
 * Implements the server side of communication with the upload applet to receive file content.
 * 
 * @author Frank Lützenkirchen
 * @author Harald Richter
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision: 34120 $ $Date: 2015-12-02 23:16:17 +0100 (Mi, 02 Dez 2015) $
 * 
 * @see org.mycore.frontend.fileupload.MCRUploadHandler
 */
public final class MCRUploadViaAppletServer implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(MCRUploadViaAppletServer.class);

    private final static int bufferSize = 65536; // 64 KByte

    private String serverIP;

    private int serverPort;

    private ServerSocket server;

    private boolean doRun = true;

    MCRUploadViaAppletServer(String baseURL) throws ServletException {
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

    String getServerIP() {
        return serverIP;
    }

    int getServerPort() {
        return serverPort;
    }

    void close() {
        try {
            if (server != null) {
                server.close();
                doRun = false;
            }
        } catch (Exception ignored) {
            LOGGER.warn("Exception while destroy() in MCRUploadServlet !!!");
        }
    }

    private void handleUpload(Socket socket) {
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
            Transaction tx = MCRUploadHelper.startTransaction();
            try {
                long numBytesStored = uploadHandler.receiveFile(path, zis, length, md5);
                MCRUploadHelper.commitTransaction(tx);
                LOGGER.debug("Stored incoming file content with " + numBytesStored + " bytes");
                pwOut.println(numBytesStored);
                pwOut.flush();
                LOGGER.info("File transfer completed successfully.");
            } catch (Exception exc) {
                LOGGER.error("Error while uploading file: " + path, exc);
                MCRUploadHelper.rollbackAnRethrow(tx, exc);
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
}
