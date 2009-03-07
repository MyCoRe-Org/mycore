package org.mycore.datamodel.ifs2;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.vfs.FileObject;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

public class MCRFileStore extends MCRStore 
{
  private static HashMap<String,MCRFileStore> stores;
    
  static
  {
    stores = new HashMap<String,MCRFileStore>();
     
    // MCR.IFS2.FileStore.FILES.BaseDir=c:\\store
    // MCR.IFS2.FileStore.FILES.SlotLayout=3-3-2-8
      
    String prefix = "MCR.IFS2.FileStore.";
    MCRConfiguration config = MCRConfiguration.instance();
    Properties prop = config.getProperties( prefix );
    for( Enumeration keys = prop.keys(); keys.hasMoreElements(); )
    {
      String key = (String)(keys.nextElement());
      if( ! key.endsWith( "BaseDir" ) ) continue;
      String baseDir = prop.getProperty( key );
      String id = key.substring( prefix.length(), key.indexOf( ".BaseDir" ) );
      String slotLayout = config.getString( prefix + id + ".SlotLayout" );
      new MCRFileStore( id, baseDir, slotLayout );
    }
  }
      
  public static MCRFileStore getStore( String id )
  { return stores.get( id ); }
      
  protected MCRFileStore( String id, String baseDir, String slotLayout )
  { 
    super( id, baseDir, slotLayout, "", "" );
    stores.put( id, this );
  }
  
  public MCRFileCollection create() throws Exception
  {
    int id = getNextFreeID();
    return create( id );
  }
  
  public MCRFileCollection create( int id ) throws Exception
  {
    FileObject fo = getSlot( id );
    if( fo.exists() )
    {
      String msg = "FileCollection with ID " + id + " already exists";
      throw new MCRException( msg );
    }
    return new MCRFileCollection( this, id, getSlot( id ), true ); 
  }
  
  public MCRFileCollection retrieve( int id ) throws Exception
  {
    FileObject fo = getSlot( id );
    if( ! fo.exists() )
    {
      String msg = "FileCollection with ID " + id + " does not exist";
      throw new MCRException( msg );
    }
    return new MCRFileCollection( this, id, getSlot( id ), false ); 
  }
  
  public Date getLastModified( int id ) throws Exception
  {
    FileObject fo = getSlot( id );
    if( ! fo.exists() ) return null;
    long time = fo.getContent().getLastModifiedTime();
    return new Date( time );
  }
  
  public void repairAllMetadata() throws Exception
  {
    for( Enumeration<Integer> e = listIDs( MCRStore.ASCENDING ); e.hasMoreElements(); )
     retrieve( e.nextElement() ).repairMetadata();
  }
}
