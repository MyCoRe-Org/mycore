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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.purl.sword.base.ChecksumUtils;

/**
 * This class can be used for instantiating a singleton for authentification of
 * SWORD users. An instance should be used for authentification of users who
 * want to use the SWORD interface of this MyCoRes system.
 * 
 * @author Nils Verheyen
 * 
 */
public class MCRSWORDAuthenticator {

    private static final Logger LOG = Logger.getLogger(MCRSWORDAuthenticator.class);

    /** The type of authentification which should be used. */
    private String authN;

    /** Contains all auth data for basic authentification. */
    private Properties basicAuthData;

    /**
     * contains the file with username password combination for basic auth.
     * passwords must be given in md5 hash.
     */
    private File swordUsersFile;

    /** Last read time of basic auth data file */
    private long lastSwordBasicFileReadTime;

    /** Private Constructor for use as a singleton. */
    public MCRSWORDAuthenticator() {
        init();
    }
    
    private void init() {
        
        LOG.info("initializing authenticator");
        basicAuthData = new Properties();
        try {
            String authFilename = MCRConfiguration.instance().getString("MCR.SWORD.auth.file");
            if (authFilename != null) {
                swordUsersFile = new File(authFilename);
                loadBasicAuthData();
            }
        } catch (IOException e) {
            LOG.error("couldn't load sword auth data: " + e.getMessage(), e);
        }
        
        authN = MCRConfiguration.instance().getString("MCR.SWORD.auth.method", "None");
        if (authN != null) {
            LOG.info("setting auth method to: " + authN);
        } else {
            LOG.error("no auth method given");
        }
    }

    /**
     * Tries to authenticate target user with unencrypted password.
     * 
     * @param username
     *            username for authentification
     * @param password
     *            unencrypted, unhashed password for auth
     * @return <code>true</code> if the user could be authenticated,
     *         <code>false</code> otherwise
     */
    public boolean authenticate(String username, String password) throws IOException {

        if ("None".equalsIgnoreCase(authN)) {
            return true;
        } else if ("Basic".equalsIgnoreCase(authN)) {
            return handleBasicAuth(username, password);
        }
        return false;
    }

    /**
     * Handles the authentification with given username and password. A md5 sum
     * is generated for given password. Passwords inside used file
     * (sword_users.properties) must be saved as md5.
     * 
     * @param username
     *            the username for authentification
     * @param password
     *            unencrypted password for authentification
     * @return <code>true</code> if authentification was successful,
     *         <code>false</code> otherwise.
     * @throws IOException
     */
    private boolean handleBasicAuth(String username, String password) throws IOException {

        if (username == null)
            return false;
        
        loadBasicAuthData();
        try {

            String savedPassword = (String) basicAuthData.get(username);
            if (savedPassword != null) {

                String md5Password = ChecksumUtils.generateMD5(password.getBytes("UTF-8"));
                return savedPassword.equals(md5Password);
            }
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            LOG.error(e.getMessage());
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        return false;
    }

    /**
     * Reads auth data from swords users file, if needed. Data is only read if
     * the last modified date of the file is newer than last read time.
     * 
     * @throws IOException
     */
    private void loadBasicAuthData() throws IOException {

        if (lastSwordBasicFileReadTime < swordUsersFile.lastModified()) {

            Reader input = new InputStreamReader(new FileInputStream(swordUsersFile), "ISO-8859-1");
            basicAuthData.load(input);
            input.close();
            lastSwordBasicFileReadTime = System.currentTimeMillis();
        }
    }
}
