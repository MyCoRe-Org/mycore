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
import java.io.*;
import java.net.*;
import mycore.common.MCRException;
import mycore.common.MCRConfiguration;
import mycore.datamodel.MCRQueryResultArray;
import mycore.datamodel.MCRObject;
import mycore.datamodel.MCRCommunicationInterface;

/**
 * This class implements the interface to choose the communication methodes
 * for the connection between MCRClient and MCRServer.
 *
 * @author Jens Kupferschmidt
 * @author Mathias Zarick
 * @version $Revision$ $Date$
 **/

public class MCRSocketCommunication implements MCRCommunicationInterface
{

private String reqtype;
private Vector hostlist;
private String mcrtype;
private String query;
private String reqstream = "";

/**
 * This is the constructor for the MCRSocketCommunication.
 **/
public MCRSocketCommunication()
  { reqstream = ""; }

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
  throws MCRException
  {
  for (int i=0;i<hostlist.size();i++) {
    System.out.println("Hostname : "+hostlist.elementAt(i));
    }
  System.out.println("MCR type : "+mcrtype);
  System.out.println("Query    : "+query);
  System.out.println();

  this.hostlist = hostlist;
  this.mcrtype = mcrtype;
  this.query = query;
  reqstream = mcrtype+"***"+query;
  }

/**
 * This methode represide the response methode for the communication.
 * For the connection parameter would the MCRConfiguration used.
 *
 * @return an empty MCRQueryResultArray as the response.
 * @exception MCRException general Exception of MyCoRe
 **/
public MCRQueryResultArray responseQuery() throws MCRException
  {
  String NL = System.getProperty("line.separator");
  String currentHost;     // in this format: server.domain.de:12345
  String host;            // in this format: server.domain.de
  int port;               // the port: 12345
  MCRQueryResultArray result = new MCRQueryResultArray();
  for (int i=0;i<hostlist.size();i++) {
    currentHost = (String)hostlist.elementAt(i);
    if ( currentHost.indexOf(':') > -1 ) {
      host = currentHost.substring(0,currentHost.indexOf(':'));
      port = Integer.parseInt(currentHost
        .substring(currentHost.indexOf(':')+1));
      }
    else {
      MCRConfiguration config = MCRConfiguration.instance();
      host = currentHost;
      port = config.getInt("MCR.communication_default_port");
      }
    MCRSocketCommunicator sc = null;
    try {
      sc = new MCRSocketCommunicator(new Socket(host, port)); }
    catch (UnknownHostException e) {
      throw new MCRException("Don't know about host: " + host +"."); }
    catch (IOException e) {
      throw new MCRException("Couldn't get I/O for the connection to: "
        + host + "."); }
    if (sc != null)
    try {
      int state = 0;
      int resultSize = 0;
      String toServer ="";
      String fromServer = sc.read();
      while ( ! fromServer.equals("bye") ) {
        if (state == 0) {
          if (fromServer.equals("connected")) {
            toServer = "query in bytes:" + reqstream.length(); state = 1; }
          }
        else if (state == 1) {
          if (fromServer.equals("ok expecting host")) {
            toServer = host; state = 2; }
          }
        else if (state == 2) {
          if (fromServer.equals("ok expecting query")) {
            toServer = reqstream; state = 3; }
          }
        else if (state == 3) {
          try {
            if ( (fromServer.substring(0,15).equals("received bytes:")) &&
                 (reqstream.length() ==
                    Integer.parseInt(fromServer.substring(15))) ) {
              toServer = "ok expecting result size";
              state = 4;
              }
            }
          catch (NumberFormatException nfe) { }
          catch (IndexOutOfBoundsException ioobe) { }
          }
        else if (state == 4) {
          try {
            if ((fromServer.substring(0,16).equals("result in bytes:"))) {
              resultSize = Integer.parseInt(fromServer.substring(16));
              toServer = "ok expecting result";
              state = 5;
              }
            }
          catch (NumberFormatException nfe) { }
          catch (IndexOutOfBoundsException ioobe) { }
          }
        else if (state == 5) {
          if (fromServer.length() == resultSize) {
            if (fromServer.startsWith(MCRObject.XML_HEADER))
              fromServer = fromServer.substring(MCRObject.XML_HEADER.length());
            result.importElements(fromServer);
            toServer = "received bytes:"+resultSize;
            state = 6;
          }
        }
        sc.write(toServer);
        if (state == 5) fromServer = sc.read(resultSize);
        else fromServer = sc.read();
      }
      sc.close();
      }
    catch (Exception e) { e.printStackTrace(System.err);}
    }
  return result;
  }

}

