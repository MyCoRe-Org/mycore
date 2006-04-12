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

package org.mycore.services.webservices;


import org.apache.log4j.Logger;

import org.mycore.datamodel.metadata.MCRXMLTableManager;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;

/**
 * This class contains MyCoRe Webservices
 *
 * @author Harald Richter
 *
 * @version $Revision$ $Date$
 *
 */
public class MCRWebService implements MCRWS 
{
  private static final Logger logger = Logger.getLogger( MCRWebService.class);
  
  private static MCRXMLTableManager TM = MCRXMLTableManager.instance();
	
  /* (non-Javadoc)
   * @see MCRWS#MCRDoRetrieveObject(java.lang.String)
   */  
  public org.w3c.dom.Document MCRDoRetrieveObject(String ID)
  {
    try
    {

      // check the ID and retrieve the data
      MCRXMLContainer result = new MCRXMLContainer();
      MCRObjectID mcrid = null;
      org.jdom.Document d = null;

      try
      {
        mcrid = new MCRObjectID(ID);

        byte[] xml = TM.retrieve(mcrid);
        if ( null != xml )
        {
          result.add("local", ID, 0, xml);
          d = result.exportAllToDocument();
        }
        else
        {
          String msg = this.getClass() + " MCRobject with ID '" + ID + "' not found";
          logger.warn( msg );
          org.jdom.Element root = new org.jdom.Element("MCRWebServiceError");
          root.setAttribute("type", "MCRDoRetrieveObject");
          d = new org.jdom.Document(root);

          root.addContent( msg );
        }
      } catch (Exception ex)
      {
        logger.warn(this.getClass() + " MCRobject with ID '" + ID + "' not found");
        org.jdom.Element root = new org.jdom.Element("MCRWebServiceError");
        root.setAttribute("type", "MCRDoRetrieveObject");
        d = new org.jdom.Document(root);

        root.addContent(ex.getMessage());
      }

      org.jdom.output.DOMOutputter doo = new org.jdom.output.DOMOutputter();

      if (logger.isDebugEnabled())
      {
        org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
        logger.debug(outputter.outputString(d));
      }

      return doo.output(d);
    } catch (Exception ex)
    {
      ex.printStackTrace();
    }
    return null;
  }
  
  /* (non-Javadoc)
   * @see MCRWS#MCRDoRetrieveClassification(java.lang.String, java.lang.String, java.lang.String)
   */  
  public org.w3c.dom.Document MCRDoRetrieveClassification( String level, String type,
      String classID, String categID)
  {
    try
    {
      String uri  = "classification:metadata:" + level + ":" + type + ":" + classID + ":" + categID;
      org.jdom.Document d = null;

      try
      {
        org.jdom.Element cl = MCRURIResolver.instance().resolve( uri );
        d                   = cl.getDocument(); //new org.jdom.Document( cl );
      } catch (Exception ex)
      {
        ex.printStackTrace();
        logger.warn(this.getClass() + " Classification  '" + classID + "' with level '" + level + "' and category '" + categID + "' not found");
        org.jdom.Element root = new org.jdom.Element("MCRWebServiceError");
        root.setAttribute("type", "MCRDoRetrieveClassification");
        d = new org.jdom.Document(root);

        root.addContent(ex.getMessage());
      }

      org.jdom.output.DOMOutputter doo = new org.jdom.output.DOMOutputter();

      if (logger.isDebugEnabled())
      {
        org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
        logger.debug(outputter.outputString(d));
      }

      return doo.output(d);
    } catch (Exception ex)
    {
      ex.printStackTrace();
    }
    return null;
  }
  
  /* (non-Javadoc)
   * @see MCRWS#MCRDoQuery(org.w3c.dom.Document)
   */  
  public org.w3c.dom.Document MCRDoQuery(org.w3c.dom.Document query)
  {
    org.jdom.Document doc    = null;
    org.jdom.Document result = null;
    try
    {
      org.jdom.input.DOMBuilder d = new org.jdom.input.DOMBuilder();
      doc = d.build(query);

      if (logger.isDebugEnabled())
      {
        org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
        logger.debug(outputter.outputString(doc));
      }
      
      // Execute query
      MCRResults res = MCRQueryManager.search(doc);

      result = new org.jdom.Document(res.buildXML());

    } catch (Exception ex)
    {
      org.jdom.Element root = new org.jdom.Element("MCRWebServiceError");
      root.setAttribute("type", "MCRDoQuery");
      result = new org.jdom.Document(root);

      root.addContent(ex.toString());
      root.addContent(doc.detachRootElement());
    }

    try
    {
      org.jdom.output.DOMOutputter doo = new org.jdom.output.DOMOutputter();
      return doo.output(result);
    } catch (Exception ex)
    {
      ex.printStackTrace();
    }
    return null;
  }

}
