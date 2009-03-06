package org.mycore.datamodel.ifs2;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.jdom.Element;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRMetaISO8601Date;

public abstract class MCRStoredNode extends MCRNode
{
  protected SortedMap<String,String> labels;
  
  protected MCRStoredNode( MCRDirectory parent, FileObject fo ) throws Exception
  { 
    super( parent, fo );
    labels = new TreeMap<String,String>();
  }
  
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

    entry.setAttribute( "numChildren", String.valueOf( this.getNumChildren() ) );
    
    MCRMetaISO8601Date date = new MCRMetaISO8601Date();
    date.setDate( new Date( this.getLastModified() ) );
    date.setFormat( MCRMetaISO8601Date.IsoFormat.COMPLETE_HH_MM_SS );
    entry.setAttribute( "lastModified", date.getISOString() );
    
    entry.removeChildren( "label" );
    if( ! labels.isEmpty() )
    {
      Iterator<String> it = labels.keySet().iterator();
      while( it.hasNext() )
      {
        String lang = it.next();  
        String label = labels.get( lang );
        entry.addContent( new Element( "label" ).setAttribute( "lang", lang ).setText( label ) );
      }
    }
  }
  
  protected void readChildData( Element entry ) throws Exception
  { 
    labels.clear();
    for( Element label : (List<Element>)( entry.getChildren( "label" ) ) )
      labels.put( label.getAttributeValue( "lang" ), label.getTextTrim() );  
  }
  
  public void renameTo( String name ) throws Exception
  {
    String oldName = getName();
    FileObject fNew = VFS.getManager().resolveFile( fo.getParent(), name ); 
    fo.moveTo( fNew );
    fo = fNew;
    
    if( parent != null )
      ((MCRDirectory)parent).updateMetadata( oldName, this );
  }
  
  public void setLabel( String lang, String label ) throws Exception
  { 
    labels.put( lang, label ); 
    updateMetadata();
  }
  
  public void clearLabels() throws Exception
  {
    labels.clear();
    updateMetadata();
  }
  
  public SortedMap<String,String> getLabels()
  { return labels; }
  
  public String getLabel( String lang )
  { return labels.get( lang ); }
  
  private final static String defaultLang = "de";
  
  public String getCurrentLabel()
  {
    if( labels.isEmpty() ) return null;
    String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
    String label = labels.get( lang );
    if( label != null ) return label;
    label = labels.get( defaultLang );
    if( label != null ) return label;
    return labels.get( labels.firstKey() );
  }
}
