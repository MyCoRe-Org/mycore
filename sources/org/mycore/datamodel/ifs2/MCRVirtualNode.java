package org.mycore.datamodel.ifs2;

import org.apache.commons.vfs.FileObject;

public class MCRVirtualNode extends MCRNode
{
  protected MCRVirtualNode( MCRNode parent, FileObject fo )
  { super( parent, fo ); }

  protected MCRNode buildChildNode( FileObject fo ) throws Exception
  { return new MCRVirtualNode( this, fo ); }
}
