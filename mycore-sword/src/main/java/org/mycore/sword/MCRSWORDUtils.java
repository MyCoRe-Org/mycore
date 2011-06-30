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

package org.mycore.sword;

import java.io.File;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.purl.sword.server.SWORDServer;

/**
 * This class provides utility methods for mycores sword interface
 * 
 * @author Nils Verheyen
 * 
 */
public class MCRSWORDUtils {

    private static final Logger LOG = Logger.getLogger(MCRSWORDUtils.class);

    /**
     * Creates a new instance of {@link SWORDServer}.
     * 
     * @return a new instance of {@link SWORDServer} or <code>null</code> if it
     *         could not be instantiated.
     * @throws MCRConfigurationException
     *             thrown if no instance of {@link SWORDServer} could be
     *             instantiated
     */
    public static final SWORDServer createServer() {
        // Instantiate the correct SWORD Server class
        SWORDServer myRepository = null;
        try {

            myRepository = (SWORDServer) MCRConfiguration.instance().getInstanceOf("MCR.SWORD.server.class", MCRSWORDServer.class.getName());
            LOG.info("Using " + myRepository.getClass().getName() + " as the SWORDServer");
        } catch (MCRConfigurationException e) {
            String repositoryClassName = MCRConfiguration.instance().getString("MCR.SWORD.server.class");
            LOG.fatal("Unable to instantiate class from 'MCR.SWORD.server.class': " + repositoryClassName);
            throw new MCRConfigurationException("Unable to instantiate class from 'MCR.SWORD.server.class': " + repositoryClassName);
        }
        return myRepository;
    }

    /**
     * Creates a new instance of {@link MCRSWORDAuthenticator}.
     * 
     * @return a new instance of {@link MCRSWORDAuthenticator} or
     *         <code>null</code> if it could not be instantiated.
     * @throws MCRConfigurationException
     *             thrown if no instance of {@link MCRSWORDAuthenticator} could
     *             be instantiated
     */
    public static MCRSWORDAuthenticator createAuthenticator() {
        // Instantiate the correct SWORD Server class
        MCRSWORDAuthenticator authenticator = null;
        try {

            authenticator = (MCRSWORDAuthenticator) MCRConfiguration.instance().getInstanceOf("MCR.SWORD.auth.class", MCRSWORDAuthenticator.class.getName());
            LOG.info("Using " + authenticator.getClass().getName() + " as the MCRSWORDAuthenticator");
        } catch (MCRConfigurationException e) {
            String repositoryClassName = MCRConfiguration.instance().getString("MCR.SWORD.auth.class");
            LOG.fatal("Unable to instantiate class from 'MCR.SWORD.server.class': " + repositoryClassName);
            throw new MCRConfigurationException("Unable to instantiate class from 'MCR.SWORD.auth.class': " + repositoryClassName);
        }
        return authenticator;
    }

    /**
     * Reads auth method from configuration.
     * 
     * @return given auth method from configuration or <code>None</code> if it
     *         is not given
     */
    public static String getAuthN() {
        // Set the authentication method
        String authN = MCRConfiguration.instance().getString("MCR.SWORD.auth.method", "None");
        if ((authN == null) || ("".equals(authN))) {
            authN = "None";
        }
        LOG.info("Authentication type set to: " + authN);
        return authN;
    }

    /**
     * Reads max uploadable file size from configuration.
     * 
     * @return the maximum size in kBytes for uploading or <code>-1</code> for
     *         unlimited size.
     */
    public static int getMaxUploadSize() {

        int maxUploadSize = MCRConfiguration.instance().getInt("MCR.SWORD.max.uploaded.file.size", -1);
        if (maxUploadSize <= 0) {
            LOG.warn("No maxUploadSize set, therefor setting max file upload size to unlimited.");
        }
        return maxUploadSize;
    }

    /**
     * Reads temp dir from mycore configuration.
     * 
     * @return a directory where files can be written into.
     * @throws MCRConfigurationException
     *             thrown if no temporary directory could be read/created
     */
    public static File getTempUploadDir() {

        String tempDirectory = MCRConfiguration.instance().getString("MCR.SWORD.temp.upload.dir");
        if ((tempDirectory == null) || (tempDirectory.isEmpty())) {
            tempDirectory = System.getProperty("java.io.tmpdir");
        }
        File tempDir = new File(tempDirectory);
        LOG.info("Upload temporary directory set to: " + tempDir);
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                throw new MCRConfigurationException("Upload directory did not exist and I can't create it. " + tempDir);
            }
        }
        if (!tempDir.isDirectory()) {
            LOG.fatal("Upload temporary directory is not a directory: " + tempDir);
            throw new MCRConfigurationException("Upload temporary directory is not a directory: " + tempDir);
        }
        if (!tempDir.canWrite()) {
            LOG.fatal("Upload temporary directory cannot be written to: " + tempDir);
            throw new MCRConfigurationException("Upload temporary directory cannot be written to: " + tempDir);
        }
        return tempDir;
    }

    /**
     * Returns the auth realm specified in mycores configuration.
     * 
     * @return the specified auth realm or "MyCoRe SWORD" if it is not given.
     */
    public static String getAuthRealm() {
        return MCRConfiguration.instance().getString("MCR.SWORD.auth.realm", "MyCoRe SWORD");
    }

    /**
     * Reads authentication data for http basic auth from given request.
     * 
     * @param request
     *            contains the request to read basic auth data from
     * @return returns a new pair with the username as first object and password
     *         as second
     */
    public static Pair<String, String> readBasicAuthData(HttpServletRequest request) {

        // Get the Authorization header, if one was supplied
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null) {
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.hasMoreTokens()) {
                String basic = st.nextToken();

                // We only handle HTTP Basic authentication
                if (basic.equalsIgnoreCase("Basic")) {
                    String credentials = st.nextToken();

                    String userPass = new String(Base64Coder.decodeString(credentials));

                    // The decoded string is in the form
                    // "userID:password".

                    int p = userPass.indexOf(":");
                    if (p != -1) {
                        String userID = userPass.substring(0, p);
                        String password = userPass.substring(p + 1);

                        return Pair.of(userID, password);
                    }
                }
            }
        }
        return null;
    }
}
