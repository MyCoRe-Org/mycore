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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
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

public class MCRClassification extends MCRClassificationObject
{
  public MCRClassification( String ID, String label ) 
  { this( ID, label, true ); }
  
  MCRClassification( String ID, String label, boolean create )
  { 
    super( ID, label ); 
    if( create ) manager().createClassification( this );  
  }
  
  public void setLabel(String label)
  {
    super.setLabel( label );
    manager().updateClassification( this );
  }
  
  protected String getClassificationID()
  { return ID; }
  
  public MCRCategory getCategory( String categID )
  { 
    ensureNotDeleted();
    MCRArgumentChecker.ensureNotEmpty( categID, "categID" );  
    
    return MCRCategory.getCategory( this.ID, categID );
  }  
  
  public void delete()
  {
    super.delete();
    manager().deleteClassification( ID );
  }
  
  public static MCRClassification getClassification( String ID )
  {
    MCRArgumentChecker.ensureNotEmpty( ID, "ID" );  
    return manager().retrieveClassification( ID );
  }
}
