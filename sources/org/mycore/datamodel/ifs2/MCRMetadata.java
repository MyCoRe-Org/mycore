package org.mycore.datamodel.ifs2;

import org.apache.commons.vfs.FileObject;

public class MCRMetadata extends MCRContent
{
  private FileObject slot;
  private MCRStore store;
  private int ID;
  
  public MCRMetadata( MCRStore store ) throws Exception
  { this( store, store.getNextFreeID() ); }
  
  public MCRMetadata( MCRStore store, int ID ) throws Exception
  {
    super( store.getSlot( ID ) );
    this.store = store;
    this.ID = ID;
    if( ! fo.exists() ) fo.createFile();
  }

  public MCRStore getStore()
  { return store; }
  
  public int getID()
  { return ID; }
  
  public long getLastModified() throws Exception
  { return slot.getContent().getLastModifiedTime(); }

  public void delete() throws Exception
  { slot.delete(); }
}
