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

import java.io.*;
import java.net.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.MCRException;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.xml.MCRXMLContainer;

/**
 * This class implements the interface for communication between the
 * local MCRClient and a remote MCRServer via HTTP/HTTPS.<br>
 * This class use org.apache.log4j for logging.
 *
 * @author Mathias Zarick
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler
 * @version $Revision$ $Date$
 **/
public class MCRServletCommunication implements MCRRemoteAccessInterface
{

// logger
static Logger logger=Logger.getLogger(MCRServletCommunication.class.getName());

// internal data
private String realhost;
private String protocol;
private int port;
private String location;
private String headercontext;
private boolean hasifsdata;
private MCRConfiguration config = MCRConfiguration.instance();

/**
 * This is the constructor for the MCRSocketCommunication.
 **/
public MCRServletCommunication()
  { 
  // get the instance of MCRConfiguration
  config = MCRConfiguration.instance();
  // set the logger property
  PropertyConfigurator.configure(config.getLoggingProperties());
  // set the defaults
  headercontext = ""; hasifsdata = false; 
  }

/**
 * This method read the connection configuration from the property file.
 *
 * @param hostAlias the alias name of the called host
 * @return true if no error was occure, else return false
 **/
private final boolean readConnectionData(String hostAlias)
  {
  realhost = config.getString("MCR.remoteaccess_"+hostAlias+"_host","");
  if (realhost.length()==0) {
    logger.error("Connection data for host "+hostAlias+" not found.");
    return false;
    }
  protocol = config.getString("MCR.remoteaccess_"+hostAlias+"_protocol","");
  if (protocol.length()==0) {
    logger.error("Connection data for host "+hostAlias+" not found.");
    return false;
    }
  protocol = protocol.toLowerCase();
  if (!protocol.equals("http")) {
    logger.error("Connection protocol for host "+hostAlias+" is not HTTP.");
    return false;
    }
  port = config.getInt("MCR.remoteaccess_"+hostAlias+"_port",0);
  if (port==0) {
    logger.error("Connection port for host "+hostAlias+" is false.");
    return false;
    }
  return true;
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
  hasifsdata = false;

  logger.debug("HostAlias        = "+hostAlias);
  logger.debug("MCRObjectID Type = "+reqtype);
  logger.debug("Query            = "+query);

  if (!readConnectionData(hostAlias)) { return result; }
  location = config.getString("MCR.remoteaccess_"+hostAlias
    +"_query_servlet");
  if (location.length()==0) {
    logger.error("Connection location for host "+hostAlias+" not found.");
    return result;
    }
  StringBuffer sb = new StringBuffer(256);
  sb.append("Connecting to ").append(protocol).append("://")
    .append(realhost).append(':').append(port).append(location).append('?')
    .append("type=").append(URLEncoder.encode(reqtype))
    .append("&hosts=local").append("&XSL.Style=xml").append("&query=").append(URLEncoder.encode(query));
  logger.debug(sb.toString());

  URL currentURL;
  try {
    currentURL = new URL(protocol,realhost,port,location);
    HttpURLConnection urlCon = (HttpURLConnection) currentURL.openConnection();
    urlCon.setDoOutput(true);
    urlCon.setRequestMethod("POST");
    PrintWriter out = new PrintWriter(urlCon.getOutputStream());
    out.print("type=" + URLEncoder.encode(reqtype)
              + "&hosts=local"
              + "&XSL.Style=xml"
              + "&query=" + URLEncoder.encode(query));
    out.close();
    BufferedInputStream in = new BufferedInputStream(urlCon.getInputStream());
    result.importElements(in);
    in.close();
    urlCon.disconnect();
    currentURL=null;
    for (int i=0;i<result.size();i++) { result.setHost(i,hostAlias); }
    }
  catch(MCRException mcre) {
    logger.error("Can't use the response from host:"+realhost+"."); }
  catch(UnknownHostException uhe) {
    logger.error("Don't know about host: "+realhost+"."); }
  catch(IOException ioe) {
    logger.error("Couldn't get I/O for the connection to: "+realhost+".");
    }
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
public final BufferedInputStream requestIFS(String hostAlias, String path) 
  throws MCRException
  {
  BufferedInputStream in = null;
  hasifsdata = false;
  logger.debug("HostAlias        = "+hostAlias);
  logger.debug("Path             = "+path);

  if (!readConnectionData(hostAlias)) { return in; }
  location = config.getString("MCR.remoteaccess_"+hostAlias
    +"_ifs_servlet");
  if (location.length()==0) {
    logger.error("Connection location for host "+hostAlias+" not found.");
    return in;
    }
  if (path.length()==0) {
    logger.error("Connection path for host "+hostAlias+" is empty.");
    return in;
    }
  StringBuffer sb = new StringBuffer(256);
  sb.append("Connecting to ").append(protocol).append("://")
    .append(realhost).append(':').append(port).append(location).append(path)
    .append('?');
  if(path.endsWith("/")) { sb.append("XSL.Style=xml&"); }
  sb.append("hosts=local");
  logger.debug(sb.toString());

  URL currentURL;
  try {
    currentURL = new URL(protocol,realhost,port,location+path);
    HttpURLConnection urlCon = (HttpURLConnection) currentURL.openConnection();
    urlCon.setDoOutput(true);
    urlCon.setRequestMethod("POST");
    PrintWriter out = new PrintWriter(urlCon.getOutputStream());
    if(path.endsWith("/")) { out.print("XSL.Style=xml&"); }
    out.print("hosts=local");
    out.close();
    headercontext = urlCon.getContentType();
    hasifsdata = true;
    return new BufferedInputStream(urlCon.getInputStream());
    }
  catch(MCRException mcre) {
    logger.error("Can't use the response from host:"+realhost+"."); }
  catch(UnknownHostException uhe) {
    logger.error("Don't know about host: "+realhost+"."); }
  catch(IOException ioe) {
    logger.error("Couldn't get I/O for the connection to: "+realhost+".");
    }
  catch(Exception e) {
    logger.error(System.err); }
  return null;
  }

/**
 * This method returns the HPPT header content string, if a requestIFS was 
 * successful running.
 *
 * @return HPPT header content string
 **/
public final String getHeaderContent()
  { if (hasifsdata) { return headercontext; } else { return ""; } }

}

