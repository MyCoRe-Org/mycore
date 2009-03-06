package org.mycore.datamodel.ifs2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRContentInputStream;

public class MCRFile extends MCRStoredNode
{
  protected String md5;
  
  protected MCRFile( MCRDirectory parent, FileObject fo ) throws Exception
  { 
    super( parent, fo ); 
    parent.readChildData( this );
  }
  
  public MCRFile( MCRDirectory parent, String name ) throws Exception
  { 
    super( parent, VFS.getManager().resolveFile( parent.fo, name ) );
    md5 = "d41d8cd98f00b204e9800998ecf8427e"; // md5 of empty file
    fo.createFile();
    updateMetadata();
  }
  
  protected MCRNode buildChildNode( FileObject fo ) throws Exception
  { return new MCRVirtualNode( this, fo ); }
  
  protected void writeChildData( Element entry ) throws Exception
  {
    super.writeChildData( entry );
    entry.setAttribute( "md5", this.getMD5() );
    entry.setAttribute( "size", String.valueOf( this.getSize() ) );
  }
  
  protected void readChildData( Element entry ) throws Exception
  { md5 = entry.getAttributeValue( "md5" ); }
  
  public void delete() throws Exception
  {
    super.delete();
    fo.delete(); 
  }
  
  public void setLastModified( long time ) throws Exception
  { 
    fo.getContent().setLastModifiedTime( time );
    updateMetadata();
  }
  
  public String getMD5()
  { return md5; } 
  
  public void setContentFrom( String uri ) throws Exception 
  {
    FileObject src = VFS.getManager().resolveFile( uri );
    InputStream in = src.getContent().getInputStream();
    setContentFrom( in );
    in.close();
  }

  public void setContentFrom( File source ) throws Exception 
  { 
    InputStream in = new FileInputStream( source ); 
    setContentFrom( in );
    in.close();
  }

  public void setContentFrom( Document xml ) throws Exception 
  {
    OutputStream out = fo.getContent().getOutputStream();
    MessageDigest digest = MCRContentInputStream.buildMD5Digest();
    DigestOutputStream dos = new DigestOutputStream( out, digest );
    
    XMLOutputter xout = new XMLOutputter();
    xout.setFormat( Format.getCompactFormat().setEncoding( "UTF-8" ) );
    xout.output( xml, dos );

    dos.close();
    md5 = MCRContentInputStream.getMD5String( digest );
    updateMetadata();
  }

  public void setContentFrom( InputStream source ) throws Exception 
  {
    MCRContentInputStream cis = new MCRContentInputStream( source );
    OutputStream out = fo.getContent().getOutputStream();
    MCRUtils.copyStream( cis, out );
    out.close();
    md5 = cis.getMD5String();
    updateMetadata();
  }
  
  public InputStream getContent() throws Exception 
  { return fo.getContent().getInputStream(); }

  public void getContentTo( OutputStream out ) throws Exception 
  { 
    InputStream in = fo.getContent().getInputStream();
    MCRUtils.copyStream( in, out );
    in.close();
  }

  public void getContentTo( File target ) throws Exception 
  {
    OutputStream out = new FileOutputStream( target );
    getContentTo( out );
    out.close();
  }

  public Document getContentAsJDOM() throws Exception 
  { 
    InputStream in = getContent();
    Document xml = new SAXBuilder().build( in );
    in.close();
    return xml;
  }
}
