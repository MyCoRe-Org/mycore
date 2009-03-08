package org.mycore.datamodel.ifs2;

import org.apache.commons.vfs.FileObject;

/**
 * Represents a set of files and directories belonging together, that are
 * stored in a persistent MCRFileStore. A FileCollection has a unique ID within
 * the store, it is the root folder of all files and directories in the collection. 
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFileCollection extends MCRDirectory 
{
  /**
   * The store this file collection is stored in.
   */
  private MCRStore store;
  
  /**
   * The ID of this file collection
   */
  private int id;

  /**
   * Creates a new file collection in the given store, or retrieves an 
   * existing one.
   *
   * @see MCRFileStore 
   * 
   * @param store the store this file collection is stored in
   * @param id the ID of this file collection
   * @param fo the directory in the local filesystem storing this file collection
   * @param create if true, a new folder for the collection is created, otherwise the collection is assumed existing 
   */
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

  /**
   * Does nothing, because a file collection's name is always the empty string and 
   * therefore can not be renamed.
   */
  public void renameTo( String name )
  {}

  /**
   * Returns the store this file collection is stored in.
   * 
   * @return the store this file collection is stored in.
   */
  public MCRStore getStore()
  { return store; }
  
  /**
   * Returns the ID of this file collection
   * 
   * @return the ID of this file collection
   */
  public int getID()
  { return id; }
  
  /**
   * Returns the empty String
   * 
   * @return the empty String
   */
  public String getPath()
  { return ""; }
  
  /**
   * Returns this object, because the FileCollection instance is
   * the root of all files and directories contained in the collection.
   * 
   * @return this
   */
  public MCRFileCollection getRoot()
  { return this; }
}
