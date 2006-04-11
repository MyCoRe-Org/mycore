/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.services.wsclient;

import java.util.Properties;
import java.rmi.Remote;

import org.mycore.common.xml.MCRURIResolver;

import org.apache.log4j.Logger;

import org.apache.axis.AxisFault;
import org.apache.axis.client.Stub;

import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.message.token.UsernameToken;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.configuration.FileProvider;

/**
 * Test client for MyCoRe MCRWebService
 * 
 * @author Harald Richter
 * @version $Revision$ $Date$
 **/
public class MCRWebServiceClient
{
  static Logger logger = Logger.getLogger( MCRWebServiceClient.class );

  private static void usage()
  {
    System.out.println( "usage: parameter of MCRWebServicClient\n" );
    System.out.println( "-endpoint     url of webservice" );
    System.out.println( "-operation    valid opartions are:" );
    System.out.println( "                 retrieve (mycore object), parameter -mcrid required" );
    System.out.println( "                 retrievecl (mycore classification), parameters -classID, -level and -catID rquired" );
    System.out.println( "                 query, parameter -file required" );
    System.out.println( "-mcrid        id of MyCoRe-Object" );
    System.out.println( "-file         xml file with query" );
  }
  
  private static void handleParams(String args[], Properties params)
  {
    for (int i=0; i<args.length; i=i+2 )
    {
      String op    = args[i];
      String value = args[i+1];
      if ( "-endpoint".equals(op))
        params.setProperty("endpoint", value);
      else if ( "-operation".equals(op))
        params.setProperty("operation", value);
      else if ( "-mcrid".equals(op))
        params.setProperty("mcrid", value);
      else if ( "-file".equals(op))
        params.setProperty("file", value);
      else if ( "-classID".equals(op))
        params.setProperty("classID", value);
      else if ( "-level".equals(op))
        params.setProperty("level", value);
      else if ( "-catID".equals(op))
        params.setProperty("catID", value);
    }
  }
  
  public static void main(String args[])
  {
    Properties params = new Properties();
    params.setProperty("endpoint", "http://localhost:8080/docportal/services/MCRWebService");
/*    
    params.setProperty("mcrid", "DocPortal_document_00410901");
    params.setProperty("operation", "retrieve");
*/    
    handleParams(args, params);
    
    String endpoint = params.getProperty("endpoint");
    System.out.println("Endpoint: " + endpoint);
    String operation = params.getProperty("operation");
    System.out.println("Operation: " + operation);

    
    EngineConfiguration config = new FileProvider("client_deploy.wsdd");
    MCRWSServiceLocator l = new MCRWSServiceLocator(config);
    
    try 
    {
      l.setMCRWebServiceEndpointAddress(endpoint);
      
      Remote remote = l.getPort(MCRWS.class);
      Stub axisPort = (Stub)remote;
      axisPort._setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
      axisPort._setProperty(UsernameToken.PASSWORD_TYPE, WSConstants.PASSWORD_DIGEST);
      axisPort._setProperty(WSHandlerConstants.USER, "wss4j");
      axisPort._setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, "org.mycore.services.wsclient.MCRPWCallback");

      //Need to cast 
      MCRWS stub          = (MCRWebServiceSoapBindingStub)axisPort;
      
      if ("retrieve".equals(operation))
      {
        String mcrid = params.getProperty("mcrid");
        if ( null != mcrid )
        {
          org.w3c.dom.Document result = stub.MCRDoRetrieveObject( mcrid );

          org.jdom.input.DOMBuilder d = new org.jdom.input.DOMBuilder();
          org.jdom.Document doc = d.build(result);
          org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
          logger.info( outputter.outputString(doc ) );
        }
        else
          System.out.println("parameter -mcrid missing");
      }
      else if ("query".equals(operation))
      {
        String file = params.getProperty("file");
        if ( null != file )
        {
          System.out.println("file://" + file );
          org.jdom.Element query = MCRURIResolver.instance().resolve( "file://" + file );
          org.jdom.Document root = new org.jdom.Document(query);
          org.jdom.output.DOMOutputter doo = new org.jdom.output.DOMOutputter();

          org.w3c.dom.Document result = stub.MCRDoQuery(doo.output(root));

          org.jdom.input.DOMBuilder d = new org.jdom.input.DOMBuilder();
          org.jdom.Document doc = d.build(result);
          org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
          logger.info( outputter.outputString(doc ) );
        } else
          System.out.println("xml file with query missing");
      }
      else if ("retrievecl".equals(operation))
      {
        String classID = params.getProperty("classID");
        String level   = params.getProperty("level");
        String catID   = params.getProperty("catID", "");
        if ( null != classID && null != level )
        {
          org.w3c.dom.Document result = stub.MCRDoRetrieveClassification(level, "children", classID,  catID);

          org.jdom.input.DOMBuilder d = new org.jdom.input.DOMBuilder();
          org.jdom.Document doc = d.build(result);
          org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
          logger.info( outputter.outputString(doc ) );
        } else
          System.out.println("parameter(s) for retrieve classification missing");
      }
      else
      {
        usage();
      }
    }
    catch (AxisFault ax)
    {
      System.out.println(ax.toString());
      ax.dump();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
}