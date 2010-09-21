/*
 * $Revision$ 
 * $Date$
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
 */

package org.mycore.oai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryManager;

/**
 * Implements the ListSets request.
 * 
 * @author Frank L\u00fctzenkirchen
 */
class MCRListSetsHandler extends MCRVerbHandler
{
  final static String VERB = "ListSets";
  
  void setAllowedParameters( Properties p )
  {
    p.setProperty( ARG_RESUMPTION_TOKEN, V_EXCLUSIVE );
  }
  
  MCRListSetsHandler( MCROAIDataProvider provider )
  {
    super( provider );
  }

  void handleRequest()
  {
    String resumptionToken = parms.getProperty( ARG_RESUMPTION_TOKEN );
    if( resumptionToken != null )
    {
      addError( ERROR_BAD_RESUMPTION_TOKEN, "Bad resumption token: " + resumptionToken );
      return;
    }
    
    if( setURIs.isEmpty() )
    {
      addError( ERROR_NO_SET_HIERARCHY, "This repository does not provide sets" );
      return;
    }
    
    Set<String> setSpecs = new HashSet<String>();
    List<Element> sets = new ArrayList<Element>();
    for( String uri : setURIs )
    {
      Element resolved = MCRURIResolver.instance().resolve( uri );
      for( Element set : (List<Element>)( resolved.getChildren( "set", NS_OAI ) ) )
      {
        String setSpec = set.getChildText( "setSpec", NS_OAI );
        if( ! setSpecs.contains( setSpec ) ) sets.add( (Element)( set.clone() ) );
      }
    }

    // Filter out empty sets
    if( MCRConfiguration.instance().getBoolean( provider.getPrefix() + "FilterEmptySets", true ) )
    {
      setSpecs.clear();
      for( Iterator<Element> it = sets.iterator(); it.hasNext(); )
      {
        Element set = it.next();
        String setSpec = set.getChildText( "setSpec", NS_OAI );
  
        // Check parent set, if existing
        if( setSpec.contains( ":" ) && ( setSpec.lastIndexOf( ":" ) > setSpec.indexOf( ":" ) ) )
        {
          String parentSetSpec = setSpec.substring( 0, setSpec.lastIndexOf( ":" ) );
          // If parent set is empty, all child sets must be empty, too
          if( ! setSpecs.contains( parentSetSpec ) ) 
          {
            it.remove();
            continue;
          }
        }
        
        // Build a query to count results
        MCRAndCondition query = new MCRAndCondition();
        query.addChild( provider.getAdapter().buildSetCondition( setSpec ) );
        if( restriction != null ) query.addChild( restriction );      
        
        if( MCRQueryManager.search( new MCRQuery( query ) ).getNumHits() == 0 )
          it.remove();
        else
          setSpecs.add( setSpec );
      }
    }
    
    output.addContent( sets );
  }
}
