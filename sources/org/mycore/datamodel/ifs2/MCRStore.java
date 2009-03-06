package org.mycore.datamodel.ifs2;

import java.io.File;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.mycore.common.MCRConfigurationException;

public class MCRStore 
{
  private String id;
  private File dir;
  private int idLength;
  private int[] slotLength;
  
  private static HashMap<String,MCRStore> stores;
  
  static
  {
    stores = new HashMap<String,MCRStore>();
    
    MCRStore store = new MCRStore( "TEST", "c:\\store", "2-3-8" );
    stores.put( store.getID(), store );
  }
  
  public static MCRStore getStore( String id )
  { return stores.get( id ); }
  
  private MCRStore( String id, String baseDir, String slotLayout )
  {
    this.id = id;
    this.idLength = Integer.parseInt( slotLayout.substring( slotLayout.lastIndexOf( "-" ) + 1 ) );
    
    StringTokenizer st = new StringTokenizer( slotLayout, "-" );
    slotLength = new int[ st.countTokens() - 1 ];
    
    int i = 0;
    while( st.countTokens() > 1 )
      slotLength[ i++ ] = Integer.parseInt( st.nextToken() );
    
    dir = new File( baseDir );
    if( ! dir.exists() )
    {
      try
      {
        boolean created = dir.mkdirs();
        if( ! created )
        {
          String msg = "Unable to create store directory " + baseDir;
          throw new MCRConfigurationException( msg );  
        }
      }
      catch( Exception ex )
      {
        String msg = "Exception while creating store directory " + baseDir;
        throw new MCRConfigurationException( msg, ex );  
      }
    }
    else
    {
      if( ! dir.canRead() )
      {
        String msg = "Store directory " + baseDir + " is not readable";
        throw new MCRConfigurationException( msg );  
      }
      if( ! dir.isDirectory() )
      {
        String msg = "Store " + baseDir + " is a file, not a directory";
        throw new MCRConfigurationException( msg );  
      }
    }
  }
  
  public String getID()
  { return id; }
  
  private static String nulls = "00000000000000000000000000000000";
  
  FileObject getSlot( int ownerID ) throws Exception
  {
    String id = nulls + String.valueOf( ownerID );
    id = id.substring( id.length() - idLength );
    
    int offset = 0;
    StringBuffer path = new StringBuffer();
    for( int i = 0; i < slotLength.length; i++ )
    {
      path.append( id.substring( offset, offset + slotLength[ i ] ) );
      offset += slotLength[ i ];
      path.append( "/" );
    }
    path.append( id );
    
    return VFS.getManager().resolveFile( dir, path.toString() );
  }

  public boolean exists( int ownerID ) throws Exception
  { return getSlot( ownerID ).exists(); }
  
  public MCRFileCollection createCollection( int ownerID ) throws Exception
  {
    FileObject slotDir = getSlot( ownerID );
    if( slotDir.exists() )
    {
      String msg = "Collection " + ownerID + " already exists";
      throw new MCRConfigurationException( msg );  
    }
    slotDir.createFolder();
    
    MCRFileCollection coll = new MCRFileCollection( this, ownerID, slotDir );
    coll.writeMetadata();
    return coll;
  }

  public MCRFileCollection retrieveCollection( int ownerID ) throws Exception
  {
    FileObject slotDir = getSlot( ownerID );
    if( ! slotDir.exists() ) 
      return null;
    else
      return new MCRFileCollection( this, ownerID, slotDir );
  }
}
