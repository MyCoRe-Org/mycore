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

package mycore.editor;

import java.util.*;
import mycore.common.*;

/**
 * Represents a variable that is read from/to an XML document
 * and edited with EditorServlet in HTML forms.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 **/
class MCREditorVariable
{
  String   path;
  String   value;
  String   attribute;
  String[] pathElements;

  MCREditorVariable( String path, String value )
  {
    setPath ( path  );
    setValue( value );
  }
    
  void setPath( String path )
  {
    this.path      = path;
    this.attribute = null;
      
    ArrayList elements = new ArrayList();
    for( StringTokenizer st = new StringTokenizer( path, "/" ); st.hasMoreTokens(); )
    {
      String token = st.nextToken();
      if( ( ! st.hasMoreTokens() ) && ( token.startsWith( "@" ) ) )
        attribute = token.substring( 1 );
      else
        elements.add( token ); 
    }
    pathElements = ( String[] )( elements.toArray( new String[ 0 ] ) );
  }
    
  String getPath()
  { return path; }
    
  void setValue( String value )
  { this.value = value; }
    
  String getValue()
  { return value; }
    
  String[] getPathElements()
  { return pathElements; }
    
  String getAttributeName()
  { return attribute; }
}

