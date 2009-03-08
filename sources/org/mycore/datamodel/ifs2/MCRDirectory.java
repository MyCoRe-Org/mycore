package org.mycore.datamodel.ifs2;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.VFS;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Represents a directory stored in a file collection, which may contain 
 * other files and directories.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRDirectory extends MCRStoredNode
{
  /**
   * The name of the XML file that stores additional data about
   * the files contained in this directory, like md5 checksum and other.
   */
  protected static String metadataFile = ".mcr-metadata.xml";
  
  /**
   * The metadata of all child nodes (files and directories) contained in
   * this directory, like md5 checksum and other.
   * 
   * @see #metadataFile
   */
  protected Element metadata;
  
  /**
   * Create MCRDirectory representing an existing, already stored directory. 
   * 
   * @param parent the parent directory of this directory
   * @param fo the local directory in the store storing this directory
   */
  protected MCRDirectory( MCRDirectory parent, FileObject fo ) throws Exception
  { 
    super( parent, fo );
    if( parent != null ) parent.readChildData( this );
    readMetadata();
  }

  /**
   * Creates a new MCRDirectory in the store.
   * 
   * @param parent the parent directory of this directory
   * @param name the name of the new directory
   */
  public MCRDirectory( MCRDirectory parent, String name ) throws Exception
  { 
    super( parent, VFS.getManager().resolveFile( parent.fo, name ) );
    fo.createFolder();
    metadata = new Document( new Element( "metadata" ) ).getRootElement();
    writeMetadata();
  }

  /**
   * Deletes this directory in the store, including all children.
   */
  public void delete() throws Exception
  { 
    super.delete();
    fo.delete( Selectors.SELECT_ALL );
  }
  
  /**
   * Returns the MCRFile or MCRDirectory that is represented by the
   * given FileObject, which is a direct child of the directory FileObject this
   * MCRDirectory is stored in.
   * 
   * @return an MCRFile or MCRDirectory child
   */
  protected MCRStoredNode buildChildNode( FileObject fo ) throws Exception
  {
    if( fo.getType().equals( FileType.FILE ) )
      return new MCRFile( this, fo );
    else
      return new MCRDirectory( this, fo );
  }

  /**
   * Reads the internal metadata XML file stored in the local directory, which
   * contains additional data about all child nodes. 
   */
  protected void readMetadata() throws Exception
  {
    FileObject md = VFS.getManager().resolveFile( fo, metadataFile );
    if( md.exists() )
    {
      InputStream in = md.getContent().getInputStream();
      metadata = new SAXBuilder().build( in ).getRootElement();
      in.close();
    }
    else metadata = new Document( new Element( "metadata" ) ).getRootElement();
  }

  /**
   * Saves internal metadata about all child nodes in an XML file in the local 
   * directory.
   */
  protected void writeMetadata() throws Exception
  {
    FileObject md = VFS.getManager().resolveFile( fo, metadataFile );
    if( ! md.exists() ) md.createFile();
    OutputStream out = md.getContent().getOutputStream();
    XMLOutputter xout = new XMLOutputter();
    xout.setFormat( Format.getPrettyFormat().setEncoding( "UTF-8" ).setIndent( "  " ) );
    xout.output( metadata.getDocument(), out );
    out.close();

    updateMetadata();
  }

  /**
   * Updates the metadata of the given child node in the internal XML file.
   * 
   * @param name the file name of the child node
   * @param child the child node thats metadata has to be updated
   */
  protected void updateMetadata( String name, MCRStoredNode child ) throws Exception
  {
    Element entry = findEntry( name );
    if( entry == null )
    {
      entry = new Element( child.isDirectory() ? "dir" : "file" );
      metadata.addContent( entry );
    }
    child.writeChildData( entry );
    writeMetadata();
  }

  /**
   * Removes the metadata of a child node from the internal metadata XML structure.
   * This method is called when a chlid node is deleted.
   * 
   * @param child the child node thats metadata has to be removed.
   */
  protected void removeMetadata( MCRStoredNode child ) throws Exception
  {
    findEntry( child.getName() ).detach();
    writeMetadata();
  }

  /**
   * Finds the metadata entry of the given child node in the metadata XML structure.
   * 
   * @param name the file name of the child node
   * @return the XML element containing metadata of the given child node
   */
  protected Element findEntry( String name ) throws Exception
  {
    for( Element entry : (List<Element>)( metadata.getChildren() ) )
      if( entry.getAttributeValue( "name" ).equals( name ) ) return entry;
    
    return null;
  }

  /**
   * Notifies the child node to read its additional metadata from the XML structure
   * stored in this directory, which is its parent.
   * 
   * @param child the child node that should read its metadata 
   */
  protected void readChildData( MCRStoredNode child ) throws Exception
  {
    Element entry = findEntry( child.getName() );
    if( entry != null ) child.readChildData( entry );
  }
  
  /**
   * Returns an XML structure containing metadata about all child nodes in this
   * directory, like name, type, size, last modified time and md5 checksum. The output
   * can be used to create a directory listing, for example.
   *  
   * @return an XML element containing information about all contained children
   */
  public Element getMetadata()
  { return metadata; }
  
  /**
   * Repairs the internal metadata structure by rebuilding all information from the
   * underlying filesystem. Can be invoked in case of corrupted metadata after external 
   * changes to the stored files. This also repairs metadata of all direct or indirect 
   * children. 
   */
  public void repairMetadata() throws Exception
  {
    metadata = new Document( new Element( "metadata" ) ).getRootElement();
    writeMetadata();
    
    for( MCRNode child : getChildren() )
      ((MCRStoredNode)child).repairMetadata();
    
    writeMetadata();
  }
}
