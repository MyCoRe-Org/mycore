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
import javax.servlet.http.*;

/**
 * Represents a directory node with its metadata and content.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRDirectory extends MCRFilesystemNode
{
  private Vector childrenIDs;
  
  static MCRDirectory getDirectory( String ID )
  { return (MCRDirectory)( MCRFilesystemNode.getNode( ID ) ); }
  
  public MCRDirectory( String name, String ownerID )
  { 
    super( name, ownerID );
    storeNew();
  }
  
  public MCRDirectory( String name, MCRDirectory parent )
  { 
    super( name, parent );
    storeNew();
  }
  
  MCRDirectory( String ID, String parentID, String ownerID, String name, long size, GregorianCalendar date )
  { super( ID, parentID, ownerID, name, size, date ); }
  
  protected void addChild( MCRFilesystemNode child )
  {
    if( childrenIDs != null )
      childrenIDs.addElement( child.getID() );
    
    touch(); 
  }
  
  protected void removeChild( MCRFilesystemNode child )
  { 
    if( childrenIDs != null )
      childrenIDs.removeElement( child.getID() );
    
    sizeOfChildChanged( child.size, 0 ); 
  }
  
  public MCRFilesystemNode[] getChildren()
  {
    ensureNotDeleted();
    
    if( childrenIDs == null )
      childrenIDs = manager.retrieveChildrenIDs( ID );
    
    MCRFilesystemNode[] children = new MCRFilesystemNode[ childrenIDs.size() ];
    for( int i = 0; i < childrenIDs.size(); i++ )
    {
      String childID = (String)( childrenIDs.get( i ) );
      children[ i ] = manager.retrieveNode( childID );
    }
    
    return children;
  }
  
  public MCRFilesystemNode[] getChildren( Comparator sortOrder )
  {
    ensureNotDeleted();

    MCRArgumentChecker.ensureNotNull( sortOrder, "sort order" );
    
    MCRFilesystemNode[] array = getChildren();
    Arrays.sort( array, sortOrder );
    return array;
  }
  
  public boolean hasChildren()
  {
    ensureNotDeleted();
    return ( getNumChildren() > 0 ); 
  }
  
  public boolean hasChild( String name )
  {
    ensureNotDeleted();
    MCRFilesystemNode child = getChild( name );
    return ( child != null );
  }
  
  public MCRFilesystemNode getChild( int index )
  {
    ensureNotDeleted();
    
    if( childrenIDs == null )
      return getChildren()[ index ];
    else
      return manager.retrieveNode( (String)( childrenIDs.get( index ) ) );  
  }
  
  public MCRFilesystemNode getChild( String name )
  {
    ensureNotDeleted();
    
    if( name.equals( "." ) ) 
      return this;
    else if( name.equals( ".." ) )
      return( hasParent() ? getParent() : null );
    else
      return manager.retrieveChild( ID, name );
  }
  
  public MCRFilesystemNode getChildByPath( String path )
  {
    ensureNotDeleted();
    
    MCRDirectory base = this;
    
    if( path.startsWith( "/" ) )
    {  
      base = getRootDirectory();
      if( path.equals( "/" ) ) 
        return base;
      else
        path = path.substring( 1 );
    }
    
    int index = path.indexOf( "/" );
    int end   = ( index == -1 ? path.length() : index );
    String name = path.substring( 0, end );
    
    MCRFilesystemNode child = getChild( name );
    if( child == null ) return null; // Not found
    if( path.indexOf( "/", index ) == -1 ) return child; // Found
    if( ! ( child instanceof MCRDirectory ) ) return null; // Not a directory
    
    MCRDirectory dir = (MCRDirectory)child;
    return dir.getChildByPath( path.substring( end + 1 ) ); // Look in child dir
  }
  
  public int getNumChildren()
  {
    ensureNotDeleted();
    
    if( childrenIDs != null )
      return childrenIDs.size();
    else
      return manager.retrieveNumberOfChildren( ID );
  }

  protected void sizeOfChildChanged( long oldSize, long newSize )
  {
    this.size -= oldSize;
    this.size += newSize;
    this.lastModified = new GregorianCalendar();
    
    manager.storeNode( this );
    
    if( hasParent() ) getParent().sizeOfChildChanged( oldSize, newSize );
  }
  
  protected void touch()
  {
    this.lastModified = new GregorianCalendar();
    
    manager.storeNode( this );
    
    if( hasParent() ) getParent().touch();
  }
  
  /**
   * Deletes this directory and its content stored in the system
   **/
  public void delete()
    throws MCRPersistenceException
  {
    ensureNotDeleted();

    for( int i = 0; i < getNumChildren(); i++ )
      getChild( i ).delete();
    
    super.delete();
    
    this.childrenIDs = null;
  }

  public final static Comparator SORT_BY_NAME_IGNORECASE = new Comparator()
  {
    public boolean equals( Object obj ){ return super.equals( obj ); }
    public int compare( Object a, Object b )
    { return ((MCRFilesystemNode)a).getName().compareToIgnoreCase( ( (MCRFilesystemNode)b).getName() ); }
  };
  
  public final static Comparator SORT_BY_NAME = new Comparator()
  {
    public boolean equals( Object obj ){ return super.equals( obj ); }
    public int compare( Object a, Object b )
    { return ((MCRFilesystemNode)a).getName().compareTo( ((MCRFilesystemNode)b).getName() ); }
  };
  
  public final static Comparator SORT_BY_SIZE = new Comparator()
  {
    public boolean equals( Object obj ){ return super.equals( obj ); }
    public int compare( Object a, Object b )
    { return (int)( ((MCRFilesystemNode)a).getSize() - ((MCRFilesystemNode)b).getSize() ); }
  };
  
  public final static Comparator SORT_BY_DATE = new Comparator()
  {
    public boolean equals( Object obj ){ return super.equals( obj ); }
    public int compare( Object a, Object b )
    { return ((MCRFilesystemNode)a).getLastModified().getTime().compareTo( ((MCRFilesystemNode)b).getLastModified().getTime() ); }
  };

  protected void collectMD5Lines( List list )
  {
    MCRFilesystemNode[] nodes = getChildren();
    for( int i = 0; i < nodes.length; i++ )
    {
      if( nodes[ i ] instanceof MCRDirectory )
      {
        MCRDirectory dir = (MCRDirectory)( nodes[ i ] );
        dir.collectMD5Lines( list );
      }
      else
      {
        MCRFile file = (MCRFile)( nodes[ i ] ); 
        String line = file.getMD5() + " " + file.getSize();
        list.add( line );
      }  
    }
  }
  
  public byte[] buildFingerprint()
  {
    ensureNotDeleted();
    
    List list = new Vector();
    collectMD5Lines( list );
    Collections.sort( list );
    
    StringBuffer sb = new StringBuffer();
    for( int i = 0; i < list.size(); i++ )
      sb.append( list.get( i ) ).append( '\n' );
    String s = sb.toString();
    
    try{ return s.getBytes( "UTF-8" ); }
    catch( UnsupportedEncodingException shouldNeverBeThrown ){ return null; }
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append( super.toString() );
    sb.append( "NumChildren = " ).append( this.getNumChildren() );
    return sb.toString();
  }
}

