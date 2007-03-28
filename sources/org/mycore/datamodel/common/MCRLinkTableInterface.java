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

package org.mycore.datamodel.common;

import java.util.List;
import java.util.Map;

/**
 * This interface is designed to choose the persistence for the link tables.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public interface MCRLinkTableInterface {
    
    /**
     * The method create a new item in the datastore.
     * 
     * @param from
     *            a string with the link ID MCRFROM
     * @param to
     *            a string with the link ID MCRTO
     * @param type
     *            a string with the link ID MCRTYPE
     * @param attr
     *            a string with the link ID MCRATTR
     */
    public void create(String from, String to, String type, String attr);

    /**
     * The method remove a item for the from ID from the datastore.
     * 
     * @param from
     *            a string with the link ID MCRFROM
     * @param to
     *            an array of strings with the link ID MCRTO
     * @param type
     *            an array of strings with the link ID MCRTYPE
     */
    public void delete(String from, String to, String type);

    /**
     * The method count the number of references with '%from%' and 'to' and
     * optional 'type' and optional 'restriction%' values of the table.
     * 
     * @param fromtype
     *            a substing in the from ID as String, it can be null
     * @param to
     *            the object ID as String, which is referenced
     * @param type
     *            the refernce type, it can be null
     * @param restriction
     *            a first part of the to ID as String, it can be null
     * @return the number of references
     */
    public int countTo(String fromtype, String to, String type, String restriction);

    /**
     * The method returns a Map of all counted distinct references
     * 
     * @param mcrtoPrefix
     * @return
     * 
     * the result-map of (key,value)-pairs can be visualized as <br />
     * select count(mcrfrom) as value, mcrto as key from
     * mcrlinkclass|mcrlinkhref where mcrto like mcrtoPrefix + '%' group by
     * mcrto;
     * 
     */
    public Map getCountedMapOfMCRTO(String mcrtoPrefix);

    /**
     * Returns a List of all link sources of <code>to</code> and a special
     * <code>type</code>
     * 
     * @param to
     *            Destination-ID
     * @param type
     *            Link reference type, this can be null. Current types are
     *            classid, child, parent, reference and derivate.
     * @return List of Strings (Source-IDs)
     */
    public List getSourcesOf(String to, String type);

    /**
     * Returns a List of all link destination of <code>from</code> and a
     * special <code>type</code>
     * 
     * @param from
     *            Source-ID
     * @param type
     *            Link reference type, this can be null. Current types are
     *            classid, child, parent, reference and derivate.
     * @return List of Strings (Destination-IDs)
     */
    public List getDestinationsOf(String from, String type);

}
