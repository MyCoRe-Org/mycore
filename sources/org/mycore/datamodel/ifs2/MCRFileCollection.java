package org.mycore.datamodel.ifs2;

public class MCRFileCollection extends MCRDirectory 
{
  private MCRStore store;
  private int ownerID;
  
  public MCRFileCollection( MCRStore store ) throws Exception
  { this( store, store.getNextFreeID() ); }
  
  public MCRFileCollection( MCRStore store, int ownerID ) throws Exception
  {
    super( null, store.getSlot( ownerID ) );
    
    this.store = store;
    this.ownerID = ownerID;

    if( ! fo.exists() )
    {
      fo.createFolder();
      readMetadata();
      writeMetadata();
    }
    else readMetadata();
  }

  public void renameTo( String name )
  {}

  public MCRStore getStore()
  { return store; }
  
  public int getOwnerID()
  { return ownerID; }
  
  public String getPath()
  { return ""; }
  
  public MCRFileCollection getRoot()
  { return this; }
}
