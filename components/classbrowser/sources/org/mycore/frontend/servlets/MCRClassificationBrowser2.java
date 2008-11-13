/*
 * 
 * $Revision: 13278 $ $Date: 2008-03-17 17:12:15 +0100 (Mo, 17 Mrz 2008) $
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

package org.mycore.frontend.servlets;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * This servlet provides a way to visually navigate through the tree of
 * categories of a classification. The XML output is transformed to HTML
 * using classificationBrowserData.xsl on the server side, then sent to
 * the client browser, where AJAX does the rest.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRClassificationBrowser2 extends MCRServlet 
{
  private static final long serialVersionUID = 1L;
  
  private static final Logger LOGGER = Logger.getLogger( MCRClassificationBrowser2.class );
  
  private static final String defaultLang = MCRConfiguration.instance().getString( "MCR.Metadata.DefaultLang", "en" );

  public void doGetPost( MCRServletJob job ) throws Exception 
  {
    long time = System.nanoTime();
    
    String currentLang = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
    
    HttpServletRequest req = job.getRequest();
    
    String classifID = req.getParameter( "classification" ); // Classification ID
    String categID   = req.getParameter( "category" );       // Category ID to start with
    LOGGER.info( "ClassificationBrowser " + classifID + ( categID == null ? "" : categID ) );   
    
    boolean uri   = Boolean.valueOf( req.getParameter( "uri" ) ); // if true, add uri
    boolean descr = Boolean.valueOf( req.getParameter( "description" ) ); // if true, add description
    
    MCRCategoryID id = new MCRCategoryID( classifID, categID );
    List<MCRCategory> children = MCRCategoryDAOFactory.getInstance().getChildren(id);
    Element xml = new Element( "classificationBrowserData" );
    xml.setAttribute( "classification", classifID );
    for( MCRCategory child : children )
    {
      Element category = new Element( "category" );
      xml.addContent( category );

      category.setAttribute( "id", child.getId().getID() );
      category.setAttribute( "children", Boolean.toString( child.hasChildren() ) );
      
      if( uri && ( child.getURI() != null ) ) 
        category.addContent( new Element( "uri" ).setText( child.getURI().toString() ) );

      // Get label in current lang, otherwise default lang, otherwise whatever available
      Map<String,MCRLabel> labels = child.getLabels();
      MCRLabel label = labels.get( currentLang );
      if( label == null ) label = labels.get( defaultLang );
      if( label == null ) label = labels.entrySet().iterator().next().getValue();
      
      category.addContent( new Element( "label" ).setText( label.getText() ) );
      if( descr && ( label.getDescription() != null ) ) 
        category.addContent( new Element( "description" ).setText( label.getDescription() ) );
    }

    String style = req.getParameter( "style" ); // XSL.Style, optional
    if( ( style != null ) && ( style.length() > 0 ) )
      req.setAttribute( "XSL.Style", style );

    MCRServlet.getLayoutService().doLayout( req, job.getResponse(), new Document( xml ) );
    time = ( System.nanoTime() - time ) / 1000000;
    LOGGER.debug( "ClassificationBrowser finished in " + time + " ms" );
  }
}
