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

package org.mycore.services.webservices;

public interface MCRWS {

    /**
     * Retrieves MyCoRe object
     * 
     * @param ID
     *            The ID of the document to retrieve
     * 
     * @return data of mycore object
     * 
     */
    public abstract org.w3c.dom.Document MCRDoRetrieveObject(String ID) throws Exception;

    /**
     * Retrieves MyCoRe Classification
     * 
     * @param level
     *            number of levels to retrievwe
     * @param type
     *            parents|children
     * @param classID
     *            The ID of the classification
     * @param categID
     *            categroryID where retrieval with level starts
     * @param format
     *            of retrieved classification, valid values are:
     *            editor['['formatAlias']']|metadata
     * 
     * @return data of mycore classification
     * 
     */
    public abstract org.w3c.dom.Document MCRDoRetrieveClassification(String level, String type, String classID, String categID, String format) throws Exception;

    /**
     * Search for MyCoRe objects
     * 
     * @param query
     *            as mycore xml query
     * 
     * @return resultset of search
     * 
     */
    public abstract org.w3c.dom.Document MCRDoQuery(org.w3c.dom.Document query) throws Exception;

    /**
     * Retrieves MyCoRe Links
     * 
     * @param from
     *            the source ID of the link
     * @param to
     *            the target ID of the link
     * @param type
     *            the link type (if it is null, refernce is default)
     * 
     * @return mcr:result JDOM object
     * 
     */
    public abstract org.w3c.dom.Document MCRDoRetrieveLinks(String from, String to, String type) throws Exception;

}