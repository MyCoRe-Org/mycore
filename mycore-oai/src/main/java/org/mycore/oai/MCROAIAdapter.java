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

import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.parsers.bool.MCRCondition;

/**
 * @author Frank L\u00fctzenkirchen
 */
public abstract class MCROAIAdapter implements MCROAIConstants
{
  protected final static Logger LOGGER = Logger.getLogger( MCRVerbHandler.class );
  
  protected String recordUriPattern;
  protected String headerUriPattern;
  
  void init( String prefix )
  { 
    recordUriPattern = MCRConfiguration.instance().getString( prefix + "RecordURIPattern" );
    headerUriPattern = MCRConfiguration.instance().getString( prefix + "HeaderURIPattern" );
  }
  
  public abstract boolean exists( String identifier );
  
  public List<MCRMetadataFormat> listMetadataFormats( String id, List<MCRMetadataFormat> defaults )
  { return defaults; }
  
  public Element getRecord( String id, MCRMetadataFormat format )
  {
    String uri = recordUriPattern.replace( "{id}", id ).replace( "{format}", format.getPrefix() );
    return getURI( uri );
  }

  public Element getHeader( String id, MCRMetadataFormat format )
  {
    String uri = headerUriPattern.replace( "{id}", id ).replace( "{format}", format.getPrefix() );
    return getURI( uri );
  }
  
  public abstract MCRCondition buildSetCondition( String setSpec );
  
  protected Element getURI( String uri )
  {
    LOGGER.debug( "get " + uri );
    return (Element)( MCRURIResolver.instance().resolve( uri ).detach() );
  }
}