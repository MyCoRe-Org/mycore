/**
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
 *
 **/

package mycore.classifications;

import mycore.common.*;

public abstract class MCRClassificationObject
{
  protected String   ID;
  protected String   label;
  protected String[] childrenIDs;
  
  protected boolean deleted = false;
  
  protected static MCRClassificationManager manager()
  { return MCRClassificationManager.instance(); }
  
  protected MCRClassificationObject( String ID, String label ) 
  {
    MCRArgumentChecker.ensureNotEmpty( ID,    "ID"    );
    MCRArgumentChecker.ensureNotEmpty( label, "label" );
    this.ID    = ID;
    this.label = label;
  }
  
  protected void ensureNotDeleted()
  {
    if( this.deleted ) throw new MCRUsageException
    ( "This classification object is invalid because it has been deleted" );
  }

  public String getID()
  { 
    ensureNotDeleted();
    return ID; 
  }
  
  protected abstract String getClassificationID();
  
  public String getLabel()
  { 
    ensureNotDeleted();
    return label; 
  }
  
  public void setLabel( String label )
  {
    ensureNotDeleted();
    MCRArgumentChecker.ensureNotEmpty( label, "label" );  
    this.label = label;
  }
  
  public boolean hasChildren()
  { 
    ensureNotDeleted();
    return( getNumChildren() > 0 ); 
  }
  
  public int getNumChildren()
  {
    ensureNotDeleted();
    if( childrenIDs != null )
      return childrenIDs.length;
    else
      return manager().retrieveNumberOfChildren( getClassificationID(), ID );
  }
  
  public MCRCategory[] getChildren()
  {
    ensureNotDeleted();
    
    MCRCategory[] children;
    if( childrenIDs == null )
    {
      String parentID = ( this instanceof MCRCategory ? ID : null );
      children = manager().retrieveChildren( getClassificationID(), parentID );
      childrenIDs = new String[ children.length ];
      for( int i = 0; i < children.length; i++ )
        childrenIDs[ i ] = children[ i ].getID();
    }
    else
    {
      children = new MCRCategory[ childrenIDs.length ];
      for( int i = 0; i < children.length; i++ )
        children[ i ] = manager().retrieveCategory( getClassificationID(), childrenIDs[ i ] );
    }
    return children;
  }
  
  public void delete()
  {
    ensureNotDeleted();
    
    MCRCategory[] children = getChildren();
    for( int i = 0; i < children.length; i++ )
      children[ i ].delete();
    
    deleted = true;
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer( getClass().getName() ).append( "\n" );
    sb.append( "ID:             " ).append( ID    ).append( "\n" );
    sb.append( "Label:          " ).append( label );
    return sb.toString();
  }
}
