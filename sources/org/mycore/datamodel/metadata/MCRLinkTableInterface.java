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

package org.mycore.datamodel.metadata;

import java.util.*;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRPersistenceException;

/**
 * This interface is designed to choose the Persistence for the link tables.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public interface MCRLinkTableInterface {

    /**
     * The initializer for the class MCRSQLLinkTableStore. It reads the
     * classification configuration and checks the table names.
     *
     * @exception throws
     *                if the type is not correct
     */
    public void init(String type);

    /**
     * The method create a new item in the datastore.
     *
     * @param from
     *            a string with the link ID MCRFROM
     * @param to
     *            a string with the link ID MCRTO
     */
    public void create(String from, String to);

    /**
     * The method remove a item for the from ID from the datastore.
     *
     * @param from
     *            a string with the link ID MCRFROM
     */
    public void delete(String from);

    /**
     * The method count the number of references to the 'to' value of the table.
     *
     * @param to
     *            the object ID as String, they was referenced
     * @return the number of references
     */
    public int countTo(String to);

    /**
	 * The method count the number of references to the 'to' value of the table with condition of a spezial doctype.
	 *
	 * @param to the object ID as String, they was referenced
	 * @param doctype the type of the from document f.e. document, disshab, ...
	 * @return the number of references
	 **/
	public int countTo( String to, String doctype, String to2 ) ;



}

