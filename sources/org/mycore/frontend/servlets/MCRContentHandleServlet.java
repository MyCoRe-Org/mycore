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

package org.mycore.frontend.servlets;


import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.jdom.output.XMLOutputter;
import org.mycore.datamodel.ifs.MCRContentIndexer;
import org.mycore.datamodel.ifs.MCRContentIndexerFactory;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileImportExport;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.frontend.editor2.MCREditorSubmission;

/**
 * This class handles: searches on arbitrary xml files and generates resultset
 *                     shows single file from ifs
 *                     upload single file in ifs
 *
 * @author Harald Richter
 * @version $Revision$ $Date$
 *
 */
public final class MCRContentHandleServlet extends MCRServlet
{
  private static String SLASH = System.getProperty( "file.separator" );
  static final private Logger LOGGER = Logger.getLogger( MCRContentHandleServlet.class );
  
  public void doGetPost( MCRServletJob job )
    throws Exception
  {
    HttpServletRequest  req = job.getRequest();
    HttpServletResponse res = job.getResponse();
    String button = getProperty(req,"button");
    // show ifsnode?
    String ifsnodeid = getProperty(req, "ifsnodeid");
    if( ifsnodeid.length() > 0 )
    {
      LOGGER.info( "ifsnode to show : " + ifsnodeid );
      showIFSNode( req, res, ifsnodeid );
      return;
    }
    
    // editor input?
    MCREditorSubmission sub = (MCREditorSubmission)( req.getAttribute( "MCREditorSubmission" ) );
    if ( null != sub )
    {
      org.jdom.Document input = sub.getXML();
      if (null == input)
        LOGGER.info( "jdom from editor is null" );
      else
      {
        XMLOutputter outputter = new XMLOutputter();
        outputter.output( input, System.out );
        org.jdom.Element root = input.getRootElement();
        String rootName = root.getName();
        LOGGER.info( "MCRContentHandleServlet Root Name: " + rootName );
        if ( rootName.equals( "files") )
          doStore( req, res, sub, input );  
        else  
        if ( rootName.equals( "types") )
          doSearch( req, res, input );  
        return;
      }
    } // editor input end

  }
  
  /**
   * Forward file of ifs to layout servlet
   *
   * @param ifsnode node id of ifs
   **/
  private void showIFSNode( HttpServletRequest  req, 
                            HttpServletResponse res,
                            String ifsnodeid )
    throws Exception
  {
    MCRFile f = (MCRFile)MCRFilesystemNode.getNode( ifsnodeid );
    if ( null == f )
    {
      LOGGER.info( ifsnodeid + " not found." );
    }
    else
    {
      org.jdom.Document input = f.getContentAsJDOM();
      req.setAttribute( "MCRLayoutServlet.Input.JDOM", input );
      RequestDispatcher rd = getServletContext().getNamedDispatcher( "MCRLayoutServlet" );
      rd.forward( req, res );
      return;
    }
  }
  
  /**
   * Build query string for lucene from input
   *
   * @param input from editor servlet, contains query as jdom
   * @return the query for lucene
   **/
  private String buildLuceneQuery( org.jdom.Document input )
    throws Exception
  {  
     String query = "";
//     XMLOutputter outputter = new XMLOutputter();
//     outputter.output( input, System.out );
     org.jdom.Element root             = input.getRootElement();
//     String roottag = root.getAttributeValue( "roottag" );
//     System.out.println("ROOTTAG: " + roottag);
     List types                        = root.getChildren( "type" );
     for( int i = 0; i < types.size(); i++ )
     {
       org.jdom.Element xType = (org.jdom.Element)( types.get( i ) );
       String value           = xType.getAttributeValue( "value" );
       String name            = xType.getAttributeValue( "name" );
       if ( null != value && null != name )
       {
         StringTokenizer s = new StringTokenizer( value, " " );
         try
         {
           while( s.hasMoreTokens() )
           {
             query = query +  "+" + name + ":" + s.nextToken() + " ";
           }
         }
         catch( NoSuchElementException e)
         {
           LOGGER.info( e );
         }
       }
     }// for
     return query;
  }

