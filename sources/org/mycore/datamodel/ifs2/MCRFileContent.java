package org.mycore.datamodel.ifs2;

import java.io.InputStream;

import org.jdom.Document;

public class MCRFileContent extends MCRContent
{
  protected MCRFile owner;
  
  MCRFileContent( MCRFile owner )
  { 
    super( owner.fo );
    this.owner = owner;
  }

  public String setFrom( Document xml ) throws Exception
  {
    String md5 = super.setFrom( xml );
    owner.setMD5( md5 );
    owner.updateMetadata();
    return md5;
  }

  public String setFrom( InputStream source ) throws Exception 
  {
    String md5 = super.setFrom( source );
    owner.setMD5( md5 );
    owner.updateMetadata();
    return md5;
  }
}
