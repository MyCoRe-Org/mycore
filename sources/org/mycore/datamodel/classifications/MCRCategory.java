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

public class MCRCategory extends MCRClassificationObject
{
  protected String parentID;
  protected String classifID;
    
  public MCRCategory( MCRClassificationObject parent, String ID, String label ) 
  {
    super( ID, label );
    
    MCRArgumentChecker.ensureNotNull( parent, "parent" );  
    this.classifID = parent.getClassificationID();
    MCRArgumentChecker.ensureIsFalse( ID.equals( classifID ),
      "A category ID can not be the same as its classification ID" );
    if( parent instanceof MCRCategory) this.parentID  = parent.ID;
    
    manager().createCategory( this );  
    
    if( parent.childrenIDs != null ) parent.childrenIDs = null;
  }
  
  MCRCategory( String ID, String label, String classifID, String parentID )
  {
    super( ID, label );
    this.classifID = classifID;
    this.parentID  = parentID;
  }

  public void setLabel(String label)
  {
    super.setLabel( label );
    manager().updateCategory( this );
  }
  
  protected String getClassificationID()
  { return classifID; }
  
  public MCRClassification getClassification()
  { 
    ensureNotDeleted();
    return MCRClassification.getClassification( classifID ); 
  }
  
  public MCRCategory getParent()
  { 
    ensureNotDeleted();
    
    if( parentID != null )
      return getCategory( classifID, parentID );
    else 
      return null;
  }
  
  public String getParentID()
  { 
    ensureNotDeleted();
    return parentID;
  }
  
  public void delete()
  {
    MCRClassificationObject parent = getParent();
    if( parent == null ) parent = getClassification();
    
    parent.childrenIDs = null; 
    
    super.delete();
    
    manager().deleteCategory( classifID, ID );
  }
  
  public static MCRCategory getCategory( String classifID, String categID )
  {
    MCRArgumentChecker.ensureNotEmpty( classifID, "classifID" );  
    MCRArgumentChecker.ensureNotEmpty( categID,   "categID"   );  
    
    return manager().retrieveCategory( classifID, categID );
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer( super.toString() ).append( "\n" );
    sb.append( "Classification: " ).append( classifID ).append( "\n" );
    sb.append( "Parent ID:      " ).append( parentID  );
    return sb.toString();
  }
}
