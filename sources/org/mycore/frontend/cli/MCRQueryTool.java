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

package mycore.commandline;

import java.io.*;
import java.util.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import mycore.common.*;
import mycore.datamodel.MCRQueryResult;
import mycore.communication.MCRCommunicationInterface;
import mycore.communication.MCRSocketCommunication;

/**
 * This class implements a main program, which start a query to a local
 * Library Server or to any remote Library Server in a configuration
 * list or to a dedicated named remote Library Server.
 * The result was presided as a text output.
 * <p>
 * The follwing parameter must you give
 * <ul>
 * <li> [local|remort|<em>hostname</em>] - for the Library Server location
 * <li> <em>type</em> - for the MCRObjectID - type
 * <li> <em>query</em> - the query for the search
 * </ul>
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
final public class MCRQueryTool
{

/**
 * The query commandline tool.
 *
 * @param args   the argument string from the command line.
 **/
public static void main(String[] args)
  {
  System.out.println();
  System.out.println("* * * M y C o R e * * *");
  System.out.println();
  System.out.println("Version 1.0");
  System.out.println();

  if (args.length != 3) { usage(); System.exit(1); }
  String host = args[0].toLowerCase();
  String type = args[1].toLowerCase();
  String query = args[2].toLowerCase();
  System.out.println("Host  : "+host);
  System.out.println("Type  : "+type);
  System.out.println("Query : "+query);
  System.out.println();

  MCRConfiguration config = MCRConfiguration.instance();

  boolean todo = false;

  MCRQueryResult metadata = new MCRQueryResult();
  if(host.equals("local")) {
    metadata.setFromQuery(type,query);
    todo = true; }

  if(host.equals("remote")) {
    String hosts = config.getString("MCR.communication_hosts");
    int veclen = config.getInt("MCR.communication_max_hosts",3);
    Vector hostlist = new Vector(veclen);
    int i = 0;
    int j = hosts.length();
    int k = 0;
    while (k!=-1) {
      k = hosts.indexOf(",",i);
      if (k==-1) { 
        hostlist.addElement(hosts.substring(i,j)); }
      else {
        hostlist.addElement(hosts.substring(i,k)); i = k+1; }
      }
    MCRCommunicationInterface comm = null;
    comm = (MCRCommunicationInterface)config
      .getInstanceOf("MCR.communication_class");   
    // the request string must convert in a 'good' syntax
    comm.request(hostlist,type+"***"+query);
    metadata.setFromXMLResultStream(comm.response());
    todo = true; }

  if(!todo) {
    Vector hostlist = new Vector(1);
    hostlist.addElement(host);
    MCRCommunicationInterface comm = null;
    comm = (MCRCommunicationInterface)config
      .getInstanceOf("MCR.communication_class");   
    // the request string must convert in a 'good' syntax
    comm.request(hostlist,type+"***"+query);
    metadata.setFromXMLResultStream(comm.response());
    }

  printResult(metadata, config, type);

  System.out.println("Ready.");
  System.out.println();
  System.exit(0);
  }

/**
 * This methode print the usage of this tool.
 **/
private static final void usage()
  {
  System.out.println(); 
  System.out.println("Usage : java MCRQueryTool [local   ] type query");
  System.out.println("                          [remote  ]");
  System.out.println("                          [hostname]");
  System.out.println(); 
  }

/**
 * This methode transform the XML result collection to an ASCII output.
 *
 * @param results the XML result collection
 **/
private static final void printResult(MCRQueryResult results,
  MCRConfiguration config, String type)
  {
  String xsltoneresult = config.getString("MCR.xslt_oneresult_"+type);
  String outoneresult = config.getString("MCR.out_oneresult_"+type);
  TransformerFactory transfakt = TransformerFactory.newInstance();
  try {
    Transformer trans = 
      transfakt.newTransformer(new StreamSource(xsltoneresult));
    for (int l=0;l<results.getSize();l++) {
      String mcrid = results.getMCRObjectIdOfElement(l);
      String mcrxml = results.getXMLOfElement(l);
      System.out.println(mcrid); 
      System.out.println();
      StreamResult sr = null;
      if (outoneresult.equals("SYSOUT")) {
        sr = new StreamResult((OutputStream) System.out); }
      else {
        sr = new StreamResult(outoneresult); }
      trans.transform(new StreamSource((Reader)new StringReader(mcrxml)),sr);
      System.out.println();
      }
    }
  catch (Exception e) {
    System.out.println(e.getMessage()); }
  System.out.println();
  }

}

