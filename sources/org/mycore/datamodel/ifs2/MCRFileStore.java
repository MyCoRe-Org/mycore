package org.mycore.datamodel.ifs2;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.vfs.FileObject;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * Stores file collections containing files and directories.
 *
 * For each store, properties must be defined, for example
 * 
 * MCR.IFS2.FileStore.STOREID.BaseDir=c:\\store
 * MCR.IFS2.FileStore.STOREID.SlotLayout=3-3-2-8
 */
public class MCRFileStore extends MCRStore 
{
  /**
   * Map of defined file stores. Key is store ID, value is
   * the store with that ID.
   */
  private static HashMap<String,MCRFileStore> stores;
    
  /**
   * Reads configuration and initializes stores.
   */
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
      
  /**
   * Returns the store with the given ID
   *
   * @param id the store ID
   * @return the store with the given ID
   */
  public static MCRFileStore getStore( String id )
  { return stores.get( id ); }

  /**
   * Creates a new file store instance
   * 
   * @param id the store ID
   * @param baseDir the base directory storing data
   * @param slotLayout the layout of slot subdirectories
   */
  protected MCRFileStore( String id, String baseDir, String slotLayout )
  { 
    super( id, baseDir, slotLayout, "", "" );
    stores.put( id, this );
  }
  
  /**
   * Creates and stores a new, empty file collection using the 
   * next free ID in the store.
   * 
   * @return a newly created file collection
   */
  public MCRFileCollection create() throws Exception
  {
    int id = getNextFreeID();
    return create( id );
  }

  /**
   * Creates and stores a new, empty file collection with the given
   * ID
   * 
   * @param id the ID of the file collection
   * @return a newly created file collection
   * @throws Exception when a file collection with the given ID already exists
   */
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

  /**
   * Returns the file collection stored under the given ID.
   * 
   * @param id the file collection's ID
   * @return the file collection with the given ID
   * @throws Exception when a collection with that ID does not exist
   */
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
  
  /**
   * Repairs metadata of all file collections stored here
   * 
   * @throws Exception
   */
  public void repairAllMetadata() throws Exception
  {
    for( Enumeration<Integer> e = listIDs( MCRStore.ASCENDING ); e.hasMoreElements(); )
     retrieve( e.nextElement() ).repairMetadata();
  }
}
