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

package mycore.communication;

import java.util.*;
import mycore.common.MCRException;
import mycore.datamodel.MCRQueryResultArray;
import mycore.datamodel.MCRCommunicationInterface;

/**
 * This class implements the interface to choose the communication methodes
 * for the connection between MCRClient and MCRServer. This class is a
 * dummy implementation without functionallity and returns empty data.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/

public class MCRCommunicationDummy implements MCRCommunicationInterface
{

/**
 * This is the empty constructor for the MCRCommunicationDummy.
 **/
public MCRCommunicationDummy()
  {
  }

/**
 * This methode represide the query request methode for the communication.
 * For the connection parameter would the MCRConfiguration used.
 *
 * @param hostlist the list of hostnames as string they should requested.
 * @param mcrtype  the type value of the MCRObjectId
 * @param query    the query as a stream
 * @exception MCRException general Exception of MyCoRe
 **/
public final void requestQuery(Vector hostlist, String mcrtype, String query)
  throws MCRException
  {
  for (int i=0;i<hostlist.size();i++) {
    System.out.println("Hostname : "+hostlist.elementAt(i));
    }
  System.out.println("MCR type : "+mcrtype);
  System.out.println("Query    : "+query);
  System.out.println();
  }
 
/**
 * This methode represide the query response methode for the communication.
 * For the connection parameter would the MCRConfiguration used.
 *
 * @return an empty MCRQueryResultArray as the response. 
 * @exception MCRException general Exception of MyCoRe
 **/
public final MCRQueryResultArray responseQuery() throws MCRException
  {
  return new MCRQueryResultArray() ;
  }
 
}
