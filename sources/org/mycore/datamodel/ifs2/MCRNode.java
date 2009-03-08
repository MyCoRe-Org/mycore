package org.mycore.datamodel.ifs2;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.VFS;

/**
 * Represents a file, directory or file collection within a file store.
 * Files and directories can be either really stored, or virtually existing
 * as a child node contained within a stored container file like zip or tar.
 * 
 * @author Frank Lützenkirchen
 */
public abstract class MCRNode
{
  /**
   * The file object representing this node in the underlying filesystem.
   */
  protected FileObject fo;
  
  /**
   * The parent node owning this file, a directory or container file
   */
  protected MCRNode parent;
  
  /**
   * Creates a new node representing a child of the given parent
   *  
   * @param parent the parent node
   * @param fo the file object representing this node in the underlying filesystem
   */
  protected MCRNode( MCRNode parent, FileObject fo )
  {
    this.fo = fo;
    this.parent = parent;
  }
  
  /**
   * Returns the file or directory name
   * 
   * @return the node's filename
   */
  public String getName()
  { return fo.getName().getBaseName(); }
  
  /**
   * Returns the complete path of this node up to the root file collection.
   * Path always start with a slash, slash is used as directory delimiter.
   * 
   * @return the absolute path of this node
   * @throws Exception
   */
  public String getPath() throws Exception
  { return parent.getPath() + "/" + getName(); }
  
  /**
   * Returns the parent node containing this node
   * 
   * @return the parent directory or container file
   */
  public MCRNode getParent()
  { return parent; }
  
  /**
   * Returns the root file collection this node belongs to
   * 
   * @return the root file collection
   */
  public MCRFileCollection getRoot()
  { return parent.getRoot(); }
  
  /**
   * Returns true if this node is a file
   * 
   * @return true if this node is a file
   */
  public boolean isFile() throws Exception
  { return fo.getType().equals( FileType.FILE ); }

  /**
   * Returns true if this node is a directory
   * 
   * @return true if this node is a directory
   */
  public boolean isDirectory() throws Exception
  { return fo.getType().equals( FileType.FOLDER ); }

  /**
   * For file nodes, returns the file content size in bytes,
   * otherwise returns 0.
   * 
   * @return the file size in bytes
   */
  public long getSize() throws Exception
  {
    if( isFile() )
      return fo.getContent().getSize();
    else
      return 0;
  }

  /**
   * Returns the time this node was last modified, or null if no such time
   * is defined in the underlying filesystem
   * 
   * @return the time this node was last modified
   */
  public Date getLastModified() throws Exception
  {
    FileContent content = fo.getContent();
    if( content != null )
      return new Date( content.getLastModifiedTime() );
    else 
      return null;
  }
  
  /**
   * Returns true if this node has child nodes. Directories and container 
   * files like zip or tar may have child nodes.
   * 
   * @return true if children exist
   */
  public boolean hasChildren() throws Exception
  { return getNumChildren() > 0; }

  /**
   * Returns the FileObject that is the father of all logical 
   * children of this FileObject. This may not be the current node itself,
   * in case the node is a container file, because then intermediate 
   * FileObject instances are created by Apache VFS.
   * 
   * @return the father of this node's children in VFS
   */
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

  /**
   * Returns the number of child nodes of this node.
   * 
   * @return the number of child nodes of this node.
   */
  public int getNumChildren() throws Exception
  {
    FileObject father = getFather();
    if( father == null ) return 0;
    
    if( father.getChild( MCRDirectory.metadataFile ) != null )
      return father.getChildren().length - 1;
    else 
      return father.getChildren().length;
  }

  /**
   * Returns the children of this node. Directories and container
   * files like zip or tar may have child nodes.
   * 
   * @return a List of child nodes, which may be empty, in undefined order
   */
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
  
  /**
   * Creates a node instance for the given FileObject, which represents the child
   *
   * @param fo the FileObject representing the child in the underlying filesystem 
   * @return the child node
   */
  protected abstract MCRNode buildChildNode( FileObject fo ) throws Exception;
  
  /**
   * Returns the child node with the given filename, or null
   * 
   * @param name the name of the child node
   * @return the child node with that name, or null when no such file exists
   */
  public MCRNode getChild( String name ) throws Exception
  { 
    FileObject father = getFather();
    return ( father == null ? null : buildChildNode( getFather().getChild( name ) ) ); 
  }

  /**
   * Returns the node with the given relative or absolute path in the file collection this node
   * belongs to. Slash is used as directory delimiter. When the path starts with a slash, it is 
   * an absolute path and resolving is startet at the root file collection. When the path is
   * relative, resolving starts with the current node. One dot represents the current node,
   * Two dots represent the parent node, like in paths used by typical real filesystems.
   * 
   * @param path the absolute or relative path of the node to find, may contain . or ..
   * @return the node at the given path, or null 
   */
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
