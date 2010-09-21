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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.jdom.Element;
import org.mycore.common.MCRConfiguration;

/**
 * MCRMetadataFormat represents a metadata format with its prefix, namespace and schema.
 * 
 * OAI-PMH supports the dissemination of records in multiple metadata formats from a repository.
 * For purposes of interoperability, repositories must disseminate Dublin Core, without any qualification. 
 * Therefore, the protocol reserves the metadataPrefix 'oai_dc', and the URL of a metadata schema for 
 * unqualified Dublin Core, which is http://www.openarchives.org/OAI/2.0/oai_dc.xsd. 
 * The corresponding XML namespace URI is http://www.openarchives.org/OAI/2.0/oai_dc/. 
 * 
 * Metadata formats are configured in mycore.properties, for example
 *  
 * MCR.OAIDataProvider.MetadataFormat.oai_dc.Schema=http://www.openarchives.org/OAI/2.0/oai_dc.xsd
 * MCR.OAIDataProvider.MetadataFormat.oai_dc.Namespace=http://www.openarchives.org/OAI/2.0/oai_dc/
 *
 * @author Frank L\u00fctzenkirchen
 */
class MCRMetadataFormat implements MCROAIConstants
{
  /** The metadata prefix */
  private String prefix;
  
  /** The XML schema for this kind of metadata */
  private String schema;
  
  /** The namespace for this kind of metadata */
  private String namespace;
  
  /**
   * Private constructor used in initialization
   */
  private MCRMetadataFormat( String prefix, String schema, String namespace )
  {
    this.prefix = prefix;
    this.schema = schema;
    this.namespace = namespace;
  }
  
  /**
   * Returns the unique metadata prefix, a string to specify the metadata format in OAI-PMH requests 
   * issued to the repository. The prefix consists of any valid URI unreserved characters. 
   * metadataPrefix arguments are used in ListRecords, ListIdentifiers, and GetRecord requests to retrieve records, 
   * or the headers of records that include metadata in the format specified by the metadataPrefix 
   */
  public String getPrefix()
  {
    return prefix;
  }
  
  public boolean equals( Object obj )
  {
    if( super.equals( obj ) ) 
      return true;
    else if( obj == null )
      return false;
    else if( ! ( obj instanceof MCRMetadataFormat ) )
      return false;
    else 
      return ((MCRMetadataFormat)obj).namespace.equals( this.namespace );
  }

  public int hashCode()
  {
    return namespace.hashCode();
  }

  /**
   * Builds an xml representation of this metadata format, as returned by the ListMetadataFormats request.
   */
  Element buildXML()
  {
    Element metadataFormat = new Element( "metadataFormat", NS_OAI );
    metadataFormat.addContent( new Element( "metadataPrefix"   , NS_OAI ).setText( prefix    ) );
    metadataFormat.addContent( new Element( "schema"           , NS_OAI ).setText( schema    ) );
    metadataFormat.addContent( new Element( "metadataNamespace", NS_OAI ).setText( namespace ) );
    return metadataFormat;
  }
  
  /**
   * Maps a metadata prefix to its MCRMetadataFormat instance
   */
  private static Map<String,MCRMetadataFormat> map;

  /**
   * Returns the metadata format defined for the given prefix.
   */
  static MCRMetadataFormat getFormat( String prefix )
  {
    return map.get( prefix );
  }

  static
  {
    map = new HashMap<String,MCRMetadataFormat>();
    
    String pre ="MCR.OAIDataProvider.MetadataFormat."; 
    
    MCRConfiguration config = MCRConfiguration.instance();
    Properties formats = config.getProperties( pre );
    
    for( Iterator it = formats.keySet().iterator(); it.hasNext(); )
    {
      String key = (String)( it.next() );
      if( ! key.endsWith( ".Schema" ) ) continue;
      
      String prefix = key.substring( pre.length(), key.indexOf( ".Schema" ) );
      String schema = config.getString( key );
      String ns     = config.getString( pre + prefix + ".Namespace" );
      
      MCRMetadataFormat format = new MCRMetadataFormat( prefix, schema, ns );
      map.put( prefix, format );
    }
  }
}
