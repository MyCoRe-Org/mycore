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

package org.mycore.backend.realhelix;

import org.mycore.common.*;
import org.mycore.datamodel.ifs.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;

/**
 * This class implements the AudioVideoExtender functions for Real Server 8 and
 * Helix Universal Streaming Server 9 instances. It reads technical metadata about 
 * stored assets by parsing the Real Server's "View Source" responses and gets a 
 * player starter file using the "/ramgen/"  mount point. The parameters can be 
 * configured in mycore.properties:
 *
 * <code>
 *   MCR.IFS.AVExtender.<StoreID>.RamGenBaseURL      URL of ramgen mount point
 *   MCR.IFS.AVExtender.<StoreID>.AsxGenBaseURL      URL of asxgen mount point
 *   MCR.IFS.AVExtender.<StoreID>.ViewSourceBaseURL  URL of view source function
 *   MCR.IFS.AVExtender.<StoreID>.RealPlayerURL      Download URL for RealOne Player
 *   MCR.IFS.AVExtender.<StoreID>.MediaPlayerURL     Download URL for Microsoft Player
 * </code>
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */ 
public class MCRAVExtRealHelix extends MCRAudioVideoExtender
{ 
  public MCRAVExtRealHelix()
  {}

  public void init( MCRFileReader file )
    throws MCRPersistenceException
  {
    super.init( file );
    
    MCRConfiguration config = MCRConfiguration.instance();  
    String prefix = "MCR.IFS.AVExtender." + file.getStoreID() + ".";
      
    baseMetadata = config.getString( prefix + "ViewSourceBaseURL" );
    
    String data = getMetadata( baseMetadata + file.getStorageID() );
    
    try
    {
      String sSize      = getBetween( "File Size:</strong>",   "Bytes", data, "0"     );
      String sBitRate   = getBetween( "Bit Rate:</strong>",    "Kbps",  data, "0.0"   );
      String sFrameRate = getBetween( "Frame Rate: </strong>", "fps",   data, "0.0"   );
      String sDuration  = getBetween( "Duration:</strong>",    "<br>",  data, "0:0.0" );
      String sType      = getBetween( "Stream:</strong>",      "<br>",  data, ""      );

      bitRate = Math.round( 1024 * Float.valueOf( sBitRate ).floatValue() );

      StringTokenizer st1 = new StringTokenizer( sFrameRate, " ," );
      while( st1.hasMoreTokens() )
      { 
        double value = Double.valueOf( st1.nextToken() ).doubleValue();
        frameRate = Math.max( frameRate, value );
      }
      mediaType = ( frameRate > 0 );
      
      StringTokenizer st2 = new StringTokenizer( sDuration, ":." );
      durationMinutes = Integer.parseInt( st2.nextToken() );
      durationSeconds = Integer.parseInt( st2.nextToken() );
      
      if( Integer.parseInt( st2.nextToken() ) > 499 ) 
      {
        durationSeconds += 1;
        if( durationSeconds > 59 ) 
        {
          durationMinutes += 1;
          durationSeconds =  0;
        }
      }
      
      StringTokenizer st3 = new StringTokenizer( sSize, "," );
      StringBuffer sb = new StringBuffer();
      while( st3.hasMoreTokens() ) sb.append( st3.nextToken() );
      size = Long.parseLong( sb.toString() );

      durationHours   = ( durationMinutes / 60 );
      durationMinutes = durationMinutes - ( durationHours * 60 );
      
      if( sType.indexOf( "MPEG Layer 3" ) >= 0 )
        contentTypeID = "mp3";
      else if( sType.indexOf( "MPEG" ) >= 0 )
        contentTypeID = "mpeg1";
      else if( sType.indexOf( "RealVideo" ) >= 0 )
        contentTypeID = "realvid";
      else if( sType.indexOf( "RealAudio" ) >= 0 )
        contentTypeID = "realaud";
      else if( sType.indexOf( "Wave File" ) >= 0 )
        contentTypeID = "wav";
      else
        contentTypeID = file.getContentType().getID();
      
      if( " wma wmv asf asx ".indexOf( " " + contentTypeID + " " ) != -1 )
      {
        basePlayerStarter = config.getString( prefix + "AsxGenBaseURL"  );
        playerDownloadURL = config.getString( prefix + "MediaPlayerURL" ); 
      }
      else
      {
        basePlayerStarter = config.getString( prefix + "RamGenBaseURL" );
        playerDownloadURL = config.getString( prefix + "RealPlayerURL" ); 
      }
      
      URLConnection con = getConnection( basePlayerStarter + file.getStorageID() );
      playerStarterCT = con.getContentType();
    }
    catch( Exception exc )
    { 
      String msg = "Error parsing metadata from Real Server ViewSource: " + file.getStorageID();  
      throw new MCRPersistenceException( msg, exc );
    }
  }
  
  public void getPlayerStarterTo( OutputStream out, String startPos, String stopPos )
    throws MCRPersistenceException
  {
    try
    {
      StringBuffer cgi = new StringBuffer( basePlayerStarter ); 
      cgi.append( file.getStorageID() );
      
      if( ( startPos != null ) || ( stopPos != null ) ) cgi.append( "?" );
      if( startPos != null ) cgi.append( "start=" ).append( startPos );
      if( ( startPos != null ) && ( stopPos != null ) ) cgi.append( "&" );
      if( stopPos != null ) cgi.append( "stop=" ).append( stopPos );
    
      URLConnection connection = getConnection( cgi.toString() );
      forwardData( connection, out );
    }
    catch( IOException exc )
    {
      String msg = "Could not send player starter file";
      throw new MCRPersistenceException( msg, exc ); 
    }
  }
}
