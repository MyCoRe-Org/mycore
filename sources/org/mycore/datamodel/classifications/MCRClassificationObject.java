/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  *** 
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
 
package mycore.classifications;

import java.util.ArrayList;
import java.util.Vector;
import mycore.common.*;

/**
 * This class is an abstract class for the implementation of the classes
 * classification an category.
 *
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public abstract class MCRClassificationObject
  {
  /** The length of the label **/
  public static final int MAX_CLASSIFICATION_LABEL = 250;
  /** The length of the description **/
  public static final int MAX_CLASSIFICATION_DESCRIPTION = 1024;

  protected String    ID;
  protected ArrayList label;
  protected ArrayList lang;
  protected ArrayList description;
  protected String [] childrenIDs;
  
  protected boolean deleted = false;

  protected static String default_lang = "en";

  /**
   * Load static data for all MCRClassificationObject
   **/
  static 
    { 
    default_lang = MCRConfiguration.instance()
      .getString("MCR.metadata_default_lang","en");
    }
  
  /**
   * The method get the MCRClassificationManager instance.
   *
   * @return the MCRClassificationManager instance.
   **/
  protected static MCRClassificationManager manager()
    { return MCRClassificationManager.instance(); }
  
  /**
   * The abstract constructor of a classififcation or a category.
   *
   * @param ID an identifier String
   **/
  public MCRClassificationObject( String ID )
    {
    MCRArgumentChecker.ensureNotEmpty( ID, "ID" );
    this.ID    = ID;
    this.label = new ArrayList(); 
    this.lang = new ArrayList(); 
    this.description = new ArrayList();
    this.childrenIDs = null; 
    this.deleted = false;
    }

  /**
   * The method ensure that the classification is not deleted.
   * @exception MCRUsageException if the classification is deleted.
   **/
  protected void ensureNotDeleted() throws MCRUsageException
    {
    if( this.deleted ) throw new MCRUsageException
    ( "This classification object is invalid because it has been deleted" );
    }

  /**
   * This method get the ID.
   * @return the ID
   * @exception MCRUsageException if the object is deleted.
   **/
  public String getID() throws MCRUsageException
    { ensureNotDeleted(); return ID; }
  
  /**
   * This method get the classification ID.
   * @return the classification ID
   **/
  protected abstract String getClassificationID();

  /**
   * The method return the label ArrayList.
   * @exception MCRUsageException if the classification is deleted.
   **/
  public ArrayList getLabelArray() throws MCRUsageException
    { ensureNotDeleted(); return label; }
  
  /**
   * The method return the label String for a given index.
   *
   * @param index a index in the ArrayList
   * @exception MCRUsageException if the classification is deleted.
   **/
  public String getLabel(int index) throws MCRUsageException
    { ensureNotDeleted(); return ((String)label.get(index)); }
  
  /**
   * The method returns the label String for a given language.
   *
   * @param index a language
   * @exception MCRUsageException if the classification is deleted.
   **/
  public String getLabel(String lang) throws MCRUsageException
    { 
    ensureNotDeleted(); 
    if (this.lang.size()==0) { return ""; }
    if (this.lang==null) { return ((String)this.label.get(0)); }
    for (int i=0;i<this.lang.size();i++) {
      if (((String)this.lang.get(i)).equals(lang)) {
        return (String)this.label.get(i); }
      }
    return "";
    }
  
  /**
   * The method return the lang ArrayList.
   * @exception MCRUsageException if the classification is deleted.
   **/
  public ArrayList getLangArray() throws MCRUsageException
    { ensureNotDeleted(); return lang; }
  
  /**
   * The method return the lang String for a given index.
   *
   * @param index a index in the ArrayList
   * @exception MCRUsageException if the classification is deleted.
   **/
  public String getLang(int index) throws MCRUsageException
    { ensureNotDeleted(); return ((String)lang.get(index)); }
  
  /**
   * The method return the description ArrayList.
   * @exception MCRUsageException if the classification is deleted.
   **/
  public ArrayList getDescriptionArray() throws MCRUsageException
    { ensureNotDeleted(); return description; }
  
  /**
   * The method return the description String for a given index.
   *
   * @param index a index in the ArrayList
   * @exception MCRUsageException if the classification is deleted.
   **/
  public String getDescription(int index) throws MCRUsageException
    { ensureNotDeleted(); return ((String)description.get(index)); }
  
  /**
   * The method returns the description String for a given language.
   *
   * @param index a language
   * @exception MCRUsageException if the classification is deleted.
   **/
  public String getDescription(String lang) throws MCRUsageException
    { 
    ensureNotDeleted(); 
    if (this.description.size()==0) { return ""; }
    if (this.lang==null) { return ((String)this.description.get(0)); }
    for (int i=0;i<this.lang.size();i++) {
      if (((String)this.lang.get(i)).equals(lang)) {
        return (String)this.description.get(i); }
      }
    return "";
    }
  
  /**
   * The method add a triple of a lang with a lable and a description
   * to the object. The label and the description can be an empty string.
   *
   * @param lang a language in form of a 'xml:lang' attribute
   * @param label a label String
   * @param description a description String
   **/
  public void addData( String lang, String label, String description )
    {
    ensureNotDeleted();
    if (lang==null) { lang = default_lang; }
    if (label==null) { label = ""; }
    if (label.length() > MAX_CLASSIFICATION_LABEL) {
      label = label.substring(0,MAX_CLASSIFICATION_LABEL); }
    if (description==null) { description = ""; }
    if (description.length() > MAX_CLASSIFICATION_DESCRIPTION) {
      description = description.substring(0,MAX_CLASSIFICATION_DESCRIPTION); }
    this.label.add(label);
    this.lang.add(lang);
    this.description.add(description);
    }
  
  /**
   * The method set a triple of a lang with a lable and a description
   * to the object. The label and the description can be an empty string.
   *
   * @param lang a language in form of a 'xml:lang' attribute
   * @param label a label String
   * @param description a description String
   **/
  public void setData( String lang, String label, String description )
    {
    ensureNotDeleted();
    if (lang==null) { lang = default_lang; }
    if (label==null) { label = ""; }
    if (label.length() > MAX_CLASSIFICATION_LABEL) {
      label = label.substring(0,MAX_CLASSIFICATION_LABEL); }
    if (description==null) { description = ""; }
    if (description.length() > MAX_CLASSIFICATION_DESCRIPTION) {
      description = description.substring(0,MAX_CLASSIFICATION_DESCRIPTION); }
    int i = -1;
    for (int j=0;j<this.lang.size();j++) {
      if (((String)this.lang.get(i)).equals(lang)) { i = j; break; }
      }
    if (i != -1) { 
      this.label.add(i,label);
      this.lang.add(i,lang);
      this.description.add(i,description);
      return;
      }
    this.label.add(label);
    this.lang.add(lang);
    this.description.add(description);
    }
  
  /**
   * The method return the size of the lang/label/description triple.
   *
   * @return the size
   **/
  public int getSize()
    { return label.size(); }

  /**
   * The method create an internal tag string for the data values
   *
   * @return an internal data string
   **/
  public String getTag()
    {
    StringBuffer sb = new StringBuffer(4096);
    for (int i=0;i<getSize();i++) {
      sb.append("<label xml:lang=\"").append(lang.get(i)).append("\" text=\"")
        .append(label.get(i)).append("\" description=\"")
        .append(description.get(i)).append("\" />");
      }
    return sb.toString();
    }

  /**
   * The method store the internal tag string to the data.
   *
   * @param the tag sting
   **/
  public void setTag(String tag)
    {
    int i = 0;
    int j = tag.length();
    int k = 0, l = 0;
    while (i<j) {
      k = tag.indexOf("<label",i);
      if (k == -1) { break; }
      i = k+6;
      k = tag.indexOf("xml:lang=",i);
      l = tag.indexOf("\"",k+10);
      String tlang = tag.substring(k+10,l);
      i = l+1;
      k = tag.indexOf("text=",i);
      l = tag.indexOf("\"",k+6);
      String ttext = tag.substring(k+6,l);
      i = l+1;
      k = tag.indexOf("description=",i);
      l = tag.indexOf("\"",k+13);
      String tdescription = tag.substring(k+13,l);
      i = l+1;
      addData(tlang,ttext,tdescription);
      }
    }

  /**
   * The method return the label tag as an JDOM element
   *
   * @param index the index of the label
   * @return the label as JDOM element
   **/
  public final org.jdom.Element getJDOMElement(int index)
    {
    if ((index<0) || (index>getSize())) return null;
    org.jdom.Element elm = new org.jdom.Element("label");
    elm.setAttribute("xml:lang",getLang(index));
    elm.setAttribute("text",getLabel(index));
    elm.setAttribute("description",getDescription(index));
    return elm;
    }

  /**
   * The method check that this instance has children.
   *
   * @return true if it is, else false
   **/
  public boolean hasChildren()
    { 
    ensureNotDeleted();
    return( getNumChildren() > 0 ); 
    }
  
  /**
   * The method return the number of chlidren.
   *
   * @return the number of chlidren
   **/
  public int getNumChildren()
    {
    ensureNotDeleted();
    if( childrenIDs != null )
      return childrenIDs.length;
    else
      return manager().retrieveNumberOfChildren( getClassificationID(), ID );
    }
  
  /**
   * The method return a list of MCRCategoryItems they are childre
   * of this.
   *
   * @return a list of children
   **/
  public MCRCategoryItem[] getChildren()
    {
    ensureNotDeleted();
    MCRCategoryItem[] children;
    if( childrenIDs == null ) {
      String parentID = ( this instanceof MCRCategoryItem ? ID : null );
      children = manager().retrieveChildren( getClassificationID(), parentID );
      childrenIDs = new String[ children.length ];
      for( int i = 0; i < children.length; i++ )
        childrenIDs[ i ] = children[ i ].getID();
      }
    else {
      children = new MCRCategoryItem[ childrenIDs.length ];
      for( int i = 0; i < children.length; i++ )
        children[ i ] = manager().retrieveCategoryItem( getClassificationID(), 
          childrenIDs[ i ] );
      }
    return children;
    }
  
  /**
   * The method remove this.
   **/
  public void delete()
    {
    ensureNotDeleted();
    MCRCategoryItem[] children = getChildren();
    for( int i = 0; i < children.length; i++ )
      children[ i ].delete();
    deleted = true;
    }

  /**
   * Put all data to a string
   **/
  public String toString()
  {
    StringBuffer sb = new StringBuffer( getClass().getName() ).append( "\n" );
    sb.append( "ID:             " ).append( ID    ).append( "\n" );
    for (int i=0;i<label.size();i++) {
      sb.append( "Lang:           " ).append( ((String)lang.get(i)) )
        .append( "\n" )
        .append( "Label:          " ).append( ((String)label.get(i)) ) 
        .append( "\n" )
        .append( "Description:    " ).append( ((String)description.get(i)) ) 
        .append( "\n" );
      }
    sb.append( "\n" );
    return sb.toString();
  }
}
