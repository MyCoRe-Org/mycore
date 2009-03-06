package org.mycore.datamodel.ifs2;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.jdom.Document;
import org.jdom.Element;

public class MCRFileCollection extends MCRDirectory 
{
  private String storeID;
  private int    ownerID;
  
  public MCRFileCollection( String storeID, int ownerID ) throws Exception
  { this( storeID, ownerID, true ); }
  
  protected MCRFileCollection( String storeID, int ownerID, boolean create ) throws Exception
  {
    super( null, getSlot( storeID, ownerID ) );
    this.storeID = storeID;
    this.ownerID = ownerID;
    if( create )
    {
      fo.createFolder();
      metadata = new Document( new Element( "metadata" ) ).getRootElement();
      writeMetadata();
    }
  }

  public static MCRFileCollection getRoot( String storeID, int ownerID ) throws Exception
  {
    MCRFileCollection root = new MCRFileCollection( storeID, ownerID, false );
    return ( root.fo.exists() ? root : null );
  }
  
  private static FileObject getSlot( String storeID, int ownerID ) throws Exception
  {
    String path = "file://c:/store"; // MCRConfiguration.instance().getString( "MCR.IFS.ContentStore." + storeID + ".BaseDir" );
    String slotDir = "000" + String.valueOf( ownerID );
    slotDir = slotDir.substring( slotDir.length() - 4 );
    slotDir = "/" + slotDir.substring( 0, 2 ) + "/" + slotDir.substring( 2 );
    path += slotDir + "/" + String.valueOf( ownerID );
    return VFS.getManager().resolveFile( path );
  }

  public void renameTo( String name )
  {}

  public String getStoreID()
  { return storeID; }
  
  public int getOwnerID()
  { return ownerID; }
  
  public String getPath()
  { return ""; }
  
  public MCRFileCollection getRoot()
  { return this; }
}
