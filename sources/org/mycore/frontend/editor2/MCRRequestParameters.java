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

package org.mycore.frontend.editor2;

import org.mycore.common.*;

import org.apache.log4j.*;
import org.apache.commons.fileupload.*;

import java.net.*;
import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Wrapper class around an HTTP request that allows to
 * treat both ordinary form submisstion and multipart/form-data 
 * submissions with uploaded files in the same way.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 **/
public class MCRRequestParameters
{
  protected final static Logger logger = Logger.getLogger(  MCREditorServlet.class );

  private Properties parameters = new Properties();
  private Hashtable  files      = new Hashtable();

  private static int    threshold;
  private static long   maxSize;  
  private static String tmpPath;

  static
  {
    MCRConfiguration config = MCRConfiguration.instance();
    String prefix = "MCR.Editor.";

    threshold = config.getInt   ( prefix + "FileUpload.MemoryThreshold", 1000000 );
    maxSize   = config.getLong  ( prefix + "FileUpload.MaxSize", 5000000 );
    tmpPath   = config.getString( prefix + "FileUpload.TempStoragePath" );
  }
  
  public MCRRequestParameters( HttpServletRequest req )
  {
    if( FileUpload.isMultipartContent( req ) )
    {
      DiskFileUpload parser = new DiskFileUpload();
      parser.setSizeThreshold( threshold );
      parser.setSizeMax( maxSize );
      parser.setRepositoryPath( tmpPath );

      List items = null;
      try{ items = parser.parseRequest( req ); }
      catch( FileUploadException ex )
      {
        String msg = "Error while parsing http multipart/form-data request from file upload webpage";
        throw new MCRException( msg, ex );
      }
 
      for( int i = 0; i < items.size(); i++ )
      {
        FileItem item = (FileItem)( items.get( i ) );

        String name = item.getFieldName();
        String value;
        
        if( item.isFormField() )
          value = item.getString();
        else
          value = item.getName();

        if( ( value != null ) && ( value.trim().length() > 0 ) && ( ! files.containsKey( name ) ) )
        {
          if( ! item.isFormField() ) files.put( name, item );
          parameters.put( name, value );
        }
      }
    }
    else
    {
      for( Enumeration e = req.getParameterNames(); e.hasMoreElements(); )
      {
        String name = (String)( e.nextElement() );
        String value  = req.getParameter( name );        
        if( value != null ) parameters.put( name, value );
      }
    }
  }

  public Enumeration getParameterNames()
  { return parameters.keys(); }

  public String getParameter( String name )
  { return parameters.getProperty( name ); }
    
  public FileItem getFileItem( String name )
  { return (FileItem)( files.get( name ) ); }
}

