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

package org.mycore.backend.remote;

import java.util.*;
import java.io.*;
import java.net.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.MCRException;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.xml.MCRXMLContainer;

/**
 * This class implements the interface for communication between the
 * local MCRClient and a remote MCRServer via HTTP/HTTPS.
 *
 * @author Mathias Zarick
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRServletCommunication implements MCRRemoteAccessInterface
{

// logger
static Logger logger=Logger.getLogger(MCRServletCommunication.class.getName());

// internal data
private String headercontext;
private boolean hasifsdata;
private MCRConfiguration config = MCRConfiguration.instance();

/**
 * This is the constructor for the MCRSocketCommunication.
 **/
public MCRServletCommunication()
  { 
  headercontext = ""; hasifsdata = false; 
  config = MCRConfiguration.instance();
  }

/**
 * This methode represide the query request methode for the communication.
 * For the connection parameter would the MCRConfiguration used.
 *
 * @param hostAlias the list of hostnames as string they should requested.
 * @param reqtype   the type value of the MCRObjectId
 * @param query     the query as a stream
 * @exception MCRException general Exception of MyCoRe
 * @return the result of the query as MCRXMLContainer
 **/
public MCRXMLContainer requestQuery(String hostAlias, String reqtype, 
  String query) throws MCRException
  {
  MCRXMLContainer result = new MCRXMLContainer();
  PropertyConfigurator.configure(config.getLoggingProperties());

  logger.debug("HostAlias        = "+hostAlias);
  logger.debug("MCRObjectID Type = "+reqtype);
  logger.debug("Query            = "+query);

  String host = config.getString("MCR.remoteaccess_"+hostAlias+"_host","");
  if (host.length()==0) {
    System.out.println("Connection data for host "+hostAlias+" not found.");
    return result;
    }
  String protocol = config.getString("MCR.remoteaccess_"+hostAlias
    +"_protocol","");
  if (protocol.length()==0) {
    System.out.println("Connection data for host "+hostAlias+" not found.");
    return result;
    }
  protocol = protocol.toLowerCase();
  if ((!protocol.equals("http"))&&(!protocol.equals("https"))) {
    System.out.println("Connection protocol for host "+hostAlias+" is not"+
      " http or https.");
    return result;
    }
  int port = config.getInt("MCR.remoteaccess_"+hostAlias+"_port",0);
  if (port==0) {
    System.out.println("Connection port for host "+hostAlias+" is false.");
    return result;
    }
  String location = config.getString("MCR.remoteaccess_"+hostAlias
    +"_query_servlet");
  if (location.length()==0) {
    System.out.println("Connection location for host "+hostAlias+" not found.");
    return result;
    }
  System.out.println("Connecting to "+protocol+"://"+host+":"+port+
    location);

  URL currentURL;
  try {
    currentURL = new URL(protocol,host,port,location);
    HttpURLConnection urlCon = (HttpURLConnection) currentURL.openConnection();
    urlCon.setDoOutput(true);
    urlCon.setRequestMethod("POST");
    PrintWriter out = new PrintWriter(urlCon.getOutputStream());
    out.print("type=" + URLEncoder.encode(reqtype)
              + "&hosts=local"
              + "&query=" + URLEncoder.encode(query));
    out.close();
    BufferedInputStream in = new BufferedInputStream(urlCon.getInputStream());
    String fromServer="";
    int inread;
    while ((inread = in.read()) != -1) fromServer = fromServer + (char) inread;
    result.importElements(fromServer.getBytes());
    for (int i=0;i<result.size();i++) { result.setHost(i,hostAlias); }
    }
  catch(MCRException mcre) {
    System.err.println("Can't use the response from host:"+host+"."); }
  catch(UnknownHostException uhe) {
    System.err.println("Don't know about host: "+host+"."); }
  catch(IOException ioe) {
    System.err.println("Couldn't get I/O for the connection to: "+host+"."); }
  catch(Exception e) {
    e.printStackTrace(System.err); }
  return result;
  }

/**
 * This methode represide the IFS request methode for the communication.
 * For the connection parameter would the MCRConfiguration used.
 *
 * @param hostAlias the list of hostnames as string they should requested.
 * @param path      the path to the IFS data
 * @exception MCRException general Exception of MyCoRe
 * @return the result of the query as MCRXMLContainer
 **/
public BufferedInputStream requestIFS(String hostAlias, String path) 
  throws MCRException
  {
  BufferedInputStream in = null;
  hasifsdata = false;
  System.out.println("HostAlias = "+hostAlias);
  System.out.println("Path      = "+path);

  return in;
  }

}

