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

import mycore.common.*;
import java.util.Vector;

class MCRClassificationManager
{
  protected static MCRClassificationManager manager;
  
  protected static MCRClassificationManager instance()
  {
    if( manager == null ) manager = new MCRClassificationManager();
    return manager;
  }
  
  protected MCRCache categoryCache;
  protected MCRCache classificationCache;
  
  protected MCRClassificationStore store;

  protected MCRClassificationManager()
  {
    MCRConfiguration config = MCRConfiguration.instance();
    
    Object object = config.getInstanceOf( "MCR.classifications_store_class" );  
    store = (MCRClassificationStore)object;
    
    int classifSize = config.getInt( "MCR.classifications_classification_cache_size", 30 );
    int categSize   = config.getInt( "MCR.classifications_category_cache_size", 500 );
    
    classificationCache = new MCRCache( classifSize );
    categoryCache       = new MCRCache( categSize   );
  }
  
  void createClassification( MCRClassification classification )
  {
    if( store.classificationExists( classification.getID() ) )
      throw new MCRPersistenceException( "Classification already exists" );
    
    store.createClassification( classification );
    classificationCache.put( classification.getID(), classification );
  }

  void updateClassification( MCRClassification classification )
  {
    store.updateClassification( classification );
    
    classificationCache.remove( classification.getID() );
    classificationCache.put   ( classification.getID(), classification );
  }
  
  void createCategory( MCRCategory category )
  {
    if( store.categoryExists( category.getClassificationID(), category.getID() ) )
      throw new MCRPersistenceException( "Category already exists" );
    
    store.createCategory( category );
    categoryCache.put( getCachingID( category ), category );
  }
  
  void updateCategory( MCRCategory category )
  {
    store.updateCategory( category );
    
    categoryCache.remove( getCachingID( category ) );
    categoryCache.put   ( getCachingID( category ), category );
  }

  MCRClassification retrieveClassification( String ID )
  {
    MCRClassification c = (MCRClassification)( classificationCache.get( ID ) );
    if( c == null )
    {
      c = store.retrieveClassification( ID );
      if( c != null ) classificationCache.put( ID, c );
    }
    return c;
  }
  
  MCRCategory retrieveCategory( String classifID, String categID )
  {
    String cachingID = classifID + "@@" + categID;
    MCRCategory c = (MCRCategory)( categoryCache.get( cachingID ) );
    
    if( c == null )
    {
      c = store.retrieveCategory( classifID, categID );
      if( c != null ) categoryCache.put( cachingID, c );
    }
    return c;
  }
  
  MCRCategory[] retrieveChildren( String classifID, String parentID )
  {
    Vector retrieved = store.retrieveChildren( classifID, parentID );
    MCRCategory[] children = new MCRCategory[ retrieved.size() ];
    
    for( int i = 0; i < children.length; i++ )
    {
      MCRCategory cRetrieved = (MCRCategory)( retrieved.elementAt( i ) );
      String cachingID = getCachingID( cRetrieved );
      MCRCategory cFromCache = (MCRCategory)( categoryCache.get( cachingID ) );
      
      if( cFromCache != null )
        children[ i ] = cFromCache;
      else
      {
        children[ i ] = cRetrieved;
        categoryCache.put( cachingID, cRetrieved );
      }
    }
    return children;
  }
  
  int retrieveNumberOfChildren( String classifID, String parentID )
  { return store.retrieveNumberOfChildren( classifID, parentID ); }
  
  void deleteClassification( String classifID )
  {
    classificationCache.remove( classifID );
    store.deleteClassification( classifID );
  }
  
  void deleteCategory( String classifID, String categID )
  {
    categoryCache.remove( classifID + "@@" + categID );
    store.deleteCategory( classifID, categID );
  }
  
  protected String getCachingID( MCRCategory category )
  { return category.getClassificationID() + "@@" + category.getID(); }
}
