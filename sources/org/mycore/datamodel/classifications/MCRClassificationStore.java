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

import java.util.Vector;

public interface MCRClassificationStore 
{
  public void createClassification( MCRClassification classification );  

  public void updateClassification( MCRClassification classification );  
  
  public void createCategory( MCRCategory category );
    
  public void updateCategory( MCRCategory category );

  public MCRClassification retrieveClassification( String ID );
  
  public MCRCategory retrieveCategory( String classifID, String categID );
  
  public Vector retrieveChildren( String classifID, String parentID );
  
  public int retrieveNumberOfChildren( String classifID, String parentID );
  
  public boolean classificationExists( String classifID );
  
  public boolean categoryExists( String classifID, String categID );
  
  public void deleteClassification( String classifID );
  
  public void deleteCategory( String classifID, String categID );
}

