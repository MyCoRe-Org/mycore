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

package org.mycore.services.fieldquery;

import org.apache.log4j.Logger;

/*
 * This class is a base for all remote access classes. If it is instanced it set
 * all data for one host defined in the file hosts.xml. Default values are the
 * WebService data.
 * 
 * @author Jens Kupferschmidt @author Frank Lützenkirchen
 */

public class MCRQueryClientBase implements MCRQueryClientInterface {

    /** The logger */
    protected final static Logger LOGGER = Logger.getLogger(MCRQueryClientBase.class);

    /** The host alias */
    protected String alias = "";

    /** The base URL for the remote host */
    protected String url = "";

    /** The access mode for the remote host */
    protected String access = "";

    /** The URL path for the remote host service */
    protected String servicepath = "";

    /* The constructor. */
    public MCRQueryClientBase() {
    }

    /*
     * The initialization.
     * 
     * @param xmlhost an entry of a remote host from hosts.xml
     */
    public void init(org.jdom.Element xmlhost) {
        alias = xmlhost.getAttributeValue("alias");
        url = xmlhost.getAttributeValue("url");
        access = xmlhost.getAttributeValue("access");
        StringBuffer sb = new StringBuffer(256);
        sb.append("Host ").append(alias).append(" with access mode ").append(access).append(" uses host url ").append(url);
        LOGGER.debug(sb.toString());
    }

    /*
     * The method return the alias of the host definition. @return the host
     * alias as String
     */
    public final String getAlias() {
        return alias;
    }

    /**
     * Executes a query on a single remote host using the defined service.
     * 
     * @param inDoc
     *            the query as W3C DOM document
     * @param results
     *            the result list to add the hits to
     */
    public void search(org.w3c.dom.Document inDoc, MCRResults results) {
    }

    /**
     * Retrieves an Object from remote host using the defined service.
     * 
     * @param hostAlias
     *            the alias of the remote host as defined in hosts.xml
     * @param ID
     *            the ID of the Object to retrieve
     * @return the object document
     */
    public org.w3c.dom.Document doRetrieveObject(String ID) {
        return null;
    }

    /**
     * Retrieves an classification part from remote host using the WebService.
     * 
     * @param level
     *            the level of the classification to retrieve
     * @param type
     *            the type of the classification to retrieve
     * @param classID
     *            the class ID of the classification to retrieve
     * @param categID
     *            the category ID of the classification to retrieve
     * @return the classification document
     */
    public org.w3c.dom.Document doRetrieveClassification(String level, String type, String classID, String categID) {
        return null;
    }
}
