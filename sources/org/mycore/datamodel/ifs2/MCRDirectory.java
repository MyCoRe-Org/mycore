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

public class MCRDirectory extends MCRStoredNode
{
  protected static String metadataFile = ".mcr-metadata.xml"; 
  protected Element metadata;
  
  protected MCRDirectory( MCRDirectory parent, FileObject fo ) throws Exception
  { 
    super( parent, fo );
    if( parent != null ) parent.readChildData( this );
    readMetadata();
  }
  
  public MCRDirectory( MCRDirectory parent, String name ) throws Exception
  { 
    super( parent, VFS.getManager().resolveFile( parent.fo, name ) );
    fo.createFolder();
    metadata = new Document( new Element( "metadata" ) ).getRootElement();
    writeMetadata();
  }
  
  public void delete() throws Exception
  { 
    super.delete();
    fo.delete( Selectors.SELECT_ALL );
  }
  
  protected MCRNode buildChildNode( FileObject fo ) throws Exception
  {
    if( fo.getType().equals( FileType.FILE ) )
      return new MCRFile( this, fo );
    else
      return new MCRDirectory( this, fo );
  }
  
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
  
  protected void removeMetadata( MCRStoredNode child ) throws Exception
  {
    findEntry( child.getName() ).detach();
    writeMetadata();
  }
  
  protected Element findEntry( String name ) throws Exception
  {
    for( Element entry : (List<Element>)( metadata.getChildren() ) )
      if( entry.getAttributeValue( "name" ).equals( name ) ) return entry;
    
    return null;
  }
  
  protected void readChildData( MCRStoredNode child ) throws Exception
  {
    Element entry = findEntry( child.getName() );
    if( entry != null ) child.readChildData( entry );
  }
  
  public Element getMetadata()
  { return metadata; }
  
  public void repairMetadata() throws Exception
  {
    metadata = new Document( new Element( "metadata" ) ).getRootElement();
    writeMetadata();
    
    for( MCRNode child : getChildren() )
      ((MCRStoredNode)child).repairMetadata();
    
    writeMetadata();
  }
}