  /**
   * Search for Content, forward search result to layout servlet as jdom
   *
   * @param input from editor servlet, contains query as jdom
   **/
  // TODO: getIndexerFromFCT 
  private void doSearch( HttpServletRequest  req, 
                         HttpServletResponse res,
                         org.jdom.Document input )
    throws Exception
  {
     String query = buildLuceneQuery( input );
     String handler = "tablename" ;
     MCRContentIndexer indexer = MCRContentIndexerFactory.getIndexerFromFCT( handler );
     String result[];
     if ( null != indexer )
     {  
       try
       { 
         LOGGER.info("QUERY: " + query);
         result = indexer.doSearchIndex( query  );
         org.jdom.Element res2 = new org.jdom.Element( "res" );
         input = new org.jdom.Document( res2 );
         for (int i=0;i<result.length;i++)
         {
           MCRFile f = (MCRFile)MCRFilesystemNode.getNode( result[i] );
           if ( null == f )
           {
             LOGGER.info( result[i] + " not found." );
           }
           else
           {
             org.jdom.Element help = f.getContentAsJDOM().detachRootElement();
             help.setAttribute( "ifsnodeid", result[i] );
             res2.addContent( help );
           }
         }
       }
       catch ( Exception ex ){}
     }
     else
       LOGGER.info("Handler not found for: " + handler );
        
     req.setAttribute( "MCRLayoutServlet.Input.JDOM", input );
     RequestDispatcher rd = getServletContext().getNamedDispatcher( "MCRLayoutServlet" );
     rd.forward( req, res );
  }
  
  /**
   * Upload files and store as content
   *
   * @param input from editor servlet, contains filename ... as jdom
   **/
  private void doStore( HttpServletRequest  req, 
                         HttpServletResponse res,
                         MCREditorSubmission sub,
                         org.jdom.Document input )
    throws Exception
  {
    org.jdom.Element root             = input.getRootElement();
    String update                     = root.getAttributeValue( "update" );
    List files                        = sub.getFiles();
    for( int j = 0; j < files.size(); j++ )
    {
      FileItem item = (FileItem)( files.get( j ) );
      String fpath = item.getName().trim();
      LOGGER.info( "Path: " + fpath );
      processFromFile( fpath, update.equals( "true" ) );
    }
  }
  
 /**
  * Loads or updates Content from an XML file.
  *
  * @param filename the location of the xml file
  * @param update if true, object will be updated, else object is created
  **/
  private static boolean processFromFile( String file, boolean update )
    {
    if( ! file.endsWith( ".xml" ) ) {
      LOGGER.warn( file + " ignored, does not end with *.xml" );
      return false;
      }
    if( ! new File( file ).isFile() ) {
      LOGGER.warn( file + " ignored, is not a file." );
      return false;
      }
    
    String ID = file;
    int i = file.lastIndexOf( SLASH );
    if ( -1 != i )
        ID = file.substring( i+1 );
    ID = ID.substring(0,ID.length()-4);
    System.out.println("++ID: " + ID );    
    String message;
    MCRDirectory difs = MCRDirectory.getRootDirectory( ID );
    if ( update ) 
    {
      if ( null == difs )
      {
        LOGGER.warn( "Content with " + ID + " not found in IFS." );
        return false;
      }
      difs.delete();
      message = " updated in iFS.";
    }
    else 
    {
      if ( null != difs )
      {
        LOGGER.warn( "Content with " + ID + " allready stored in IFS." );
        return false;
      }
      message = " stored in IFS.";
    }
    LOGGER.info( "Reading file " + file + " ..." );
    File f = new File( file );
    MCRFileImportExport.importFiles(f, ID );
    LOGGER.info( ID + message );
    LOGGER.info("");
    
    return true;
    }
}
