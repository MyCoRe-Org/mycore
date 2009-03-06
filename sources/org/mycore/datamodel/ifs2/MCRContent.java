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
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRContentInputStream;

public abstract class MCRContent
{
  protected FileObject fo; 
  
  MCRContent( FileObject fo )
  { this.fo = fo; }
  
  public void setFrom( String uri ) throws Exception 
  {
    FileObject src = VFS.getManager().resolveFile( uri );
    InputStream in = src.getContent().getInputStream();
    setFrom( in );
    in.close();
  }

  public void setFrom( File source ) throws Exception 
  { 
    InputStream in = new FileInputStream( source ); 
    setFrom( in );
    in.close();
  }

  public String setFrom( Document xml ) throws Exception 
  {
    OutputStream out = fo.getContent().getOutputStream();
    MessageDigest digest = MCRContentInputStream.buildMD5Digest();
    DigestOutputStream dos = new DigestOutputStream( out, digest );
    
    XMLOutputter xout = new XMLOutputter();
    xout.setFormat( Format.getPrettyFormat().setEncoding( "UTF-8" ).setIndent( "  " ) );
    xout.output( xml, dos );

    dos.close();
    return MCRContentInputStream.getMD5String( digest );
  }

  public String setFrom( InputStream source ) throws Exception 
  {
    MCRContentInputStream cis = new MCRContentInputStream( source );
    OutputStream out = fo.getContent().getOutputStream();
    MCRUtils.copyStream( cis, out );
    out.close();
    return cis.getMD5String();
  }
  
  public InputStream getInputStream() throws Exception 
  { return fo.getContent().getInputStream(); }

  public void getTo( OutputStream out ) throws Exception 
  { 
    InputStream in = fo.getContent().getInputStream();
    MCRUtils.copyStream( in, out );
    in.close();
  }

  public void getTo( File target ) throws Exception 
  {
    OutputStream out = new FileOutputStream( target );
    getTo( out );
    out.close();
  }

  public Document getAsXML() throws Exception 
  { 
    InputStream in = getInputStream();
    Document xml = new SAXBuilder().build( in );
    in.close();
    return xml;
  }
}
