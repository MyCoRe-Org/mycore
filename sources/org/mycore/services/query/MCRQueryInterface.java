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

import java.util.*;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLContainer;

/**
 * This interface is designed to choose the tranformer from XQuery to
 * the used query system of the persistence layer. 
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public interface MCRQueryInterface
{

public final static char COMMAND_OR='O';
public final static char COMMAND_AND='A';
public final static char COMMAND_XOR='X';

/**
 * This method parse the XQuery string and return the result as
 * MCRXMLContainer. If the type is null or empty or maxresults
 * is lower 1 an empty list was returned.
 *
 * @param query                 the XQuery string
 * @param maxresults            the maximum of results
 * @param type                  a list of the MCRObject types seperated by ,
 * @return                      a result list as MCRXMLContainer
 **/
public MCRXMLContainer getResultList(String query, String type,
  int maxresults);

/**
 * returns the ObjectID of the Object containing derivate with given ID
 * @param DerivateID ID of Derivate
 * @return ObjectID
 */
public String getObjectID(String DerivateID);

/**
 * returns XMLContainer containing mycoreobject related do DerivateID
 * @param DerivateID
 * @return
 */
public MCRXMLContainer getObjectForDerivate(String DerivateID);

/**
 * merges to XMLContainer after specific rules
 * @see #COMMAND_OR
 * @see #COMMAND_AND
 * @see #COMMAND_XOR
 * @param result1 1st MCRXMLContainer to be merged
 * @param result2 2nd MCRXMLContainer to be merged
 * @param operation available COMMAND_XYZ
 * @return merged ResultSet
 */
public MCRXMLContainer mergeResults(
	MCRXMLContainer result1,
	MCRXMLContainer result2,
	char operation);
}

