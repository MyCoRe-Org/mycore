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
 * This methode represide the request methode for the communication.
 * For the connection parameter would the MCRConfiguration used.
 *
 * @param hostlist the list of hostnames as string they should requested.
 * @param reqstream the stream of the request. The syntax of this XML
 *                  stream is extenal described.
 * @exception MCRException general Exception of MyCoRe
 **/
public void request(Vector hostlist, String reqstream)
  throws MCRException;
 
/**
 * This methode represide the response methode for the communication.
 * For the connection parameter would the MCRConfiguration used.
 *
 * @return resstream the stream of the response. The syntax of this XML
 *                  stream is extenal described.
 * @exception MCRException general Exception of MyCoRe
 **/
public String response() throws MCRException;
 
}
