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

package org.mycore.frontend.editor2;

import org.mycore.common.*;

import org.apache.log4j.*;
import org.apache.commons.fileupload.*;

import org.jdom.Element;

import java.util.*;
import java.text.*;

/**
 * A single variable that was submitted from a MyCoRe
 * XML editor form.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 **/
public class MCREditorVariable implements Comparable
{
  protected final static Logger logger = Logger.getLogger(  MCREditorServlet.class );

  String   name;
  String   value;
  String   sortPattern;
  String   attributeName;
  String[] pathElements;
  Element  component;
  String   autofill;
  FileItem file;
  
  MCREditorVariable( String name, String value, String sortNr, FileItem file, Element component )
  {
    this.name      = name;
    this.value     = value;
    this.component = component;
    this.file      = file;

    if( component != null )
    {
      String attrib = component.getAttributeValue( "autofill" );
      String elem   = component.getChildTextTrim( "autofill" );

      if( ( attrib != null ) && ( attrib.trim().length() > 0 ) )
        this.autofill = attrib.trim();
      else if( ( attrib != null ) && ( attrib.trim().length() > 0 ) )
        this.autofill = elem.trim();
    }

    buildSortPattern( sortNr );
    buildPathElements( name );
  }
  
  public String getName()
  { return name; }
  
  public String getValue()
  { return value; }
  
  public FileItem getFile()
  { return file; }
  
  public int compareTo( Object o )
  {
    MCREditorVariable other = (MCREditorVariable)o;

    int length = Math.min( other.sortPattern.length(), this.sortPattern.length() );
    String spo = other.sortPattern.substring( 0, length );
    String spt = this .sortPattern.substring( 0, length );
    return spt.compareTo( spo );
  }
  
  private static DecimalFormat df = new DecimalFormat( "0000" );

  private void buildSortPattern( String sortNr )
  {
    StringTokenizer st = new StringTokenizer( sortNr, "." );
    StringBuffer sb = new StringBuffer();
    while( st.hasMoreTokens() )
    {
      int number = Integer.parseInt( st.nextToken() );
      sb.append( df.format( number ) );
    }
    this.sortPattern = sb.toString();
  }

  private void buildPathElements( String path )
  {
    this.attributeName = null;
      
    ArrayList elements = new ArrayList();
    for( StringTokenizer st = new StringTokenizer( path, "/" ); st.hasMoreTokens(); )
    {
      String token = st.nextToken();
      if( ( ! st.hasMoreTokens() ) && ( token.startsWith( "@" ) ) )
        attributeName = token.substring( 1 );
      else
        elements.add( token ); 
    }
    pathElements = ( String[] )( elements.toArray( new String[ 0 ] ) );
  }
}

