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
 
package org.mycore.datamodel.classifications;

import java.util.ArrayList;
import java.util.Vector;
import org.mycore.common.*;

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
  /** The number of the languages **/
  public static final int MAX_CLASSIFICATION_LANG = 8;
  /** The length of the text **/
  public static final int MAX_CLASSIFICATION_TEXT = 256;
  /** The length of the description **/
  public static final int MAX_CLASSIFICATION_DESCRIPTION = 256;

  protected String    ID;
  protected ArrayList lang;
  protected ArrayList text;
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
      .getString("MCR.metadata_default_lang",default_lang);
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
    this.text = new ArrayList(); 
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
   * The method return the text ArrayList.
   * @exception MCRUsageException if the classification is deleted.
   **/
  public ArrayList getTextArray() throws MCRUsageException
    { ensureNotDeleted(); return text; }
  
  /**
   * The method return the text String for a given index.
   *
   * @param index a index in the ArrayList
   * @exception MCRUsageException if the classification is deleted.
   **/
  public String getText(int index) throws MCRUsageException
    { 
    ensureNotDeleted(); 
    if ((index<0)||(index>text.size())) { return ""; }
    return ((String)text.get(index)); 
    }
  
  /**
   * The method returns the text String for a given language.
   *
   * @param index a language
   * @exception MCRUsageException if the classification is deleted.
   **/
  public String getText(String lang) throws MCRUsageException
    { 
    ensureNotDeleted(); 
    if (this.text.size()==0) { return ""; }
    if (!MCRUtils.isSupportedLang(lang)) {
      return (String)this.text.get(0); }
    for (int i=0;i<this.lang.size();i++) {
      if (((String)this.lang.get(i)).equals(lang)) {
        return (String)this.text.get(i); }
      }
    return (String)this.text.get(0);
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
    { 
    ensureNotDeleted(); 
    if ((index<0)||(index>text.size())) { return default_lang; }
    return ((String)lang.get(index)); 
    }
  
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
    { 
    ensureNotDeleted(); 
    if ((index<0)||(index>text.size())) { return ""; }
    return ((String)description.get(index)); 
    }
  
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
    if (!MCRUtils.isSupportedLang(lang)) { 
      return (String)this.description.get(0); }
    for (int i=0;i<this.lang.size();i++) {
      if (((String)this.lang.get(i)).equals(lang)) {
        return (String)this.description.get(i); }
      }
    return (String)this.description.get(0);
    }
  
  /**
   * The method add a triple of a lang with a lable and a description
   * to the object. The text and the description can be an empty string.
   *
   * @param lang a language in form of a 'xml:lang' attribute
   * @param text a text String
   * @param description a description String
   **/
  public void addData( String lang, String text, String description )
    {
    ensureNotDeleted();
    if (lang==null) { lang = default_lang; }
    if (text==null) { text = ""; }
    if (text.length() > MAX_CLASSIFICATION_TEXT) {
      text = text.substring(0,MAX_CLASSIFICATION_TEXT); }
    if (description==null) { description = ""; }
    if (description.length() > MAX_CLASSIFICATION_DESCRIPTION) {
      description = description.substring(0,MAX_CLASSIFICATION_DESCRIPTION); }
    this.text.add(text);
    this.lang.add(lang);
    this.description.add(description);
    }
  
  /**
   * The method set a triple of a lang with a lable and a description
   * to the object. The text and the description can be an empty string.
   *
   * @param lang a language in form of a 'xml:lang' attribute
   * @param text a text String
   * @param description a description String
   **/
  public void setData( String lang, String text, String description )
    {
    ensureNotDeleted();
    if (lang==null) { lang = default_lang; }
    if (text==null) { text = ""; }
    if (text.length() > MAX_CLASSIFICATION_TEXT) {
      text = text.substring(0,MAX_CLASSIFICATION_TEXT); }
    if (description==null) { description = ""; }
    if (description.length() > MAX_CLASSIFICATION_DESCRIPTION) {
      description = description.substring(0,MAX_CLASSIFICATION_DESCRIPTION); }
    int i = -1;
    for (int j=0;j<this.lang.size();j++) {
      if (((String)this.lang.get(i)).equals(lang)) { i = j; break; }
      }
    if (i != -1) { 
      this.text.add(i,text);
      this.lang.add(i,lang);
      this.description.add(i,description);
      return;
      }
    this.text.add(text);
    this.lang.add(lang);
    this.description.add(description);
    }
  
  /**
   * The method return the size of the lang/text/description triple.
   *
   * @return the size
   **/
  public int getSize()
    { return text.size(); }

  /**
   * The method return the text tag as an JDOM element
   *
   * @param index the index of the text
   * @return the text as JDOM element
   **/
  public final org.jdom.Element getJDOMElement(int index)
    {
    if ((index<0) || (index>getSize())) return null;
    org.jdom.Element elm = new org.jdom.Element("label");
    elm.setAttribute("xml:lang",getLang(index));
    elm.setAttribute("text",getText(index));
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
    for (int i=0;i<text.size();i++) {
      sb.append( "Lang:           " ).append( ((String)lang.get(i)) )
        .append( "\n" )
        .append( "Text:          " ).append( ((String)text.get(i)) ) 
        .append( "\n" )
        .append( "Description:    " ).append( ((String)description.get(i)) ) 
        .append( "\n" );
      }
    sb.append( "\n" );
    return sb.toString();
  }
}
