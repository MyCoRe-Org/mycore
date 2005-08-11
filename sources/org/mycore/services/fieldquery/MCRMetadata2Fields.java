package org.mycore.services.fieldquery;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.apache.log4j.Logger;
import org.jdom.JDOMException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Transforms XML metadata to search field values for indexing searchable data.
 * Reads search field configuration from file searchfields-[TYPE].xml. This file
 * and the result of the transformation are described by the schema searchfields.xsd.   
 * 
 * @author Frank Lützenkirchen
 **/
public class MCRMetadata2Fields 
{
  /** The logger **/
  private static final Logger LOGGER = Logger.getLogger(MCRMetadata2Fields.class);

  /**
   * Transforms XML stored in a MCRFile to search field values.
   * 
   * @param file the MCRFile, must contain xml document
   * @return a List of JDOM field elements with values, see searchfields.xsd for schema
   * @throws IOException if MCRFile content could not be read
   * @throws JDOMException if MCRFile xml content could not be parsed 
   **/
  public static List buildFields( MCRFile file )
    throws IOException, JDOMException
  { return buildFields( file.getContentAsJDOM(), file.getContentTypeID() ); }
    
  /**
   * Transforms XML metadata of MCRObject to search field values.
   * 
   * @param obj the MCRObject
   * @return a List of JDOM field elements with values, see searchfields.xsd for schema
   **/
  public static List buildFields( MCRObject obj )
  { return buildFields( obj.createXML(), obj.getId().getTypeId() ); }

  /**
   * Transforms XML data in a JDOM document to search field values.
   * 
   * @param input the XML document
   * @param type the type of metadata which determines the configuration file searchfields-[TYPE].xml that will be used
   * @return a List of JDOM field elements with values, see searchfields.xsd for schema
   **/
  public static List buildFields( Document input, String type )
  {
    String uri = "resource:searchfields-" + type + ".xml";
    Element def = MCRURIResolver.instance().resolve( uri );
    Document xsl = buildStylesheet( type, def );

    try
    {
      JDOMSource xslsrc = new JDOMSource( xsl );
      JDOMSource xmlsrc = new JDOMSource( input );
      JDOMResult xmlres = new JDOMResult();
      TransformerFactory factory = TransformerFactory.newInstance();                                                                                                             
      Transformer transformer = factory.newTransformer( xslsrc );
      transformer.transform( xmlsrc, xmlres );
      List resultList = xmlres.getResult();
      Element root = (Element)( resultList.get( 0 ) );
      
      if( LOGGER.isDebugEnabled() )
      {
        LOGGER.debug( "---------- search fields ---------" );
        XMLOutputter out = new XMLOutputter( org.jdom.output.Format.getPrettyFormat() );
        LOGGER.debug( out.outputString( root.getChildren() ) );
        LOGGER.debug( "----------------------------------" );
      }
            
      return root.getChildren();
    }
    catch( Exception ex )
    {
        String msg = "Exception while transforming metadata to search fields";
        throw new MCRException( msg, ex );
    }
  }
  
  /** Cached stylesheets for metadata to searchfield value transformation **/
  private static MCRCache stylesheets = new MCRCache( 20 );
  
  /** Builds stylesheet to transform a given type of metadata using a given searchfields definition **/
  private static synchronized Document buildStylesheet( String type, Element definition )
  {
    Document xsl = (Document)( stylesheets.get( type ) );
    if( xsl == null )
    {  
      Namespace xslns = Namespace.getNamespace( "xsl", "http://www.w3.org/1999/XSL/Transform" );
      Namespace mcrns = Namespace.getNamespace( "mcr", "http://www.mycore.org/" );

      Element stylesheet = new Element( "stylesheet" );
      stylesheet.setAttribute( "version", "1.0" );
      stylesheet.setNamespace( xslns );
      xsl = new Document( stylesheet );
      
      Element template = new Element( "template", xslns );
      template.setAttribute( "match", "/" );
      stylesheet.addContent( template );
      
      Element searchfields = new Element( "searchfields", mcrns );
      template.addContent( searchfields );
      
      List fields = definition.getChildren( "field", mcrns );
      for( int i = 0; i < fields.size(); i++ )
      {
        Element field = (Element)( (Element)( fields.get( i ) ) ).clone();  
        Element forEach = new Element( "for-each", xslns );
        forEach.setAttribute( "select", field.getAttributeValue( "xpath" ) );
        field.removeAttribute( "xpath" );
        searchfields.addContent( forEach );
        forEach.addContent( field );
        field.setAttribute( "value", "{" + field.getAttributeValue( "value" ) + "}" );
        List attributes = field.getChildren( "attribute", mcrns );
        for( int j = 0; j < attributes.size(); j++ )
        {
          Element attribute = (Element)( attributes.get( j ) );
          attribute.setAttribute( "value", "{" + attribute.getAttributeValue( "value" ) + "}" );
        }
      }
      
      stylesheets.put( type, xsl );
      
      if( LOGGER.isDebugEnabled() )
      {
        LOGGER.debug( "---------- stylesheet to build search fields ---------" );
        XMLOutputter out = new XMLOutputter( org.jdom.output.Format.getPrettyFormat() );
        LOGGER.debug( out.outputString( xsl ) );
        LOGGER.debug( "------------------------------------------------------" );
      }
    }
    return xsl;
  }

  /**
   * Test application, reads a metadata xml file from local filesystem
   * and builds search fields from it. If log level is DEBUG, output will show
   * the stylesheet that was used and the search fields that have been generated.
   **/
  public static void main( String[] args )
    throws Exception
  {
      Document xml = new SAXBuilder().build( new File( "c:\\demo-document.xml" ) );
      buildFields( xml, "document" );
  }
}
