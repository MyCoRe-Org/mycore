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

package mycore.datamodel;

import java.util.*;
import mycore.common.MCRException;
import mycore.datamodel.MCRQueryResultArray;

/**
 * This interface is designed to choose the communication methodes
 * for the connection between MCRClient and MCRServer.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/

public interface MCRCommunicationInterface
{

/**
 * This methode represide the query request methode for the communication.
 * For the connection parameter would the MCRConfiguration used.
 *
 * @param hostlist the list of hostnames as string they should requested.
 * @param mcrtype  the type value of the MCRObjectId
 * @param query    the query as a stream
 * @exception MCRException general Exception of MyCoRe
 **/
public void requestQuery(Vector hostlist, String mcrtype, String query)
  throws MCRException;
 
/**
 * This methode represide the response methode for the communication.
 * For the connection parameter would the MCRConfiguration used.
 *
 * @return an empty MCRQueryResultArray as the response.
 * @exception MCRException general Exception of MyCoRe
 **/
public MCRQueryResultArray responseQuery() throws MCRException;
 
}
