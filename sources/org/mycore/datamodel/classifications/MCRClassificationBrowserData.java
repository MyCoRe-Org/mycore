/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * Copyright (C) 2000 University of Essen, Germany
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
 * along with this program, normally in the file sources/gpl.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.datamodel.classifications;

import org.mycore.common.MCRConfiguration;
import java.util.*;
import org.apache.log4j.Logger;
import org.jdom.*;


/**
 * Instances of MCRClassificationBrowser contain the data of the currently displayed
 * navigation tree. MCRClassificationBrowser uses one MCRClassificationBrowserData instance per
 * browser session to store and update the category lines to be displayed.
 *
 * @author Anja Schaar
 *
 */
public class MCRClassificationBrowserData
{
  protected boolean        				showComments;
  protected String						pageName;
  protected String						xslStyle;
  protected String 						uri;

  private 	static 	MCRConfiguration 	config;
  private 	static 	Logger 				logger=Logger.getLogger(MCRClassificationBrowserData.class);
  private   MCRCategoryItem[] 			categItem ;
  private 	Vector         				lines;
  private 	MCRClassificationItem 		classif;
  private   String						startPath;
  private   String						actItemID, lastItemID;
  private   String[]					categFields;
  private   String						emptyLeafs;
  private   String						view;
  private   String						comments;




