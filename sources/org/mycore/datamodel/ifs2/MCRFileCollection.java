package org.mycore.datamodel.ifs2;

import org.apache.commons.vfs.FileObject;

public class MCRFileCollection extends MCRDirectory 
{
  private MCRStore store;
  private int ownerID;
  
  MCRFileCollection( MCRStore store, int ownerID, FileObject slotDir ) throws Exception
  { 
    super( null, slotDir );
    this.store = store;
    this.ownerID = ownerID;
    readMetadata();
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
