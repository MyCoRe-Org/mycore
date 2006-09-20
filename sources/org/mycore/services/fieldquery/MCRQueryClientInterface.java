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

/*
 * The interface for the remote access.
 * 
 * @author Jens Kupferschmidt @author Frank Lützenkirchen
 */

public interface MCRQueryClientInterface {

    /*
     * Initalize the class for using.
     */
    abstract void init(org.jdom.Element xmlhost);
    
    /*
     * The method return the alias of the host definition. @return the host
     * alias as String
     */
    abstract String getAlias();

    /**
     * Executes a query on a single remote host using the defined service.
     * 
     * @param inDoc
     *            the query as W3C DOM document
     * @param results
     *            the result list to add the hits to
     */
    abstract void search(org.w3c.dom.Document inDoc, MCRResults results);

    /**
     * Retrieves an Object from remote host using the defined service.
     * 
     * @param hostAlias
     *            the alias of the remote host as defined in hosts.xml
     * @param ID
     *            the ID of the Object to retrieve
     * @return the object document
     */
    abstract org.w3c.dom.Document doRetrieveObject(String ID);
    
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
     * @param format
     *             of retrieved classification, valid values are: editor['['formatAlias']']|metadata
@return the classification document
     */
    abstract org.w3c.dom.Document doRetrieveClassification(String level, String type, String classID, String categID, String format);
}
