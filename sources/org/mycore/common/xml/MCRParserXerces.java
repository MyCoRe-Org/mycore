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

package org.mycore.common.xml;

import java.io.*;
import org.apache.log4j.*;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * Implements the MCRParserInterface using the Xerces XML to parse XML streams
 * to a DOM document.
 *
 * @author Jens Kupferschmidt
 * @author Frank L�tzenkirchen
 * @author Thomas Scheffler (yagee)
 *
 * @version $Revision$ $Date$
 **/
public class MCRParserXerces 
  implements MCRParserInterface, ErrorHandler
{
  // the Xerces parser to be used
  SAXBuilder builderValid;
  SAXBuilder builder;

  // parser configuration flags 
  private static boolean flagvalidation        = false;
  private static boolean flagnamespaces        = true;
  private static boolean flagschemasupport     = true;
  private static boolean flagnoschemasupport   = false;
  private static boolean flagschemafullsupport = true;
  private static boolean flagnoschemafullsupport = false;
  private static boolean flagdeferreddom       = true;
  private static MCREntityResolver entityResolver;

  private static String setvalidation          =
    "http://xml.org/sax/features/validation";
  private static String setnamespaces          =
    "http://xml.org/sax/features/namespaces";
  private static String setschemasupport       =
    "http://apache.org/xml/features/validation/schema";
  private static String setschemafullsupport   =
    "http://apache.org/xml/features/validation/schema-full-checking";

  /**
   * Constructor for the xerces parser. Sets default validation flag as
   * specified by the property MCR.parser_schema_validation in mycore.properties
   **/
  public MCRParserXerces()
  {
    flagvalidation = MCRConfiguration.instance()
      .getBoolean( "MCR.parser_schema_validation", flagvalidation );
	builderValid=new SAXBuilder("org.apache.xerces.parsers.SAXParser",true);
	builder=new SAXBuilder("org.apache.xerces.parsers.SAXParser",false);
	entityResolver=new MCREntityResolver();

	builder.setFeature( setnamespaces,        flagnamespaces        );
	builder.setFeature( setschemasupport,     flagnoschemasupport    );
	builder.setFeature( setschemafullsupport, flagnoschemafullsupport );
	builderValid.setFeature( setnamespaces,        flagnamespaces        );
	builderValid.setFeature( setschemasupport,     flagschemasupport     );
	builderValid.setFeature( setschemafullsupport, flagnoschemafullsupport );

	builder.setReuseParser(true);
	builderValid.setReuseParser(true);
    
	builder.setErrorHandler  ( this );
	builderValid.setErrorHandler  ( this );

	builder.setEntityResolver( entityResolver );
	builderValid.setEntityResolver( entityResolver );
  }

  /**
   * Parses the XML byte stream with xerces parser and 
   * returns a DOM document. Uses the validation flag from mycore.properties.
   *
   * @param uri	the URI of the XML input stream
   * @param validate if true, will validate against XML Schema
   * @throws MCRException if XML could not be parsed
   * @return the parsed XML stream as a DOM document
   **/
  public Document parseURI( String uri )
  { return parseURI( uri, flagvalidation ); }

  /**
   * Parses the XML byte stream with xerces parser and 
   * returns a DOM document. Uses the validation flag given.
   *
   * @param uri	the URI of the XML input stream
   * @param validate if true, will validate against XML Schema
   * @throws MCRException if XML could not be parsed
   * @return the parsed XML stream as a DOM document
   **/
  public synchronized Document parseURI( String uri, boolean validate )
  { return parse( new InputSource( uri ), validate ); }

  /**
   * Parses the XML byte stream with xerces parser and 
   * returns a DOM document. Uses the validation flag from
   * mycore.properties
   *
   * @param xml the XML byte stream
   * @throws MCRException if XML could not be parsed
   * @return the parsed XML stream as a DOM document
   **/
  public Document parseXML( String xml )
  { return parseXML( xml, flagvalidation ); }

  /**
   * Parses the XML byte stream with xerces parser and 
   * returns a DOM document. Uses the validation flag given.
   *
   * @param xml the XML byte stream
   * @param validate if true, will validate against XML Schema
   * @throws MCRException if XML could not be parsed
   * @return the parsed XML stream as a DOM document
   **/
  public Document parseXML( String xml, boolean validate )
  {
    InputSource source = new InputSource( new StringReader( xml ) );
    return parse( source, validate ); 
  }

  /**
   * Parses the XML byte stream with xerces parser and 
   * returns a DOM document. Uses the validation flag from
   * mycore.properties
   *
   * @param xml the XML byte stream
   * @throws MCRException if XML could not be parsed
   * @return the parsed XML stream as a DOM document
   **/
  public Document parseXML( byte[] xml )
  { return parseXML( xml, flagvalidation ); }

  /**
   * Parses the XML byte stream with xerces parser and 
   * returns a DOM document. Uses the given validation flag.
   *
   * @param xml the XML byte stream
   * @param validate if true, will validate against XML Schema
   * @throws MCRException if XML could not be parsed
   * @return the parsed XML stream as a DOM document
   **/
  public Document parseXML( byte[] xml, boolean validate )
  {
    InputSource source = new InputSource( new ByteArrayInputStream( xml ) );
    return parse( source, validate ); 
  }

  /**
   * Parses the InputSource with xerces parser and 
   * returns a DOM document. Uses the given validation flag.
   *
   * @param source the XML InputSource
   * @param validate if true, will validate against XML Schema
   * @throws MCRException if XML could not be parsed
   * @return the parsed XML stream as a DOM document
   **/
  private synchronized Document parse( InputSource source, boolean validate )
  {
  	SAXBuilder builder;
  	if (validate)
  		builder=this.builderValid;
  	else
  		builder=this.builder;
    try
    {
      return builder.build(source);
    }
    catch( Exception ex ) 
    {
      logger.error( "Error while parsing XML document", ex );
      throw new MCRException( "Error parsing XML document", ex );
    }
  }

  /** The logger */
  private Logger logger = Logger.getLogger( MCRParserXerces.class );

  /**
   * Handles parser warnings
   **/
  public void warning( SAXParseException ex )
  { 
    logger.warn( getSAXErrorMessage( ex ), ex );
  }

  /**
   * Handles fatal parse errors
   **/
  public void error( SAXParseException ex )
  { 
    logger.error( getSAXErrorMessage( ex ), ex );
    throw new MCRException( "Error parsing XML document: "+ex.getMessage(), ex );
  }

  /**
   * Handles fatal parse errors
   **/
  public void fatalError( SAXParseException ex )
  { 
    logger.fatal( getSAXErrorMessage( ex ) );
    throw new MCRException( "Error parsing XML document: "+ex.getMessage(), ex );
  }

 /** 
  * This methode returns a string of the location.
  *
  * @param ex   the SAXParseException exception
  * @return the location string
  **/
  private String getSAXErrorMessage( SAXParseException ex ) 
  {
    StringBuffer str = new StringBuffer();
    String systemId = ex.getSystemId();
    if (systemId != null) 
    {
      int index = systemId.lastIndexOf('/');
      if( index != -1 ) 
        systemId = systemId.substring(index + 1);
      str.append(systemId);
    }
    str.append( ": line=" );
    str.append( ex.getLineNumber() );
    str.append( " : column=" );
    str.append( ex.getColumnNumber() );
    str.append( " : message=" );
    str.append( ex.getLocalizedMessage() );
    return str.toString();
  }
}
