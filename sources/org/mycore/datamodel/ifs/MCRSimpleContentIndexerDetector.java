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

package org.mycore.datamodel.ifs;

/** 
 * A simple implementation of an MCRFileContentTypeDetector, detects
 * the file type based on the filename extension and a magic bytes
 * pattern at some offset in the header of the file's content.
 * The rules for detecting each file type are embedded in the
 * &lt;rules&gt; element of the file content types 
 * definition XML file.
 *
 * @see MCRFileContentTypeDetector
 * @see MCRFileContentType
 * @see MCRFileContentTypeFactory
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRSimpleContentIndexerDetector implements MCRContentIndexerDetector
{
  /** Creates a new detector */
  public MCRSimpleContentIndexerDetector(){}
  
//  private static Logger logger = Logger.getLogger( MCRSimpleIndexerDetector.class );
  
  /**
   * Adds a detection rule from the file content type definition XML file.
   * The detector parses the &lt;rules&gt; element provided
   * with each content type in the file content types XML definition.
   *
   * @param type the file content type the rule is for
   * @param rules the rules XML element containing the rules for detecting that type
   */
   public String getIndexer( String fct )
  {
    if ( fct.equals( "tablename" ) )
      return fct;
    else
      return null;
  }
  

}
