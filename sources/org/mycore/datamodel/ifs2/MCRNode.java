package org.mycore.datamodel.ifs2;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.VFS;

public abstract class MCRNode
{
  protected FileObject fo;
  protected MCRNode parent;
  
  protected MCRNode( MCRNode parent, FileObject fo )
  {
    this.fo = fo;
    this.parent = parent;
  }
  
  public String getName()
  { return fo.getName().getBaseName(); }
  
  public String getPath() throws Exception
  { return parent.getPath() + "/" + getName(); }
  
  public MCRNode getParent()
  { return parent; }
  
  public MCRNode getRoot()
  { return parent.getRoot(); }
  
  public boolean isFile() throws Exception
  { return fo.getType().equals( FileType.FILE ); }
  
  public boolean isDirectory() throws Exception
  { return fo.getType().equals( FileType.FOLDER ); }
  
  public long getSize() throws Exception
  {
    if( isFile() )
      return fo.getContent().getSize();
    else
      return 0;
  }

  public long getLastModified() throws Exception
  {
    if( isFile() )
      return fo.getContent().getLastModifiedTime();
    else
      return 0;
  }
  
  public boolean hasChildren() throws Exception
  { return getNumChildren() > 0; }

  private FileObject getFather() throws Exception
  {
    if( isDirectory() ) 
      return fo;
    else if( getSize() == 0 )
      return null;
    else if( VFS.getManager().canCreateFileSystem( fo ) )
    {
      FileObject father = fo;
      while( VFS.getManager().canCreateFileSystem( father ) )
        father = VFS.getManager().createFileSystem( father );
      return father;
    }
    else return null;
  }
  
  public int getNumChildren() throws Exception
  {
    FileObject father = getFather();
    if( father == null ) return 0;
    
    if( father.getChild( MCRDirectory.metadataFile ) != null )
      return father.getChildren().length - 1;
    else 
      return father.getChildren().length;
  }

  public List<MCRNode> getChildren() throws Exception
  {
    List<MCRNode> children = new ArrayList<MCRNode>();
    FileObject father = getFather();
    if( father != null )
    {
      FileObject[] childFos = father.getChildren();
      for( int i = 0; i < childFos.length; i++ )
      {
        FileObject childFO = childFos[ i ];
        if( ! childFO.getName().getBaseName().equals( MCRDirectory.metadataFile ) )
          children.add( buildChildNode( childFO ) );
      }
    }
    return children;
  }
  
  protected abstract MCRNode buildChildNode( FileObject fo ) throws Exception;
  
  public MCRNode getChild( String name ) throws Exception
  { 
    FileObject father = getFather();
    return ( father == null ? null : buildChildNode( getFather().getChild( name ) ) ); 
  }
  
  public MCRNode getNodeByPath( String path ) throws Exception
  {
    MCRNode current = path.startsWith( "/" ) ? getRoot() : this;
    StringTokenizer st = new StringTokenizer( path, "/" );
    while( ( current != null ) && st.hasMoreTokens() )
    {
      String name = st.nextToken();
      if( name.equals( "." ) ) 
        continue;
      else if( name.equals( ".." ) )
        current = parent;
      else
        current = getChild( name );
    }
    return current;
  }
}
