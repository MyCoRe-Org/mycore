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
  protected Vector children = new Vector();
  
  public MCRDirectory( String name, String ownerID )
  { 
    super( name, ownerID ); 
    // create
  }
  
  public MCRDirectory( String name, MCRDirectory parent )
  { 
    super( name, parent ); 
    // create
  }
  
  protected void addChild( MCRFilesystemNode child )
  {
    // retrieve children
    children.addElement( child ); 
    touch();
    // update
  }
  
  protected void removeChild( MCRFilesystemNode child )
  {
    // retrieve children
    children.removeElement( child ); 
    sizeOfChildChanged( child.size, 0 );
  }
  
  public MCRFilesystemNode[] getChildren()
  {
    ensureNotDeleted();
    // retrieve children
    MCRFilesystemNode[] array = new MCRFilesystemNode[ getNumChildren() ];
    children.copyInto( array );
    return array;
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
    // retrieve children
    return ( children.size() > 0 ); 
  }
  
  public boolean hasChild( String name )
  {
    ensureNotDeleted();
    MCRFilesystemNode child = getChild( name );
    return ( child == null );
  }
  
  public MCRFilesystemNode getChild( int index )
  {
    ensureNotDeleted();
    // retrieve children
    return (MCRFilesystemNode)( children.get( index ) ); 
  }
  
  public MCRFilesystemNode getChild( String name )
  {
    ensureNotDeleted();
    // retrieve children
    for( int i = 0; i < getNumChildren(); i++ )
    {
      MCRFilesystemNode child = getChild( i );
      if( child.getName().equals( name ) ) return child;
    }
    return null;
  }
  
  public MCRFilesystemNode getChildByPath( String path )
  {
    ensureNotDeleted();
    // retrieve children 
    MCRDirectory base = this;
    
    if( path.startsWith( "/" ) )
    {  
      base = getRootDirectory();
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
    // retrieve children
    return children.size(); 
  }

  protected void sizeOfChildChanged( long oldSize, long newSize )
  {
    this.size -= oldSize;
    this.size += newSize;
    this.lastModified = new GregorianCalendar();
    // update
    if( parent != null ) parent.sizeOfChildChanged( oldSize, newSize );
  }
  
  protected void touch()
  {
    this.lastModified = new GregorianCalendar();
    // update
    if( parent != null ) parent.touch();
  }
  
  /**
   * Deletes this directory and its content stored in the system
   **/
  public void delete()
    throws MCRPersistenceException
  {
    ensureNotDeleted();

    MCRFilesystemNode[] array = getChildren();
    for( int i = 0; i < array.length; i++ )
      array[ i ].delete();
    
    super.delete();
    
    this.children = null;
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
}
