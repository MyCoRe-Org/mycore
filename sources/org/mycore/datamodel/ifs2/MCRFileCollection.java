package org.mycore.datamodel.ifs2;

import org.apache.commons.vfs.FileObject;

public class MCRFileCollection extends MCRDirectory 
{
  private MCRStore store;
  private int id;

  protected MCRFileCollection( MCRStore store, int id, FileObject fo, boolean create ) throws Exception
  {
    super( null, fo );
    this.store = store;
    this.id = id;
    
    if( create )
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
  
  public int getID()
  { return id; }
  
  public String getPath()
  { return ""; }
  
  public MCRFileCollection getRoot()
  { return this; }
}
