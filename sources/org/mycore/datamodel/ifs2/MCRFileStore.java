package org.mycore.datamodel.ifs2;

import java.util.Date;
import java.util.Enumeration;

import org.apache.commons.vfs.FileObject;
import org.mycore.common.MCRException;

public class MCRFileStore extends MCRStore 
{
  protected MCRFileStore( String id, String baseDir, String slotLayout )
  { super( id, baseDir, slotLayout, "", "" ); }
  
  public MCRFileCollection create() throws Exception
  {
    int id = getNextFreeID();
    return create( id );
  }
  
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
  
  public Date getLastModified( int id ) throws Exception
  {
    FileObject fo = getSlot( id );
    if( ! fo.exists() ) return null;
    long time = fo.getContent().getLastModifiedTime();
    return new Date( time );
  }
  
  public void repairAllMetadata() throws Exception
  {
    for( Enumeration<Integer> e = listIDs( MCRStore.ASCENDING ); e.hasMoreElements(); )
     retrieve( e.nextElement() ).repairMetadata();
  }
}
