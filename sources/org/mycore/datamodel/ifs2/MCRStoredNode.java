package org.mycore.datamodel.ifs2;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.jdom.Element;

public abstract class MCRStoredNode extends MCRNode
{
  protected String label;
  
  protected MCRStoredNode( MCRDirectory parent, FileObject fo ) throws Exception
  { super( parent, fo ); }
  
  public void delete() throws Exception
  { 
    if( parent != null ) 
     ((MCRDirectory)parent).removeMetadata( this );
  }
  
  protected void updateMetadata() throws Exception
  {
    if( parent != null )
      ((MCRDirectory)parent).updateMetadata( this.getName(), this ); 
  }

  protected void writeChildData( Element entry ) throws Exception
  {
    entry.setAttribute( "name", this.getName() );
    if( label != null )
      entry.setAttribute( "label", this.getLabel() );
  }
  
  protected void readChildData( Element entry ) throws Exception
  { label = entry.getAttributeValue( "label", (String)null ); }
  
  public void renameTo( String name ) throws Exception
  {
    String oldName = getName();
    FileObject fNew = VFS.getManager().resolveFile( fo.getParent(), name ); 
    fo.moveTo( fNew );
    fo = fNew;
    
    if( parent != null )
      ((MCRDirectory)parent).updateMetadata( oldName, this );
  }
  
  public void setLabel( String label ) throws Exception
  { 
    this.label = label; 
    updateMetadata();
  }
  
  public String getLabel()
  { return label; }
}