  public MCRClassificationBrowserData( String u )   throws Exception
  {
  	uri = u;
	config = MCRConfiguration.instance();
	String classifID = "";

	logger.info( this.getClass() + " incomming Path " + uri);
	String[] uriParts = mySplit();
	logger.info( this.getClass() + " Start" );
	if ( uriParts.length > 1) {
		logger.debug( this.getClass() + " PathParts - classification " + uriParts[1]);
		logger.debug( this.getClass() + " Number of PathParts =" + uriParts.length);
		classifID = config.getString( "MCR.ClassificationBrowser." + uriParts[1] +".Classification" );
		pageName  = config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".EmbeddingPage" );
		xslStyle  = config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".Style" );
		emptyLeafs= config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".EmptyLeafs" );
		view  	  = config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".View" );
		comments  = config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".Comments" );
		startPath = uriParts[1];
	}
	setClassification(classifID);
	clearPath(uriParts );
	setActualPath();
	if ( view == null || !view.endsWith("tree") ) 	view = "flat";
	if ( comments == null ) 		 				comments = "false";
	showComments  = comments.endsWith("true")?true:false;

	logger.info( this.getClass() + " SetClassification " + classifID);
	logger.info( this.getClass() + " Leere Knoten auslassen: " + emptyLeafs );
	logger.info( this.getClass() + " Darstellung: " + view );
	logger.info( this.getClass() + " Kommentare: " + comments );

  }

  private String[]  mySplit( )   throws Exception {
        //entspricht:  String[]  uriParts = uri.split("/");
        if (uri.endsWith("/")) uri = uri.substring(0, uri.length()-1);
        String[] tmpP = new String[uri.length()];
        int pCount =0;
        int first = uri.indexOf ( "/" );
        logger.debug( this.getClass() + " Index " + first);
        while( first >= 0 && first+1 < uri.length() ) {
                logger.info( this.getClass() + " Länge " + uri.length());
                //logger.info( this.getClass() + " tmpP " + tmpP[pCount] );
                tmpP[pCount] = uri.substring(0, first);
                uri = uri.substring(first+1);
                first = uri.indexOf ( "/" );
                pCount++;
                logger.info( this.getClass() + " uri " + uri);
                logger.info( this.getClass() + " Index " + first);
        }
        tmpP[pCount] = uri;
        logger.debug( this.getClass() + " pCount " + pCount);

        String[] Parts = new String[pCount+1];
        for (int i=0; i<=pCount; i++ ) Parts[i]=tmpP[i];

	return Parts;
  }

  private void setClassification( String classifID )   throws Exception
  {
    lines       	= new Vector();
    classif 		= MCRClassificationItem.getClassificationItem(classifID);
	categItem = classif.getChildren();

    for( int i = 0,j = classif.getNumChildren(); i<j; i++) {
      lines.addElement( new MCRNavigTreeLine( categItem[i] , 1 ) );
    }
  }

  private void clearPath(String[] uriParts )  throws Exception {
  	String[] cati = new String[uriParts.length];
  	String   path = "";
	int len =0;
    // pfad bereinigen
	for ( int k=2; k< uriParts.length; k++ ) {
		  logger.debug( this.getClass() + " uriParts[k]=" + uriParts[k] + " k=" + k);
		  if ( uriParts[k].length()>0 ){
		  	  if (uriParts[k].equalsIgnoreCase("..") && len > 0) {
		  	  	len--;
		  	  }else {
				cati[len]= uriParts[k];
				len ++;
		  	  }
		  }
		  logger.debug( this.getClass() + " cati[len]=" + cati[len] + " len=" + len);
	}

	//reinitialisieren
	categFields = new String[len];
	for ( int i=0; i < len ; i++ ){
		categFields[i]= cati[i];
		path += categFields[i]+ (i+1<categFields.length?"/":"");
	}
	uri = new String( uriParts[1] + "/" + path);
  }

  private void setActualPath(  )   throws Exception {
	actItemID = lastItemID ="";
	for ( int k=0; k< categFields.length; k++) {
		update(categFields[k]);
		lastItemID=actItemID;
		actItemID= categFields[k];
	}
	logger.debug( this.getClass() + " lastItemID " + lastItemID);
	logger.debug( this.getClass() + " actItemID " + actItemID);
	logger.debug( this.getClass() + " setActualPath OK" );
  }


  /**
   * Returns true if category comments for the classification
   * currently displayed should be shown.
   */
  public boolean showComments()
  { return showComments; }


  /**
   * Returns the pageName for the classification
   */
  public String getPageName()
  { return pageName; }

  /**
	 * Returns the xslStyle for the classification
	 */
  public String getXslStyle()
  { return xslStyle; }

  public MCRClassificationItem getClassification() {
   return  classif;
  }

  public MCRNavigTreeLine getLine( int i )  {
    if( i >= lines.size() )
      return null;
    else
      return (MCRNavigTreeLine)( lines.elementAt( i ) );
  }

  public org.jdom.Document loadTreeIntoSite(org.jdom.Document cover,
  		org.jdom.Document  browser ){

		Element placeholder = cover.getRootElement().getChild( "classificationBrowser" );
  		logger.info( this.getClass() + " Found Entry at " + placeholder);
	    if ( placeholder != null ) {
			List children = browser.getRootElement().getChildren();
			for( int j = 0; j < children.size(); j++ )	{
			    Element child = (Element)( (Element)( children.get( j ) ) ).clone();
				placeholder.addContent( child );
			}
	    }
		logger.debug(cover);
    	return cover;
  }

  /**
	* Creates an XML representation of MCRClassificationBrowserData
	* @author Anja Schaar
	*
	*/
   public org.jdom.Document  createXmlTree( String lang )
	 throws Exception
   {

	 MCRClassificationItem cl = getClassification();
     //MCRLinkTableManager clm = new MCRLinkTableManager();
     //clm.countCategoryReferencesFuzzy(cl.getClassificationID(), "");
	 Element xDocument = new Element( "classificationBrowse" );
	 logger.info(cl.toString());

	 Element xID = new Element( "classifID" );
	 xID.addContent(cl.getClassificationID());
	 xDocument.addContent( xID );

	 Element xLabel = new Element( "label" );
	 xLabel.addContent(cl.getText(lang));
	 xDocument.addContent( xLabel );

	 Element xDesc = new Element( "description" );
	 xDesc.addContent(cl.getDescription(lang));
	 xDocument.addContent( xDesc );

	 Element xDocuments = new Element( "cntDocuments" );
	 xDocuments.addContent( String.valueOf( cl.countDocLinks() ) );
	 xDocument.addContent( xDocuments );

	 Element xShowComments = new Element( "showComments" );
	 xShowComments.addContent( String.valueOf( showComments() ) );
	 xDocument.addContent( xShowComments );

	 Element xUri = new Element( "uri" );
	 xUri.addContent(uri);
	 xDocument.addContent( xUri );

	 Element xStartPath = new Element( "startPath" );
	 xStartPath.addContent(startPath);
	 xDocument.addContent( xStartPath );

	 // data as XML from outputNavigationTree
	 Element xNavtree = new Element( "navigationtree" );
	 xNavtree.setAttribute ( "classifID", cl.getClassificationID() );
	 xNavtree.setAttribute ( "categID", actItemID );
	 xNavtree.setAttribute ( "predecessor",  lastItemID);
	 xNavtree.setAttribute ( "emptyLeafs",  emptyLeafs);
	 xNavtree.setAttribute ( "view",  view);

	 xDocument.addContent( xNavtree );

	 int i = 0;
	 MCRNavigTreeLine line;

	 while( ( line = getLine( i++ ) ) != null )
	 {
	   int numDocs =  line.cat.countDocLinks();
	   logger.info( this.getClass() + " NumDocs - " + numDocs );

	   if ( emptyLeafs.endsWith("no") && numDocs == 0    ) {
		    logger.debug( this.getClass() + " empty Leaf continue - " + emptyLeafs );
	   		continue;
	   }
	   Element xRow = new Element( "row" );
	   xNavtree.addContent( xRow );
	   Element xCol = new Element( "col" );
	   xRow.addContent( xCol );
	   xCol.setAttribute( "lineLevel",  String.valueOf( line.level - 1 ) );

	   if( line.status.equals( "T" ) )
	   {
		 xCol.setAttribute( "plusminusbase",  line.cat.getID() );
		 xCol.setAttribute( "folder1",  "folder_plus" );
		 xCol.setAttribute( "folder2", numDocs > 0 ? "folder_closed_in_use" : "folder_closed_empty" );
	   }
	   else if( line.status.equals( "F" ) )
	   {
		 xCol.setAttribute( "plusminusbase",  line.cat.getID() );
		 xCol.setAttribute( "folder1",  "folder_minus" );
		 xCol.setAttribute( "folder2", numDocs > 0 ? "folder_open_in_use" : "folder_open_empty" );
	   }
	   else
	   {
		 xCol.setAttribute( "folder1", "folder_plain" );
		 xCol.setAttribute( "folder2", numDocs > 0 ? "folder_closed_in_use" : "folder_closed_empty" );
	   }

	   int numLength = String.valueOf( numDocs ).length();

	   xCol = new Element( "col" );
	   xRow.addContent( xCol );
	   xCol.setAttribute( "numDocs",  String.valueOf( numDocs ) );

	   if( numLength > 0 )
	   {
		 String search ="/"+uri;
		 if ( line.cat.getID().equalsIgnoreCase(actItemID)	 )
			 search+= "/..";
		 else
		 	 search+= "/" +line.cat.getID();

		 if ( search.indexOf("//") > 0 )
			search = search.substring(0, search.indexOf("//")) + search.substring(search.indexOf("//")+1);
		 //search = search.replaceAll("//","/");
		 xCol.setAttribute( "searchbase",  search );
		 xCol.setAttribute( "lineID",  line.cat.getID() );
	   }

	   xCol.addContent( line.cat.getText(lang));

	   if( showComments() && ( line.cat.getDescription(lang) != null)  )  {
		 Element comment = new Element( "comment" );
		 xCol.addContent( comment );
		 comment.setText(line.cat.getDescription(lang) );
	   }

	 }
	 return new org.jdom.Document( xDocument );
  }


  public void update( String categID )    throws Exception  {
    int     lastLevel = 0;
    boolean hideLevel = false;

	logger.debug( this.getClass() + " update CategoryTree for: " + categID);
    for( int i = 0; i < lines.size(); i++ )
    {
      MCRNavigTreeLine 	line = getLine( i );
	  hideLevel = hideLevel && ( line.level > lastLevel );
	  logger.debug( this.getClass() + " compare CategoryTree on " +i + "_" +line.cat.getID() + " to " + categID);


      if ( view.endsWith("tree")) {
      	if ( hideLevel) {
			lines.removeElementAt( i-- );
      	} else if( categID.equals( line.cat.getID() ) )
      	{
			if( line.status.equals( "F" ) ) // hide expanded category children
			{
			   line.status = "T";
			   hideLevel  = true;
			   lastLevel  = line.level;
			} else if( line.status.equals( "T" ) ) // expand category children
			{
			  line.status = "F";
			  MCRCategoryItem[] children = line.cat.getChildren();
			  for( int j = 0, k= children.length;  j < k ; j++ )    {
					MCRCategoryItem cat = children[j] ;
					lines.insertElementAt( new MCRNavigTreeLine( cat, line.level + 1 ), ++i );
			  }
      		}
      	}
      } else {
      	if( categID.equalsIgnoreCase(line.cat.getID() ) )  {
      		line.level=0;
		    logger.info( this.getClass() + " expand " +line.cat.getID());
			line.status = "F";
		    MCRCategoryItem[] children = line.cat.getChildren();
          	for( int j = 0, k= children.length;  j < k ; j++ )    {
			  MCRCategoryItem cat = children[j] ;
          	  lines.insertElementAt( new MCRNavigTreeLine( cat, line.level + 1 ), ++i );
          	}
          }
         else {
			logger.debug( this.getClass() + " remove lines " + i + "_" +line.cat.getID() );
			lines.removeElementAt( i-- );
	  	  }
	   }
    }
  }
}
