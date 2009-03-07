package org.mycore.datamodel.ifs2;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.vfs.FileObject;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRException;

public class MCRMetadataStore extends MCRStore 
{
  protected MCRMetadataStore( String id, String baseDir, String slotLayout, String type )
  { super( id, baseDir, slotLayout, type + "_", ".xml" ); }
  
  public int create( Document xml ) throws Exception
  {
    int id = getNextFreeID();
    create( xml, id );
    return id; 
  }
  
  public void create( Document xml, int id ) throws Exception
  {
    FileObject fo = getSlot( id );
    if( fo.exists() )
    {
      String msg = "Metadata object with ID " + id + " already exists in store";
      throw new MCRException( msg );
    }
    fo.createFile();
    write( xml, getSlot( id ) );
  }
  
  public void update( Document xml, int id ) throws Exception
  {
    FileObject fo = getSlot( id );
    if( ! fo.exists() )
    {
      String msg = "Metadata object with ID " + id + " does not exist in store";
      throw new MCRException( msg );
    }
    write( xml, getSlot( id ) );
  }
  
  protected void write( Document xml, FileObject fo ) throws Exception
  {
    OutputStream out = fo.getContent().getOutputStream();
    XMLOutputter xout = new XMLOutputter();
    xout.setFormat( Format.getPrettyFormat().setEncoding( "UTF-8" ).setIndent( "  " ) );
    xout.output( xml, out );
    out.close();
  }
  
  public Document retrieve( int id ) throws Exception
  {
    FileObject fo = getSlot( id );
    if( ! fo.exists() ) return null;
    InputStream in = fo.getContent().getInputStream();
    Document xml = new SAXBuilder().build( in );
    in.close();
    return xml;
  }
  
  public void delete( int id ) throws Exception
  {
    FileObject fo = getSlot( id );
    fo.delete();
  }
  
  public Date getLastModified( int id ) throws Exception
  {
    FileObject fo = getSlot( id );
    if( ! fo.exists() ) return null;
    long time = fo.getContent().getLastModifiedTime();
    return new Date( time );
  }
}
