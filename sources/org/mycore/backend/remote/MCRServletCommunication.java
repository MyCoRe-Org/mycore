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
 * @author Mathias Zarick
 * @version $Revision$ $Date$
 **/

public class MCRServletCommunication implements MCRCommunicationInterface
{

private String reqtype;
private String hostAlias;
private String query;

/**
 * This is the constructor for the MCRSocketCommunication.
 **/
public MCRServletCommunication()
  { }

/**
 * This methode represide the query request methode for the communication.
 * For the connection parameter would the MCRConfiguration used.
 *
 * @param hostlist the list of hostnames as string they should requested.
 * @param reqtype  the type value of the MCRObjectId
 * @param query    the query as a stream
 * @exception MCRException general Exception of MyCoRe
 **/
public void requestQuery(String hostAlias, String reqtype, String query)
  throws MCRException
  {
  this.hostAlias = hostAlias;
  this.reqtype = reqtype;
  this.query = query;
  debug();
  }

/**
 * This method represides the response methode for the communication.
 * For connection parameters the MCRConfiguration is used.
 *
 * @return an empty MCRQueryResultArray as the response.
 * @exception MCRException general Exception of MyCoRe
 **/
public MCRQueryResultArray responseQuery() throws MCRException
{
  String NL = System.getProperty("line.separator");
  URL currentURL;
  MCRConfiguration config = MCRConfiguration.instance();
  String protocol = config.getString("MCR.communication_"+hostAlias
    +"_protocol");
  String host = config.getString("MCR.communication_"+hostAlias+"_host");
  int port = config.getInt("MCR.communication_"+hostAlias+"_port");
  String location = config.getString("MCR.communication_"+hostAlias
    +"_servlet_location");
  MCRQueryResultArray result = new MCRQueryResultArray();
  try {
    currentURL = new URL(protocol,host,port,location);
    HttpURLConnection urlCon = (HttpURLConnection) currentURL.openConnection();
    urlCon.setDoOutput(true);
    urlCon.setRequestMethod("POST");
    PrintWriter out = new PrintWriter(urlCon.getOutputStream());
    out.print("type=" + URLEncoder.encode(reqtype)
              + "&hosts=" + URLEncoder.encode(hostAlias)
              + "&query=" + URLEncoder.encode(query));
    out.close();
    BufferedInputStream in = new BufferedInputStream(urlCon.getInputStream());
    String fromServer="";
    int inread;
    while ( (inread = in.read()) != -1 ) fromServer = fromServer + (char) inread;
    result.importElements(fromServer);
  }
  catch(UnknownHostException uhe) {
    System.err.println("Don't know about host: " + host +". ");
  }
  catch(IOException ioe) {
    System.err.println("Couldn't get I/O for the connection to: " + host + ". ");
  }
  catch(Exception e) {
    e.printStackTrace(System.err);
  }
  return result;
}

/**
 * This methode debug this class.
 **/
public final void debug()
  {
  System.out.println("Hostname : "+hostAlias);
  System.out.println("MCR type : "+reqtype);
  System.out.println("Query    : "+query);
  System.out.println();
  }
}

