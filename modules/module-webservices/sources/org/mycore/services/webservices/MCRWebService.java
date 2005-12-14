/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * Copyright (C) 2005 University of Essen, Germany
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
 * along with this program, normally in the file documentation/license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.services.webservices;


import java.util.*;
import java.io.*;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.output.*;

import javax.xml.transform.Templates;
import javax.xml.transform.sax.TransformerHandler;

import org.mycore.backend.lucene.MCRLuceneSearcher;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.common.xml.MCRXSLTransformation;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRQueryParser;
import org.mycore.services.fieldquery.MCRResults;


/**
 * This class contains MyCoRe Webservices
 *
 * @author Harald Richter
 *
 * @version $Revision$ $Date$
 *
 */
public class MCRWebService 
{
  private static final Logger logger = Logger.getLogger( MCRWebService.class);
  private static final MCRConfiguration config = MCRConfiguration.instance();
  
  private static MCRXMLTableManager TM = MCRXMLTableManager.instance();
	
  /**
   * Retrieves MyCoRe object   
   *
   * @param ID The ID of the document to retrieve   
   * 
   * @return data of miless document
   * 
   **/  
  public org.w3c.dom.Document MCRDoRetrieveObject(String ID)
  {
    try
    {
      
      // check the ID and retrieve the data
      MCRXMLContainer result = new MCRXMLContainer();
      MCRObjectID mcrid = null;
      org.jdom.Document d = null;

      try {
          mcrid = new MCRObjectID(ID);

          byte[] xml = TM.retrieve(mcrid);
          result.add("local", ID, 0, xml);
          d = result.exportAllToDocument();
      } catch (MCRException e) {
          logger.warn(this.getClass() + " The ID " + ID + " is not a MCRObjectID!");
          org.jdom.Element root = new org.jdom.Element( "MCRWebServiceError" );
          root.setAttribute("type", "MCRDoRetrieveObject");
          d = new org.jdom.Document( root );
        
          root.addContent( e.getMessage() );
      }
      
      
      org.jdom.output.DOMOutputter doo = new org.jdom.output.DOMOutputter();
      
      if( logger.isDebugEnabled() )
      {
        org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
        logger.debug( outputter.outputString( d ) );
      }

      return doo.output(d);
    } catch (Exception ex)
    {
      ex.printStackTrace();
    }
    return null;
  }

}
