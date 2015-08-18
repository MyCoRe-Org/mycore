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
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.purl.sword.atom.Summary;
import org.purl.sword.atom.Title;
import org.purl.sword.base.HttpHeaders;
import org.purl.sword.base.SWORDErrorDocument;
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
    public static SWORDServer createServer() {
        // Instantiate the correct SWORD Server class
        SWORDServer myRepository = null;
        try {

            myRepository = MCRConfiguration.instance().getInstanceOf("MCR.SWORD.server.class", MCRSWORDServer.class.getName());
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
        authenticator = MCRConfiguration.instance().getInstanceOf("MCR.SWORD.auth.class", MCRSWORDAuthenticator.class.getName());
        LOG.info("Using " + authenticator.getClass().getName() + " as the MCRSWORDAuthenticator");
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
    
    /**
     * Utility method to construct a SWORDErrorDocumentTest
     * 
     * @param errorURI
     *            The error URI to pass
     * @param status
     *            The HTTP status to return
     * @param summary
     *            The textual description to give the user
     * @param request
     *            The HttpServletRequest object
     * @param response
     *            The HttpServletResponse to send the error document to
     */
    public static void makeErrorDocument(String errorURI, int status, String summary, HttpServletRequest request, HttpServletResponse response) throws IOException {
        SWORDErrorDocument sed = new SWORDErrorDocument(errorURI);
        Title title = new Title();
        title.setContent("ERROR");
        sed.setTitle(title);
        Calendar calendar = Calendar.getInstance();
        String utcformat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        SimpleDateFormat zulu = new SimpleDateFormat(utcformat);
        String serializeddate = zulu.format(calendar.getTime());
        sed.setUpdated(serializeddate);
        Summary sum = new Summary();
        sum.setContent(summary);
        sed.setSummary(sum);
        if (request.getHeader(HttpHeaders.USER_AGENT) != null) {
            sed.setUserAgent(request.getHeader(HttpHeaders.USER_AGENT));
        }
        response.setStatus(status);
        response.setContentType("application/atom+xml; charset=UTF-8");
        String errorDocText = sed.marshall().toXML();
        PrintWriter out = response.getWriter();
        out.write(errorDocText);
        out.flush();

        LOG.debug(errorDocText);
    }
}
