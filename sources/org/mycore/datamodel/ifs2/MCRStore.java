package org.mycore.datamodel.ifs2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.mycore.common.MCRConfigurationException;

public abstract class MCRStore 
{
  protected String id;
  protected File dir;
  protected int idLength;
  protected int[] slotLength;
  protected String prefix; 
  protected String suffix;
  
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
  
  FileObject getSlot( int ID ) throws Exception
  {
    String id = nulls + String.valueOf( ID );
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

  public boolean exists( int id ) throws Exception
  { return getSlot( id ).exists(); }
  
  protected int offset = 10; // Sicherheitsabstand, initially 10, later 1
  protected int lastID = 0;

  public synchronized int getNextFreeID()
  {
    int found = 0;
    String max = findMaxID( dir, 0 );
    if( max != null ) found = slot2id( max );

    lastID = Math.max( found, lastID );
    lastID += ( lastID > 0 ? offset : 1 );
    offset = 1;
    return lastID;
  }
  
  private int slot2id( String slot )
  {
    slot = slot.substring( prefix.length() );
    slot = slot.substring( 0, idLength );
    return Integer.parseInt( slot );
  }
  
  private String findMaxID( File dir, int depth )
  {
    String[] children = dir.list();
    
    if( ( children == null ) || ( children.length == 0 ) )
      return null;
    
    Arrays.sort( children );

    if( depth == slotLength.length )  
      return children[ children.length - 1 ];
        
    for( int i = children.length - 1; i >= 0; i-- )
    {
      File child = new File( dir, children[ i ] );
      if( ! child.isDirectory() ) continue;
      String found = findMaxID( child, depth + 1 );
      if( found != null ) return found;
    }
    return null;
  }
  
  public final static boolean ASCENDING = true;
  public final static boolean DESCENDING = false;
  
  public Enumeration<Integer> listIDs( boolean order )
  { 
    return new Enumeration<Integer>()
    { 
      List<File> files = new ArrayList<File>();
      int nextID = -1;
      boolean order;
      
      Enumeration<Integer> init( boolean order )
      { 
        this.order = order;
        addChildren( files, dir );
        nextID = findNextID();
        return this;
      }
  
      private void addChildren( List<File> files, File dir )
      {
        String[] children = dir.list();
        if( children == null ) return;
        
        Arrays.sort( children );
        if( order )
          for( int i = 0; i < children.length; i++ )
            files.add( i, new File( dir, children[ i ] ) );
        else
          for( int i = children.length - 1; i >= 0; i-- )
            files.add( i, new File( dir, children[ i ] ) );
      }

      public boolean hasMoreElements()
      { return ( nextID > 0 ); }
  
      public Integer nextElement()
      {
        int id = nextID;
        nextID = findNextID();
        return id;
      }
      
      private int findNextID()
      {
        if( files.isEmpty() ) return 0;
    
        File first = files.remove( 0 );
        if( first.getName().length() == idLength )
          return MCRStore.this.slot2id( first.getName() );
    
        addChildren( files, first );
        return findNextID();
      }
    }.init( order );
  }
}