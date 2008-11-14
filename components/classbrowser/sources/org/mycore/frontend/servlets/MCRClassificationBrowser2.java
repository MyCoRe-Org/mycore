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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRQueryParser;

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
    
    String classifID  = req.getParameter( "classification" ); // Classification ID
    String categID    = req.getParameter( "category" );       // Category ID to start with

    String objectType  = req.getParameter( "objectType" );  // MCRObject type to search and count
    String field       = req.getParameter( "field" );       // Search field that maps category
    String restriction = req.getParameter( "restriction" ); // Additional query expression
    String parameters  = req.getParameter( "parameters" );  // MCRSearchServlet maxResults=x&numPerPage=y&...

    boolean countResults = Boolean.valueOf( req.getParameter( "countResults" ) ); // count query results?
    boolean countLinks   = Boolean.valueOf( req.getParameter( "countLinks" ) );   // count category links?
    
    boolean uri   = Boolean.valueOf( req.getParameter( "addURI" ) ); // if true, add uri
    boolean descr = Boolean.valueOf( req.getParameter( "addDescription" ) ); // if true, add description
    
    LOGGER.info( "ClassificationBrowser " + classifID + " " + ( categID == null ? "" : categID ) );   

    MCRCategoryID id = new MCRCategoryID( classifID, categID );
    List<MCRCategory> children = MCRCategoryDAOFactory.getInstance().getChildren(id);
    Element xml = new Element( "classificationBrowserData" );
    xml.setAttribute( "classification", classifID );

    MCRAndCondition queryCondition = new MCRAndCondition();
    MCRQueryCondition categCondition = new MCRQueryCondition( MCRFieldDef.getDef( field ), "=", "DUMMY" );
    queryCondition.addChild( categCondition );

    if( ( objectType != null ) && ( objectType.trim().length() > 0 ) )
    {
      xml.setAttribute( "objectType", objectType );
      MCRCondition cond = new MCRQueryCondition( MCRFieldDef.getDef( "objectType" ), "=", objectType );
      queryCondition.addChild( cond );
    }
    if( ( restriction != null ) && ( restriction.trim().length() > 0 ) )
    { 
      MCRCondition cond = new MCRQueryParser().parse( restriction );
      queryCondition.addChild( cond );
    }
    
    if( parameters != null ) xml.setAttribute( "parameters", parameters );
    
    List<Element> data = new ArrayList<Element>();
    for( MCRCategory child : children )
    {
      Element category = new Element( "category" );
      data.add( category );

      String childID = child.getId().getID();

      category.setAttribute( "id", childID );
      category.setAttribute( "children", Boolean.toString( child.hasChildren() ) );

      categCondition.setValue( childID );
      category.setAttribute( "query", queryCondition.toString() );

      if( countResults )
      {
        int numResults = MCRQueryManager.search( new MCRQuery( queryCondition ) ).getNumHits();
        category.setAttribute( "numResults", String.valueOf( numResults ) );
      }
      
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
    
    if( countLinks )
    {
      if( objectType.trim().length() == 0 ) objectType = null;
      Map<MCRCategoryID,Number> count = MCRCategLinkServiceFactory.getInstance().countLinksForType(id, objectType);
      for( Element child : data )
      {
        MCRCategoryID childID = new MCRCategoryID( classifID, child.getAttributeValue( "id" ) );
        Number num = count.get( childID );
        if( num != null )
          child.setAttribute( "numLinks", String.valueOf( num.intValue() ) );
        else
          child.setAttribute( "numLinks", "0" );
      }
    }
    
    // Sort categories by id, by label or keep natural sort order
    final String sortBy = req.getParameter( "sortBy" ); // id, label or nothing ("")
    if( sortBy != null ) Collections.sort( data, new Comparator<Element>()
    {        
      public int compare( Element a, Element b )
      {
        if( "id".equals( sortBy ) )
          return( a.getAttributeValue( "id" ).compareTo( b.getAttributeValue( "id" ) ) );
        else if( "label".equals( sortBy ) )
          return( a.getChildText( "label" ).compareToIgnoreCase( b.getChildText( "label" ) ) );
        else
          return 0;
      }
    } );
    xml.addContent( data );

    String style = req.getParameter( "style" ); // XSL.Style, optional
    if( ( style != null ) && ( style.length() > 0 ) )
      req.setAttribute( "XSL.Style", style );

    MCRServlet.getLayoutService().doLayout( req, job.getResponse(), new Document( xml ) );
    time = ( System.nanoTime() - time ) / 1000000;
    LOGGER.info( "ClassificationBrowser finished in " + time + " ms" );
  }
}
