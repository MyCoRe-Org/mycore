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

package mycore.ifs;

import mycore.common.*;
import org.jdom.*;
import java.util.*;

/** 
 * A simple implementation of an MCRFileContentTypeDetector.
 *
 * @see MCRFileContentTypeDetector
 * @see MCRFileContentType
 * @see MCRFileContentTypeFactory
 *
 * @author Frank Lützenkirchen
 */
public class MCRSimpleFCTDetector implements MCRFileContentTypeDetector
{
  private List rulesList = new Vector();
  
  public MCRSimpleFCTDetector(){}
  
  public void addRule( MCRFileContentType type, Element rules )
  {
    MCRDetectionRule rule = new MCRDetectionRule( type );
    rulesList.add( rule );
          
    try
    {
      List extensions = rules.getChildren( "extension" );
      
      for( int i = 0; i < extensions.size(); i++ )
      {
        Element elem  = (Element)( extensions.get( i ) );
        
        double  score = elem.getAttribute( "score" ).getDoubleValue();
        String  ext   = elem.getTextTrim();
        
        rule.addExtensionRule( ext, score );
      }
    }
    catch( Exception exc )
    {
      String msg = "Error parsing detection rules for file content type " + type.getLabel();
      throw new MCRConfigurationException( msg, exc );
    }
  }
  
  public MCRFileContentType detectType( String filename, byte[] header )
  { 
    double maxScore = 0.0;
    MCRFileContentType detected = null;
    
    for( int i = 0; ( i < rulesList.size() ) && ( maxScore < 1.0 ) ; i++ )
    {
      MCRDetectionRule rule = (MCRDetectionRule)( rulesList.get( i ) );
      double score = rule.getTotalScore( filename );
      
      if( score > maxScore )
      {
        maxScore = score;
        detected = rule.getFileContentType();
      }
    }
    
    return detected;
  }
  
  class MCRDetectionRule
  {
    private MCRFileContentType type;
    private Hashtable extensions;
    
    MCRDetectionRule( MCRFileContentType type )
    { this.type = type; }
    
    void addExtensionRule( String extension, double score )
    {  extensions.put( extension, new Double( score ) ); }
    
    MCRFileContentType getFileContentType()
    { return type; }
    
    double getTotalScore( String filename )
    {
      Enumeration exts = extensions.keys();
      double maxScore = 0.0;
      
      while( exts.hasMoreElements() && ( maxScore < 0 ) )
      {
        String ext = (String)( exts.nextElement() );
        if( filename.endsWith( "." + ext ) )
        {
          double score = ( (Double)( extensions.get( ext ) ) ).doubleValue();
          maxScore = Math.max( maxScore, score );
        }
      }
      
      return maxScore;
    }
  }
}
