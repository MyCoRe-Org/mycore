/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.backend.cm7;

import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;
import java.util.*;
import java.io.File;
import org.mycore.common.MCRPersistenceException;

/**
 * <B>This class implements the access routines for a IBM Content Manager
 * item.</B>
 *
 * @author Frank Luetzenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public final class MCRCM7Item implements DKConstant
  {
  protected DKDDO ddo;
  protected static Hashtable properties = new Hashtable();

  /**
   * Constructor for a new Item.
   * 
   * @param ddo                 a DKDDO object
   * @exception DKException     Exceptions of CM
   **/
  public MCRCM7Item( DKDDO ddo ) throws DKException
    { this.ddo = ddo; }

  /**
   * Constructor for a new Item with given DL connection.
   * 
   * @param connection          the given DL connection
   * @param indexClass          the given Index Class
   * @param itemId              the item ID for the new item
   * @exception DKException     Exceptions of CM
   **/
  public MCRCM7Item(DKDatastoreDL connection, String indexClass, String itemId)
    throws DKException
    {
    DKPid pid = new DKPid();
    pid.setObjectType( indexClass );
    pid.setPrimaryId( itemId );
    ddo = new DKDDO( connection, pid );
    }

  /**
   * Constructor for a new Item with given DL connection.
   * 
   * @param connection          the given DL connection
   * @param indexClass          the given Index Class
   * @param itemType            the type for the new item
   * @exception DKException     Exceptions of CM
   **/
  public MCRCM7Item( DKDatastoreDL connection, String indexClass, short itemType)
    throws DKException
    {
    DKPid pid = new DKPid();
    pid.setObjectType( indexClass );
    ddo = new DKDDO( connection, pid );
    ddo.addProperty( DK_CM_PROPERTY_ITEM_TYPE, new Short( itemType ) );
    }

  /**
   * Constructor for a new Item as result fo a search.
   * 
   * @param condition           the condition for search to this item
   * @param indexClass          the given Index Class
   * @param connection          the given DL connection
   * @exception MCRPersistenceException Exceptions of MyCoRe
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public MCRCM7Item(String condition, String indexClass,
    DKDatastoreDL connection) throws DKException, MCRPersistenceException,
    Exception
    {
    DKNVPair parms[] = null;
    StringBuffer qs = new StringBuffer(128);
    qs.append("SEARCH=(INDEX_CLASS=").append(indexClass);
    if ((condition == null) || (condition.length() == 0)) {
      qs.append(')'); }
    else {
      qs.append(", COND=(").append(condition).append("))"); }
    dkQuery query = connection.createQuery(qs.toString(),DK_PARAMETRIC_QL_TYPE,
      parms);
    query.execute(parms);
    dkIterator iter = ((DKResults) query.result()).createIterator();
    if (! iter.more()) throw new MCRPersistenceException(
      "There is no item in index class " + indexClass +
      " that matches the condition (" + condition + ")" );
    this.ddo = ( DKDDO ) iter.next();
    }

  /**
   * This methode returns a string with the IndexClass name.
   *
   * @return the IndexClass name
   * @exception DKException     Exceptions of CM
   **/
  public String getIndexClass()
    throws DKException
    { return ddo.getPid().getObjectType().trim(); }

  /**
   * This methode returns a connection to the DL.
   *
   * @return a connection to the DL
   * @exception DKException     Exceptions of CM
   **/
  public DKDatastoreDL getConnection()
    throws DKException
    { return (DKDatastoreDL)( ddo.getDatastore() ); }

  /**
   * This methode returns the item ID.
   *
   * @return the item ID
   * @exception DKException     Exceptions of CM
   **/
  public String getItemId()
    throws DKException
    { return ddo.getPid().getPrimaryId(); }

  /**
   * This methode creates the item in the datastore.
   *
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void create() throws DKException, Exception
    { ddo.add(); } 

  /**
   * This methode retrievs the item from the datastore.
   *
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void retrieve() throws DKException, Exception
    { ddo.retrieve(); }

  /**
   * This methode updates the item in the datastore.
   *
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void update() throws DKException, Exception
    { ddo.update(); }

  /**
   * This methode deletes the item from the datastore.
   *
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void delete() throws DKException, Exception
    { ddo.del(); }

  /**
   * This methode reads the properties in this item.
   *
   * @exception Exception       Exceptions of JDK
   * @exception DKException     Exceptions of CM
   **/
  public final void readProperties()
    throws DKException, Exception
    {
    String         indexClass = getIndexClass();
    DKSequentialCollection c = (DKSequentialCollection)
        getConnection().listEntityAttrs( indexClass );
    dkIterator i = c.createIterator();
    for (int j = 0; j < c.cardinality(); j++) {
      DKAttrDefDL attribDef = (DKAttrDefDL) i.next();
      String      key       = indexClass + "||" + attribDef.getName();
      properties.put( key, attribDef ); }
    }

  /**
   * This methode gets a value from a object to this item.
   *
   * @param name                the name of the object
   * @return an object
   * @exception DKException     Exceptions of CM
   **/
  public final Object getValue(String name) throws DKException
    {
    short dataId = ddo.dataId( name );
    if ((dataId == 0) || ddo.isNull(dataId) || ! ddo.isDataSet(dataId))
      return null;
    else
      return ddo.getData(dataId);
    }

  /**
   * This methode sets a value from a object to this item.
   *
   * @param name                the name of the object
   * @param value               the value of the object
   * @exception Exception       Exceptions of JDK
   * @exception DKException     Exceptions of CM
   **/
  public final void setValue(String name, Object value)
    throws DKException, Exception
    {
    String key = getIndexClass() + "||" + name;
    if ( ! properties.containsKey(key)) readProperties();
    if ( ! properties.containsKey(key)) return;
    DKAttrDefDL attribDef = (DKAttrDefDL)(properties.get(key));
    short dataId = ddo.dataId(name);
    if (dataId == 0) {
      dataId = ddo.addData(name);
      ddo.addDataProperty(dataId,DK_PROPERTY_TYPE,
        new Short(attribDef.getType()));
      ddo.addDataProperty(dataId,DK_PROPERTY_NULLABLE,
        new Boolean(attribDef.isNullable())); }
    if (value == null) {
      ddo.setNull( dataId ); }
    else {
      if (value instanceof String) {
        String s = (String)value;
        if (s.length() > attribDef.getSize())
          value = s.substring(0,attribDef.getSize()-3) + "...";
        }
      ddo.setData(dataId, value);
      }
    }

  /**
   * This method gets a string value for the named keyfield.
   *
   * @param keyfield            the keyfield name string
   * @return the coresponding string value for the keyfield
   * @exception DKException     Exceptions of CM
   **/
  public final String getKeyfieldToString(String keyfield)
    throws DKException
    {
    String value = (String)getValue(keyfield);
    if ( value != null ) value = value.trim();
    return value;
    }

  /**
   * This method gets an integer value for the named keyfield.
   *
   * @param keyfield            the keyfield name string
   * @return the coresponding integer value for the keyfield
   * @exception DKException     Exceptions of CM
   **/
  public final int getKeyfieldToInt(String aKeyfield)
    throws DKException
    { return ((Number)getValue(aKeyfield)).intValue(); }

  /**
   * This method gets a boolean value for the named keyfield.
   *
   * @param keyfield            the keyfield name string
   * @return the coresponding boolean value for the keyfield
   * @exception DKException     Exceptions of CM
   **/
  public final boolean getKeyfieldToBoolean(String keyfield)
    throws DKException
    { return ((String)getValue(keyfield)).startsWith("t"); }

  /**
   * This method gets a date value for the named keyfield.
   *
   * @param keyfield            the keyfield name string
   * @return the coresponding date value for the keyfield
   * @exception DKException     Exceptions of CM
   **/
  public final GregorianCalendar getKeyfieldToDate(String aKeyfield)
    throws DKException
    {
    DKDate date = (DKDate)getValue( aKeyfield );
    if ( date == null ) return null;
    GregorianCalendar value = new GregorianCalendar(
      date.getYear(), date.getMonth(), date.getDate() );
    return value;
    }

  /**
   * This method increment or decrement a named keyfield.
   *
   * @param keyfield            the keyfield name string
   * @param delta               the increment/decrement value
   *
   * @return the coresponding date value for the keyfield
   * @exception Exception       Exceptions of JDK
   * @exception DKException     Exceptions of CM
   **/
  public final int changeIntKeyfield(String aKeyfield, int delta)
    throws DKException, Exception
    {
    int value = getKeyfieldToInt( aKeyfield );
    value += delta;
    setKeyfield( aKeyfield, value );
    return value;
    }

  /**
   * This method sets a string value for the named keyfield.
   * 
   * @param keyfield            the keyfield name string
   * @param value               the value for this keyfield
   * @exception Exception       Exceptions of JDK
   * @exception DKException     Exceptions of CM
   **/
  public final void setKeyfield(String keyfield, String value)
    throws DKException, Exception
    {
    if (value != null) {
      value = value.trim(); if ( value.length() == 0 ) value = null; }
    setValue(keyfield, value);
    }

  /**
   * This method sets a integer value for the named keyfield.
   * 
   * @param keyfield            the keyfield name string
   * @param value               the value for this keyfield
   * @exception Exception       Exceptions of JDK
   * @exception DKException     Exceptions of CM
   **/
  public final void setKeyfield(String keyfield, int value)
    throws DKException, Exception
    { setValue(keyfield, new Integer(value)); }

  /**
   * This method sets a long value for the named keyfield.
   * 
   * @param keyfield            the keyfield name string
   * @param value               the value for this keyfield
   * @exception Exception       Exceptions of JDK
   * @exception DKException     Exceptions of CM
   **/
  public final void setKeyfield(String keyfield, long value)
    throws DKException, Exception
    { setValue(keyfield, new Long(value)); }

  /**
   * This method sets a boolean value for the named keyfield.
   * 
   * @param keyfield            the keyfield name string
   * @param value               the value for this keyfield
   * @exception Exception       Exceptions of JDK
   * @exception DKException     Exceptions of CM
   **/
  public final void setKeyfield(String keyfield, boolean value)
    throws DKException, Exception
    { setValue( keyfield, new Boolean(value).toString().substring(0,1)); }

  /**
   * This method sets a calendar value for the named keyfield.
   * 
   * @param keyfield            the keyfield name string
   * @param value               the value for this keyfield
   * @exception Exception       Exceptions of JDK
   * @exception DKException     Exceptions of CM
   **/
  public final void setKeyfield(String aKeyfield, GregorianCalendar value)
    throws DKException, Exception
    {
    DKDate date = null;
    if (value != null) {
      date = new DKDate( value.get( GregorianCalendar.YEAR            ),
                         value.get( GregorianCalendar.MONTH           ),
                         value.get( GregorianCalendar.DAY_OF_MONTH )  ); }
    setValue(aKeyfield, date);
    }

  /**
   * This method returns an instance of a BLOB.
   * 
   * @param partId              the ID of the part 
   * @return a DKBlobDL
   * @exception DKException     Exceptions of CM
   **/
  public DKBlobDL getBlob(int partId)
    throws DKException
    {
    DKParts parts = (DKParts)getValue(DKPARTS);
    if (parts == null) return null;
    DKBlobDL part = null;
    int      id   = -1;
    dkIterator iter = parts.createIterator();
    while(iter.more()) {
      part = (DKBlobDL) iter.next();
      id   = ((DKPidXDODL)(part.getPidObject())).getPartId();
      if( id == partId ) { return part; }
      }
    return null;
    }

  /**
   * This method returns an instance of a Part.
   * 
   * @param partId              the ID of the part 
   * @return a string
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final String getPart(int partId) throws DKException, Exception
    {
    DKBlobDL part = getBlob(partId);
    if(part == null) { return null; }
    String content = new String(part.getContent());
    if ( content.equals( "--null--" ) )
      return null;
    else
      return content;
    }

  /**
   * This method returns an instance of a Part as byte array.
   * 
   * @param partId              the ID of the part 
   * @return a array of byte
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final byte[] getPartToBytes( int partId )
    throws Exception
    {
    DKBlobDL part = getBlob( partId );
    if(part == null) { return null; }
    byte[] content = part.getContent();
    if( ( content.length == 16 ) &&
        ( new String( content ).equals( "--null--" ) ) )
      return null;
    else
      return content;
    }

  /**
   * This method returns the length of an instance of a Part as byte array.
   * 
   * @param partId              the ID of the part 
   * @param buffer              an array of bytes
   * @return the length of the array
   * @exception Exception       Exceptions of JDK
   **/
  public final int getPart( int partId, byte[] buffer )
    throws Exception
    {
    byte[] content = getPartToBytes( partId );
    if( content == null )
      return 0;
    else {
      System.arraycopy( content, 0, buffer, 0, content.length );
      return content.length; }
    }

  /**
   * This method add a part to a part vector.
   * 
   * @param part                the part 
   * @exception Exception       Exceptions of JDK
   **/
  protected final  void addPartToParts( DKBlobDL part )
    throws Exception
    {
    DKParts parts = (DKParts)getValue( DKPARTS );
    if ( parts == null ) {
      parts = new DKParts();
      short data_id = ddo.addData( DKPARTS );
      ddo.addDataProperty( data_id, DK_CM_PROPERTY_TYPE, 
        new Short( DK_CM_COLLECTION_XDO ) );
      ddo.addDataProperty( data_id, DK_CM_PROPERTY_NULLABLE, 
        new Boolean( true ) );
      ddo.setData( data_id, parts );
      }
    parts.addElement( part );
    }

  /**
   * Set a part of this item.
   *
   * @param partId              the ID of this part
   * @param buffer              the stream buffer of this part
   * @param bytes               the size of the buffer
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void setPart( int partId, byte[] buffer, int bytes )
    throws DKException, Exception
    {
    byte[] content;
    if( bytes == buffer.length )
      content = buffer;
    else {
      content = new byte[ bytes ];
      System.arraycopy( buffer, 0, content, 0, bytes ); }

    DKBlobDL part = getBlob( partId );
    if ( part == null ) {
      if ( bytes == 0 ) return;
      part = new DKBlobDL( getConnection() );
      DKPidXDODL pid  = new DKPidXDODL();
      pid.setPartId( partId );
      part.setPidObject     ( pid             );
      part.setContentClass  ( DK_CC_UNKNOWN   );
      part.setAffiliatedType( DK_BASE         );
      part.setContent       ( content         );
      addPartToParts(part);
      }
    else {
      if ( bytes > 0 )
        part.setContent( content );
      else
        part.setContent( "--null--".getBytes() );
      }
    }

  /**
   * Set a part of this item.
   *
   * @param partId              the ID of this part
   * @param text                the text of this part
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void setPart( int partId, String text )
    throws DKException, Exception
    {
    DKBlobDL part = getBlob( partId );
    if ( part == null ) {
      if ( text == null ) return;
      part = new DKBlobDL( getConnection() );
      DKPidXDODL pid  = new DKPidXDODL();
      pid.setPartId( partId );
      part.setPidObject     ( pid             );
      part.setContentClass  ( DK_CC_ASCII     );
      part.setAffiliatedType( DK_BASE         );
      part.setContent       ( text.getBytes() );
      addPartToParts(part);
      }
    else {
      if ( text != null )
        part.setContent( text.getBytes() );
      else
        part.setContent( "--null--".getBytes() );
      }
    }

  /**
   * Set a part of this item with TextSearch integration.
   *
   * @param partId              the ID of this part
   * @param text                the text of this part
   * @param textSearchServer    the TextSearch Server name
   * @param textSearchIndex     the TextSearch Index name
   * @param textSearchInfo      the TextSearch Information string
   * @exception DKException     Exceptions of CM
   * @exception Exception       Exceptions of JDK
   **/
  public final void setPart(int partId, String text, String textSearchServer,
    String textSearchIndex, String textSearchInfo)
    throws DKException, Exception
    {
    setPart(partId,text);
    DKBlobDL part = getBlob(partId);
    if ( part != null ) {
      DKSearchEngineInfoDL sei = new DKSearchEngineInfoDL();
      sei.setSearchEngine("SM");
      sei.setSearchIndex (textSearchServer+"-"+textSearchIndex);
      sei.setSearchInfo  (textSearchInfo);
      part.setExtension("DKSearchEngineInfoDL",(dkExtension)sei);
      }
    }

  }

