package org.mycore.datamodel.ifs2;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.vfs.FileObject;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

public class MCRMetadataStore extends MCRStore 
{
  private static HashMap<String,MCRMetadataStore> stores;
    
  static
  {
    stores = new HashMap<String,MCRMetadataStore>();
   
    // MCR.IFS2.MetadataStore.DocPortal_document.BaseDir=c:\\store
    // MCR.IFS2.MetadataStore.DocPortal_document.SlotLayout=3-3-2-8
    
    String prefix = "MCR.IFS2.MetadataStore.";
    MCRConfiguration config = MCRConfiguration.instance();
    Properties prop = config.getProperties( prefix );
    for( Enumeration keys = prop.keys(); keys.hasMoreElements(); )
    {
      String key = (String)(keys.nextElement());
      if( ! key.endsWith( "BaseDir" ) ) continue;
      String baseDir = prop.getProperty( key );
      String type = key.substring( prefix.length(), key.indexOf( ".BaseDir" ) );
      String slotLayout = config.getString( prefix + type + ".SlotLayout" );
      new MCRMetadataStore( type, baseDir, slotLayout );
    }
  }
    
  public static MCRMetadataStore getStore( String type )
  { return stores.get( type ); }
    
  protected MCRMetadataStore( String type, String baseDir, String slotLayout )
  { 
    super( type, baseDir, slotLayout, type + "_", ".xml" ); 
    stores.put( type, this );
  }
  
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
