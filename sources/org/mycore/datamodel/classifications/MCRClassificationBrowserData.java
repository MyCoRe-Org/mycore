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

package org.mycore.datamodel.classifications;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.user.MCRUserMgr;
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
  private 	static 	Logger 				LOGGER=Logger.getLogger(MCRClassificationBrowserData.class);
  private   MCRCategoryItem[] 			categItem ;
  private 	Vector         				lines;
  private 	MCRClassificationItem 		classif;
  private   String						startPath	="";
  private   String						actItemID	="";
  private 	String 						lastItemID	="";
  private   String[]					categFields;
  private   String						emptyLeafs	=null;
  private   String						view		=null;
  private   String						comments	=null;
  private   String						searchField	="";
  private   String						sort		=null;
  private   String						doctype		=null;
  private   String[]					doctypeArray=null;
  private   String						restriction	=null;
  int 		maxlevel = 0;
  int 		totalNumOfDocs = 0;
 

  public MCRClassificationBrowserData( String u , String mode, String actclid, String actEditorCategid)   throws Exception  {
  	uri = u;
	config = MCRConfiguration.instance();

	LOGGER.info( this.getClass() + " incomming Path " + uri);
	String[] uriParts = uri.split("/");		//	mySplit();
	LOGGER.info( this.getClass() + " Start" );
	String classifID = "";
	
	if (uriParts.length <= 1) {
		LOGGER.debug( this.getClass() + " PathParts - classification is default");
		pageName     = config.getString( "MCR.ClassificationBrowser.default.EmbeddingPage" );
		xslStyle     = config.getString( "MCR.ClassificationBrowser.default.Style" );
		emptyLeafs   = config.getString( "MCR.ClassificationBrowser.default.EmptyLeafs" );
		view  	     = config.getString( "MCR.ClassificationBrowser.default.View" );
		doctype      = config.getString( "MCR.ClassificationBrowser.default.Doctype" );
		comments     = config.getString( "MCR.ClassificationBrowser.default.Comments" );
		searchField  = config.getString( "MCR.ClassificationBrowser.default.searchField" );
		classifID    = actclid;
		startPath = "default";		
	}
	else {
		LOGGER.debug( this.getClass() + " PathParts - classification " + uriParts[1]);
		LOGGER.debug( this.getClass() + " Number of PathParts =" + uriParts.length);
		try {
			classifID    = config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".Classification" );
		} catch (org.mycore.common.MCRConfigurationException noClass){
			classifID    = actclid;	
		}
		try {
			pageName     = config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".EmbeddingPage" );
		} catch (org.mycore.common.MCRConfigurationException noPagename){
			pageName     = config.getString( "MCR.ClassificationBrowser.default.EmbeddingPage" );
		}
		try {
			xslStyle     = config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".Style" );
		} catch (org.mycore.common.MCRConfigurationException noStyle){
			xslStyle     = config.getString( "MCR.ClassificationBrowser.default.Style" );			
		}
		try {
			searchField  = config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".searchField" );
		} catch (org.mycore.common.MCRConfigurationException noSearchfield){
			searchField  = config.getString( "MCR.ClassificationBrowser.default.searchField" );
		}

		try {
			emptyLeafs= config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".EmptyLeafs" );
		} catch (org.mycore.common.MCRConfigurationException noEmptyLeafs){
			emptyLeafs= config.getString( "MCR.ClassificationBrowser.default.EmptyLeafs" );
		}
		try {
			view  	  = config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".View" );
		} catch (org.mycore.common.MCRConfigurationException noView){
			view  	  = config.getString( "MCR.ClassificationBrowser.default.View" );
		}
		try {
			doctype   = config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".Doctype" );
		} catch (org.mycore.common.MCRConfigurationException noDoctype){
			doctype   = config.getString( "MCR.ClassificationBrowser.default.Doctype" );
		}
		try {
			sort      = config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".Sort" );
			comments  = config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".Comments" );
			restriction  = config.getString( "MCR.ClassificationBrowser."+ uriParts[1] +".Restriction" );
		} catch (org.mycore.common.MCRConfigurationException ig){
			// ignore for this parameters, the are optionally
			;
		}
		startPath = uriParts[1];		
	} 
	
	if ("edit".equals(mode) ){
		pageName  = config.getString( "MCR.classeditor.EmbeddingPage" );
		xslStyle  = config.getString( "MCR.classeditor.Style" );
		sort      = "false";
		view	  = "tree";
		
		if ( classifID.length() == 0) {
			return;
		}
	}
	
	if ( emptyLeafs == null ) 						emptyLeafs = "yes";
	if ( view == null || !view.endsWith("flat") ) 	view = "tree";
	if ( comments == null ) 		 				comments = "false";

	setClassification(classifID);
	clearPath(uriParts );
	setActualPath(actEditorCategid);

	if ( doctype  != null ) {
		try {
			String typelist = config.getString( "MCR.type_"+ doctype);
			doctypeArray=typelist.split(",");
		} catch  (Exception allignore){
			LOGGER.info( this.getClass() + "No search type was set - it seams to be ok"  );
		}
	}
	showComments  = comments.endsWith("true")?true:false;

	LOGGER.info( this.getClass() + " SetClassification " + classifID);
	LOGGER.info( this.getClass() + " Leere Knoten auslassen: " + emptyLeafs );
	LOGGER.info( this.getClass() + " Darstellung: " + view );
	LOGGER.info( this.getClass() + " Kommentare: " + comments );
	LOGGER.info( this.getClass() + " Doctypes: " + doctype );
	LOGGER.info( this.getClass() + " Restriction: " + restriction );
	LOGGER.info( this.getClass() + " Sortiert: " + sort );
  }
	

  private void setClassification( String classifID )   throws Exception
  {
    lines       	= new Vector();    
    classif 		= MCRClassificationItem.getClassificationItem(classifID);
    categItem 		= classif.getChildrenFromJDom();    //classif.getChildren();
    
    
    for( int i = 0,j = classif.getNumChildren(); i<j; i++) {
      lines.addElement( new MCRNavigTreeLine( categItem[i] , 1 )) ;
    }
        
  }

  private void clearPath(String[] uriParts )  throws Exception {
  	String[] cati = new String[uriParts.length];
  	String   path = "";
	int len =0;
    // pfad bereinigen
	for ( int k=2; k< uriParts.length; k++ ) {
		  LOGGER.debug( this.getClass() + " uriParts[k]=" + uriParts[k] + " k=" + k);
		  if ( uriParts[k].length()>0 ){
		  	  if (uriParts[k].equalsIgnoreCase("..") && len > 0) {
		  	  	len--;
		  	  }else {
				cati[len]= uriParts[k];
				len ++;
		  	  }
		  }
		  LOGGER.debug( this.getClass() + " cati[len]=" + cati[len] + " len=" + len);
	}

	//reinitialisieren
	categFields = new String[len];
	for ( int i=0; i < len ; i++ ){
		categFields[i]= cati[i];
		path += categFields[i]+ (i+1<categFields.length?"/":"");
	}
	uri = new String( uriParts[1] + "/" + path);
  }
  

  private void setActualPath( String actEditorCategid )   throws Exception {
	actItemID = lastItemID ="";
	for ( int k=0; k< categFields.length; k++) {
		update(categFields[k]);
		lastItemID=actItemID;
		actItemID= categFields[k];
	}
	if (actEditorCategid != null){
		actItemID = lastItemID =actEditorCategid;
	}
	LOGGER.debug( this.getClass() + " lastItemID " + lastItemID);
	LOGGER.debug( this.getClass() + " actItemID " + actItemID);
	LOGGER.debug( this.getClass() + " setActualPath OK" );
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
  		LOGGER.info( this.getClass() + " Found Entry at " + placeholder);
	    if ( placeholder != null ) {
			List children = browser.getRootElement().getChildren();
			for( int j = 0; j < children.size(); j++ )	{
			    Element child = (Element)( (Element)( children.get( j ) ) ).clone();
				placeholder.addContent( child );
			}
	    }
		LOGGER.debug(cover);
    	return cover;
  }
  
  public org.jdom.Document  createXmlTreeforAllClassifications(  )	 throws Exception {
	 MCRClassificationManager clm = new MCRClassificationManager();
 	 MCRClassificationItem[] clI = clm.getAllClassification();
 	 
	 Element xDocument = new Element( "classificationbrowse" );
	 MCRSession mcrSession 	= MCRSessionMgr.getCurrentSession();
	 String	userid = mcrSession.getCurrentUserID();         
	 ArrayList privs = MCRUserMgr.instance().retrieveAllPrivsOfTheUser(userid);
	 Element EditClassbutton = new Element( "userCanEdit" );
	 EditClassbutton.addContent( String.valueOf(privs.contains("create-classification" ) || privs.contains("delete-classification")) );	 
	 xDocument.addContent( EditClassbutton );
		 
	 Element xNavtree = new Element( "classificationlist" );
	 xDocument.addContent( xNavtree );
	 String browserClass = "";
	 String Counter = "";
	 
	 for(int i=0; i<clI.length && clI[i] != null; i++ ) {
		  Element cli = clI[i].getClassificationItemAsJDom();
		  try {
			  browserClass  = config.getString( "MCR.classeditor." + clI[i].getClassificationID() );
		  } catch(Exception ignore){
			  browserClass = "default";
		  }
		  cli.setAttribute("browserClass", browserClass);				  
		  try {
			  String typelist = config.getString( "MCR.type_alldocs");
			  doctypeArray=typelist.split(",");			  
			  Counter = Integer.toString(clI[i].countDocLinks(doctypeArray,""));
		  } catch(Exception ignore){
			  Counter = "NaN";
		  }
		  cli.setAttribute("counter", Counter);			  
		  xNavtree.addContent(cli);
	 }
	 
	 return new Document( xDocument );
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
 	 Element xDocument = new Element( "classificationBrowse" );
	 LOGGER.info(cl.getClassificationID());

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
	 xDocuments.addContent(String.valueOf( cl.countDocLinks(doctypeArray, restriction)) );
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

	 // Editierbutton Einfügen - wenn das privieg es erlaubt
	 MCRSession mcrSession 	= MCRSessionMgr.getCurrentSession();
	 String	userid = mcrSession.getCurrentUserID();         
	 ArrayList privs = MCRUserMgr.instance().retrieveAllPrivsOfTheUser(userid);
	 Element Editbutton = new Element( "userCanEdit" );
	 Editbutton.addContent( String.valueOf( privs.contains("modify-classification")) );	 
	 xDocument.addContent( Editbutton );

	 // data as XML from outputNavigationTree
	 Element xNavtree = new Element( "navigationtree" );
	 xNavtree.setAttribute ( "classifID", cl.getClassificationID());
	 xNavtree.setAttribute ( "categID", actItemID );
	 xNavtree.setAttribute ( "predecessor",  lastItemID);
	 xNavtree.setAttribute ( "emptyLeafs",  emptyLeafs);
	 xNavtree.setAttribute ( "view",  view);
	 xNavtree.setAttribute ( "doctype", doctype!=null?doctype:"alldocs");
	 xNavtree.setAttribute ( "restriction", restriction!=null?restriction:"");
	 xNavtree.setAttribute ( "searchField", searchField);


	 int i = 0;
	 MCRNavigTreeLine line;	 

	 while( ( line = getLine( i++ ) ) != null )	 {
		 
	   int numDocs =line.cat.counter;		//line.cat.countDocLinks(doctypeArray,restriction);
	   //für Sortierung schon mal die leveltiefe bestimmen
	   LOGGER.info( this.getClass() + " NumDocs - " + numDocs );

	   if ( emptyLeafs.endsWith("no") && numDocs == 0    ) {
		    LOGGER.debug( this.getClass() + " empty Leaf continue - " + emptyLeafs );
	   		continue;
	   }
	   Element xRow = new Element( "row" );
	   Element xCol1 = new Element( "col" );
	   Element xCol2 = new Element( "col" );   
	   int numLength = String.valueOf( numDocs ).length();
	   
	   xRow.addContent(xCol1 );
	   xRow.addContent( xCol2 );
	   xNavtree.addContent( xRow );
	   
	   xCol1.setAttribute( "lineLevel",  String.valueOf( line.level - 1 ) );	   
	   xCol1.setAttribute( "childpos",  "middle" );
	   
	   if ( line.level > maxlevel)	 {
		   xCol1.setAttribute( "childpos",  "first" );
		   maxlevel = line.level;
		   if ( getLine(i) == null) {
			   // Spezialfall nur genau ein Element
			   xCol1.setAttribute( "childpos",  "firstlast" );
		   }
	   } else if ( getLine(i) == null) {
		   xCol1.setAttribute( "childpos",  "last" );
	   }
	   
	   xCol1.setAttribute( "folder1", "folder_plain" );
	   xCol1.setAttribute( "folder2", numDocs > 0 ? "folder_closed_in_use" : "folder_closed_empty" );
	   
	   if( line.status.equals( "T" ) )   {
		 xCol1.setAttribute( "plusminusbase",  line.cat.getID() );
		 xCol1.setAttribute( "folder1",  "folder_plus" );
	   } else if( line.status.equals( "F" ) )   {
		 xCol1.setAttribute( "plusminusbase",  line.cat.getID() );
		 xCol1.setAttribute( "folder1",  "folder_minus" );
		 xCol1.setAttribute( "folder2", numDocs > 0 ? "folder_open_in_use" : "folder_open_empty" );
	   }
	      
	   xCol2.setAttribute( "numDocs",  String.valueOf( numDocs ) );

	   if( numLength > 0 )	   {
		 String search ="/"+uri;
		 if ( line.cat.getID().equalsIgnoreCase(actItemID)	 )			 
			 	search+= "/..";
		 else	search+= "/" +line.cat.getID();

		 if ( search.indexOf("//") > 0 )
			search = search.substring(0, search.indexOf("//")) + search.substring(search.indexOf("//")+1);
		 
		 xCol2.setAttribute( "searchbase",  search );
		 xCol2.setAttribute( "lineID",  line.cat.getID() );
	   }

	   xCol2.addContent( line.cat.getText(lang));

	   if( showComments() && ( line.cat.getDescription(lang) != null)  )  {
		 Element comment = new Element( "comment" );
		 xCol2.addContent( comment );
		 comment.setText(line.cat.getDescription(lang) );
	   }
	   
	 }
	 xNavtree.setAttribute ( "rowcount", ""+i );
	 xDocument.addContent( xNavtree );
	 
     if ( "true".equals(sort) )
     	xDocument = sortMyTree(xDocument);
	 return new org.jdom.Document( xDocument );
  }


  public void update( String categID )    throws Exception  {
    int     lastLevel = 0;
    boolean hideLevel = false;

	LOGGER.debug( this.getClass() + " update CategoryTree for: " + categID);
    for( int i = 0; i < lines.size(); i++ )    {
      MCRNavigTreeLine 	line = getLine( i );
	  hideLevel = hideLevel && ( line.level > lastLevel );
	  LOGGER.debug( this.getClass() + " compare CategoryTree on " +i + "_" +line.cat.getID() + " to " + categID);


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
			  MCRCategoryItem[] children = line.cat.getChildrenFromJDom();				  //line.cat.getChildren();
			  for( int j = 0, k= children.length;  j < k ; j++ )    {
					MCRCategoryItem cat = children[j] ;
					lines.insertElementAt( new MCRNavigTreeLine( cat, line.level + 1 ), ++i );
			  }
      		}
      	}
      } else {
      	if( categID.equalsIgnoreCase(line.cat.getID() ) )  {
      		line.level=0;
		    LOGGER.info( this.getClass() + " expand " +line.cat.getID());
			line.status = "F";
		    MCRCategoryItem[] children = line.cat.getChildrenFromJDom();  // 	line.cat.getChildren();
          	for( int j = 0, k= children.length;  j < k ; j++ )    {
			  MCRCategoryItem cat = children[j] ;
          	  lines.insertElementAt( new MCRNavigTreeLine( cat, line.level + 1 ), ++i );
          	}
          }
         else {
			LOGGER.debug( this.getClass() + " remove lines " + i + "_" +line.cat.getID() );
			lines.removeElementAt( i-- );
	  	  }
	   }

    }
  }
  private Element sortMyTree(Element xDocument) {
	  	Element xDoc = (Element) xDocument.clone();
	  	for ( int i=0; i<maxlevel; i++) {
	  		xDoc = sortMyTreeperLevel(xDoc, i, 0 );
	  	}
	  	return xDoc;
	  }

  private Element sortMyTreeperLevel( Element xDocument, int activelevel, int position){
	  	Element xDoc = (Element) xDocument;
	  	Element aktRow = ( (Element) xDoc.getChild("navigationtree").getChildren().get(position));
	  	String  aktText = ( (Element) aktRow.getChildren().get(1)).getText();

		List children = xDoc.getChild("navigationtree").getChildren();
		int level = activelevel;
		int Cnt = 0;
		for( int j = position+1; j < children.size(); j++ )	{
			Element child = (Element)((Element) children.get( j ));
			Element col1 = (Element)( (Element) child ).getChildren().get(0);
			Element col2 = (Element)( (Element) child ).getChildren().get(1);

	  		try {
	  			level = col1.getAttribute("lineLevel").getIntValue();
	  		} catch (Exception ignored){;}

	  		String sText = col2.getText();

	  		if ( activelevel == level ) {
				if (aktText.compareTo(sText) > 0 ) {
					changeRows(xDoc, aktRow, child);
					boolean bjumpOverChilds=true;
					while ( bjumpOverChilds && j < children.size()-1 )
					{
						Element next = (Element)((Element) children.get( j+1));
						if ( next != null  ){
							Element colx = (Element)( (Element) child ).getChildren().get(0);
							int nextlevel = level;
					  		try {
					  			nextlevel = colx.getAttribute("lineLevel").getIntValue();
					  		} catch (Exception ignored){;}

					  		if ( nextlevel > level )
					  			j++;
					  		else bjumpOverChilds=false;
						}else
							bjumpOverChilds=false;
					}
					Cnt++;
	  				xDoc = sortMyTreeperLevel(xDoc, activelevel, position );
	  			}
	  		if ( position < children.size()-1 && j==children.size()-1 && Cnt==0)
	  			xDoc = sortMyTreeperLevel(xDoc, activelevel, position+1 );
	  		}
		}
	  	return  xDoc;
	  }

  private void changeRows(Element xDoc, Element aktRow, Element child ){

		Element col1 = (Element)( (Element) child ).getChildren().get(0);
		Element col2 = (Element)( (Element) child ).getChildren().get(1);
		Element placer  = (Element)( (Element) aktRow ).clone();
	  	Element place1  = (Element)( (Element) placer ).getChildren().get(0);
	  	Element place2  = (Element)( (Element) placer ).getChildren().get(1);

		Element xc1 = new Element("col");
		Element xc2 = new Element("col");

		aktRow.setContent(0, xc1);
		xc1.setAttribute( "lineLevel",  col1.getAttributeValue("lineLevel") );

		xc1.setAttribute( "childpos", col1.getAttributeValue( "childpos"));
		xc1.setAttribute( "folder1", col1.getAttributeValue( "folder1"));
		xc1.setAttribute( "folder2", col1.getAttributeValue("folder2"));
		if (col1.getAttributeValue("plusminusbase") != null )
			xc1.setAttribute( "plusminusbase",  col1.getAttributeValue("plusminusbase"));

		aktRow.setContent(1, xc2);
		xc2.setAttribute( "numDocs",  col2.getAttributeValue("numDocs") );
		xc2.setAttribute( "searchbase",  col2.getAttributeValue("searchbase") );
		if (col2.getAttributeValue("lineID") != null )
			xc2.setAttribute( "lineID",  col2.getAttributeValue("lineID") );
		xc2.addContent( col2.getText());

		Element xc3 = new Element("col");
		Element xc4 = new Element("col");

		child.setContent(0,xc3);
		xc3.setAttribute( "lineLevel", place1.getAttributeValue("lineLevel") );
		xc3.setAttribute( "childpos", place1.getAttributeValue( "childpos"));
		xc3.setAttribute( "folder1", place1.getAttributeValue( "folder1"));
		xc3.setAttribute( "folder2", place1.getAttributeValue("folder2"));
		if (place1.getAttributeValue("plusminusbase") != null )
			xc3.setAttribute( "plusminusbase", place1.getAttributeValue("plusminusbase"));

		child.setContent(1,xc4);
		xc4.setAttribute( "numDocs",  place2.getAttributeValue("numDocs") );
		xc4.setAttribute( "searchbase",  place2.getAttributeValue("searchbase") );
		if (place2.getAttributeValue("lineID") != null )
			xc4.setAttribute( "lineID",  place2.getAttributeValue("lineID") );
		xc4.addContent( place2.getText());
	  }

}
