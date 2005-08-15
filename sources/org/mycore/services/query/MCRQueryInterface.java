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

package org.mycore.services.query;

import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This interface is designed to choose the tranformer from XQuery to the used
 * query system of the persistence layer.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public interface MCRQueryInterface {

	/**
	 * This method parse the XQuery string and return the result as
	 * MCRXMLContainer. If the type is null or empty or maxresults is lower 1 an
	 * empty list was returned.
	 * 
	 * @param query
	 *            the XQuery string
	 * @param maxresults
	 *            the maximum of results
	 * @param type
	 *            a list of the MCRObject types seperated by ,
	 * @return a result list as MCRXMLContainer
	 */
	public MCRXMLContainer getResultList(String query, String type,
			int maxresults);

	/**
	 * returns the ObjectID of the Object containing derivate with given ID
	 * 
	 * @param DerivateID
	 *            ID of Derivate
	 * @return MCRObjectID of data objects
	 */
	public MCRObjectID getObjectID(String DerivateID);

}
