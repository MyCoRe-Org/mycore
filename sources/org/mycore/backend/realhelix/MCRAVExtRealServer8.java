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

package mycore.ifs;

import mycore.common.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;

/**
 * This class implements the AudioVideoExtender functions for Real Server 8
 * instances. It reads technical metadata about stored assets by parsing
 * the Real Server's "View Source" responses and gets a player starter file
 * using the "/ramgen/"  mount point. The parameters can be configured 
 * in mycore.properties:
 *
 * <code>
     MCR.IFS.AVExtender.<StoreID>.RamGenBaseURL      URL of ramgen mount point
     MCR.IFS.AVExtender.<StoreID>.ViewSourceBaseURL  URL of view source function
     MCR.IFS.AVExtender.<StoreID>.PlayerURL          Download URL for RealPlayer
 * </code>
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */ 
public class MCRAVExtRealServer8 extends MCRAudioVideoExtender
{ 
  public MCRAVExtRealServer8()
  {}

  public void init( MCRFile file )
    throws MCRPersistenceException
  {
    this.file = file;
    
    MCRConfiguration config = MCRConfiguration.instance();  
    String prefix = "MCR.IFS.AVExtender." + file.getStoreID() + ".";
      
    basePlayerStarter = config.getString( prefix + "RamGenBaseURL"     );
    baseMetadata      = config.getString( prefix + "ViewSourceBaseURL" );
    playerDownloadURL = config.getString( prefix + "PlayerURL"         ); 
    
    String data = getMetadata( baseMetadata + file.getStorageID() );
    
    URLConnection con = getConnection( basePlayerStarter + file.getStorageID() );
    playerStarterCT = con.getContentType();

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
      else if( sType.indexOf( "RealVideo" ) >= 0 )
        contentTypeID = "real";
      else if( sType.indexOf( "RealAudio" ) >= 0 )
        contentTypeID = "real";
    }
    catch( Exception exc )
    { 
      String msg = "Error parsing metadata from RealServer ViewSource: " 
                   + file.getStorageID();  
      throw new MCRPersistenceException( msg, exc );
    }
  }
  
  public void getPlayerStarterTo( OutputStream out, String queryString )
    throws MCRPersistenceException
  {
    try
    {
      StringBuffer cgi = new StringBuffer( basePlayerStarter ); 
      cgi.append( file.getStorageID() );
      if( queryString != null ) cgi.append( "?" ).append( queryString );
    
      URLConnection connection = getConnection( cgi.toString() );
      forwardData( connection, out );
    }
    catch( IOException exc )
    {
      String msg = "Could not send RealPlayer starter .ram file";
      throw new MCRPersistenceException( msg, exc ); 
    }
  }
}
