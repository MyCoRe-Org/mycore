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
import java.text.*;

/**
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public abstract class MCRContentStoreBase
{ 
  /** DateFormat used to construct new unique IDs based on timecode */
  protected DateFormat formatter;

  /** The last ID that was constructed */
  protected String lastID;
  
  /** The unique store ID for this MCRContentStore implementation */
  protected String storeID;
  
  protected String prefix;

  public MCRContentStoreBase()
  {
    formatter = new SimpleDateFormat( "yyyy-MM-dd_HHmmss_SSS" );
    lastID    = null;
  }
  
  public void init( String storeID )
  { 
    this.storeID = storeID; 
    this.prefix = "MCR.IFS.ContentStore." + storeID + ".";
  }

  public String getID()
  { return storeID; }

  /**
   * Constructs a new unique ID based on timecode for storing content
   */
  protected synchronized String buildNextID()
  {
    String ID = null;
    do{ ID = formatter.format( new Date() ); }
    while( ID.equals( lastID ) );
    return ( lastID = ID );
  }
  
  protected String[] buildSlotPath()
  {
    Random random = new Random();
    int na = random.nextInt( 100 );
    int nb = random.nextInt( 100 );
    String sa = String.valueOf( na );
    String sb = String.valueOf( nb );
    if( na < 10 ) sa = "0" + sa;
    if( nb < 10 ) sb = "0" + sb;
    String[] slots = new String[ 2 ];
    slots[ 0 ] = sa;
    slots[ 1 ] = sb;
    return slots;
  }
}

