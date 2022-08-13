/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.pi.purl;

import org.w3c.dom.Document;

/**
 * Interface for a PURL Manager to register Persistent URLs on a PURL server
 * 
 *  Hint:
 * -----
 * Please check in your code that you do not register / override regular PURLs in test / development
 * by checking:
 * if (resolvingURL.contains("localhost")) {
 * purl = "/test" + purl;
 * }
 *
 * @author Robert Stephan
 */
public interface MCRPURLManager {

    /**
     * Login into the server
     *
     * @param purlServerURL - the base URL of the PURL server
     * @param user          - the PURL server user
     * @param password      - the user's password
     */
    public void login(String purlServerURL, String user, String password);

    /**
     * logout from PURL server
     */
    public void logout();

    /**
     * register a new PURL
     *
     * @param purl        - the PURL
     * @param target      the target URL
     * @param type        - the PURL type
     * @param maintainers - the maintainers
     * @return the HTTP Status Code of the request
     */
    public int registerNewPURL(String purl, String target, String type, String maintainers);

    /**
     * updates an existing PURL
     *
     * @param purl        - the PURL (relative URL)
     * @param target      - the target URL
     * @param type        - the PURL type
     * @param maintainers list of maintainers (PURL server users or groups)
     * @return the HTTP Status Code of the request
     */
    public int updateExistingPURL(String purl, String target, String type, String maintainers);

    /**
     * deletes an existing PURL
     *
     * @param purl
     * @return the HTTP Status Code of the request
     */
    public int deletePURL(String purl);

    /**
     * check if a purl has the given target url
     *
     * @param purl      - the purl
     * @param targetURL - the target URL
     * @return true, if the target URL is registered at the given PURL
     */
    public boolean isPURLTargetURLUnchanged(String purl, String targetURL);

    /**
     * return the PURL metadata
     *
     * @param purl      - the purl
     * @return an XML document containing the metadata of the PURL
     *        or null if the PURL does not exist
     */
    public Document retrievePURLMetadata(String purl);

    /**
     * check if a PURL exists
     *
     * @param purl      - the purl
     * @return true, if the given PURL is known
     */
    public boolean existsPURL(String purl);
}
