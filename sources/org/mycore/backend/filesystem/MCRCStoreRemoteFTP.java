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

package org.mycore.backend.filesystem;

import java.io.OutputStream;
import java.util.StringTokenizer;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPTransferType;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRFileReader;

/**
 * This class implements the MCRContentStore interface to store the content of
 * MCRFile objects on an FTP Server. The FTP connection parameters are
 * configured in mycore.properties:
 * 
 * <code>
 *   MCR.IFS.ContentStore.<StoreID>.Hostname       Hostname of remote server
 *   MCR.IFS.ContentStore.<StoreID>.FTPPort        FTP port of remote server, default is 21
 *   MCR.IFS.ContentStore.<StoreID>.UserID         User ID for FTP connections
 *   MCR.IFS.ContentStore.<StoreID>.Password       Password for this user
 *   MCR.IFS.ContentStore.<StoreID>.BaseDirectory  Directory on server where content will be stored
 *   MCR.IFS.ContentStore.<StoreID>.DebugFTP       If true, FTP debug messages are written to stdout, default is false
 * </code>
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRCStoreRemoteFTP extends MCRContentStore {
	/** Hostname of FTP server */
	protected String host;

	/** FTP Port of remote host */
	protected int port;

	/** User ID for FTP login */
	protected String user;

	/** Password for FTP login */
	protected String password;

	/** Base directory on FTP server where content is stored */
	protected String baseDir;

	/** If true, FTP debug messages are written to stdout */
	protected boolean debugFTP;

	/** FTP Return codes if mkdir is successful in our interpretation */
	protected final static String[] mkdirOK = { "257", "521", "550" };

	/** FTP Return codes if rmdir is successful in our interpretation */
	protected final static String[] rmdirOK = { "250", "550" };

	public void init(String storeID) {
		super.init(storeID);

		MCRConfiguration config = MCRConfiguration.instance();

		host = config.getString(prefix + "Hostname");
		port = config.getInt(prefix + "FTPPort", 21);
		user = config.getString(prefix + "UserID");
		password = config.getString(prefix + "Password");
		baseDir = config.getString(prefix + "BaseDirectory");
		debugFTP = config.getBoolean(prefix + "DebugFTP", false);
	}

	protected String doStoreContent(MCRFileReader file,
			MCRContentInputStream source) throws Exception {
		FTPClient connection = connect();
		try {
			StringBuffer storageID = new StringBuffer();
			String[] slots = buildSlotPath();

			// Recursively create slot directories
			for (int i = 0; i < slots.length; i++) {
				connection.quote("MKD " + slots[i], mkdirOK);
				connection.chdir(slots[i]);
				storageID.append(slots[i]).append("/");
			}

			String fileID = buildNextID(file);
			connection.put(source, fileID);
			storageID.append(fileID);

			return storageID.toString();
		} finally {
			disconnect(connection);
		}
	}

	protected void doDeleteContent(String storageID) throws Exception {
		FTPClient connection = connect();
		try {
			connection.delete(storageID);

			// Recursively remove all directories that have been created, if
			// empty:
			StringTokenizer st = new StringTokenizer(storageID, "/");
			int numDirs = st.countTokens() - 1;
			String[] dirs = new String[numDirs];

			for (int i = 0; i < numDirs; i++) {
				dirs[i] = st.nextToken();
				if (i > 0)
					dirs[i] = dirs[i - 1] + "/" + dirs[i];
			}

			for (int i = numDirs; i > 0; i--) {
				connection.quote("RMD " + dirs[i - 1], rmdirOK);
			}
		} finally {
			disconnect(connection);
		}
	}

	protected void doRetrieveContent(MCRFileReader file, OutputStream target)
			throws Exception {
		FTPClient connection = connect();
		try {
			connection.get(target, file.getStorageID());
		} finally {
			disconnect(connection);
		}
	}

	/**
	 * Connects to remote host via FTP
	 */
	protected FTPClient connect() throws MCRPersistenceException {
		FTPClient connection = null;

		try {
			connection = new FTPClient(host, port);
			connection.debugResponses(debugFTP);
			connection.login(user, password);
			connection.setType(FTPTransferType.BINARY);
		} catch (Exception exc) {
			String msg = "Could not connect to " + host + ":" + port
					+ " via FTP";
			throw new MCRPersistenceException(msg, exc);
		}

		try {
			connection.chdir(baseDir);
		} catch (Exception exc) {
			String msg = "Could not chdir to " + baseDir + " on FTP host "
					+ host;
			throw new MCRPersistenceException(msg, exc);
		}

		return connection;
	}

	/**
	 * Closes the FTP connection to remote host
	 * 
	 * @param connection
	 *            the FTP connection to close
	 */
	protected void disconnect(FTPClient connection) {
		try {
			connection.quit();
		} catch (Exception ignored) {
		}
	}
}
