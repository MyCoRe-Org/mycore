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

import java.util.Properties;

class MCRGetRecordHandler extends MCRVerbHandler
{
  final static String VERB = "GetRecord";
  
  void setAllowedParameters( Properties p )
  {
    p.setProperty( ARG_IDENTIFIER,      V_REQUIRED );
    p.setProperty( ARG_METADATA_PREFIX, V_REQUIRED );
  }
  
  MCRGetRecordHandler( MCROAIDataProvider provider )
  { super( provider ); }
  
  void handleRequest()
  {
    String identifier      = parms.getProperty( ARG_IDENTIFIER );
    String metadata_prefix = parms.getProperty( ARG_METADATA_PREFIX );

    if( ! checkIdentifier( identifier ) ) return;
    
    MCRMetadataFormat format = MCRMetadataFormat.getFormat( metadata_prefix );
    if( format == null )
    {
      String msg = "This metadata format is not supported by the repository: " + metadata_prefix;
      addError( ERROR_CANNOT_DISSEMINATE_FORMAT, msg );
      return;
    }
    
    List<MCRMetadataFormat> formats = provider.getAdapter().listMetadataFormats( identifier, provider.getMetadataFormats() );
    if( ! formats.contains( format ) )
    {
      String msg = "The item " + identifier + " does not support the metadata format " + metadata_prefix;
      addError( ERROR_CANNOT_DISSEMINATE_FORMAT, msg );
      return;
    }
    
    String id = identifier.substring( identifier.lastIndexOf( ':' ) + 1 );
    output.addContent( provider.getAdapter().getRecord( id, format ) );   
  }
}
