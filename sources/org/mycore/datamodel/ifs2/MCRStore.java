package org.mycore.datamodel.ifs2;

import java.io.File;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.mycore.backend.sql.MCRSQLConnection;
import org.mycore.common.MCRConfigurationException;

public class MCRStore 
{
  protected String id;
  protected File dir;
  protected int idLength;
  protected int[] slotLength;
  protected String prefix; 
  protected String suffix;
  
  private static HashMap<String,MCRStore> stores;
  
  static
  {
    stores = new HashMap<String,MCRStore>();
    
    MCRStore store = new MCRStore( "TEST", "c:\\store", "2-3-8", "", "" );
    stores.put( store.getID(), store );
  }
  
  public static MCRStore getStore( String id )
  { return stores.get( id ); }
  
  protected MCRStore( String id, String baseDir, String slotLayout, String prefix, String suffix )
  {
    this.id = id;
    this.prefix = prefix;
    this.suffix = suffix;
    
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
    path.append( prefix ).append( id ).append( suffix );
    
    return VFS.getManager().resolveFile( dir, path.toString() );
  }

  public boolean exists( int ownerID ) throws Exception
  { return getSlot( ownerID ).exists(); }
  
  
  protected int offset = 10; // Initially 10, later 1
  protected int lastID = 0;

  public synchronized int getNextFreeID()
  {
    int found = findMaxID();
    lastID = Math.max( found, lastID );
    lastID += ( lastID > 0 ? offset : 1 );
    offset = 1;
    return lastID;
  }

  private int findMaxID()
  {
    File d = dir;
    String max = "";
    
    for( int i = 0; i <= slotLength.length; i++ )
    {
      File[] children = d.listFiles();
      if( children != null )
      {
        for( File child : children )
        {
          if( child.getName().compareTo( max ) > 0 )
            max = child.getName();
          d = new File( d, max );
        }
      }
    }
    
    if( max == "" ) return 0;
    
    max = max.substring( prefix.length() );
    max = max.substring( 0, idLength );
    return Integer.parseInt( max );
  }
}
