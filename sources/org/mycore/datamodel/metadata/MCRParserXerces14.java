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

package mycore.datamodel;

import java.io.*;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import mycore.common.MCRConfiguration;
import mycore.common.MCRConfigurationException;
import mycore.common.MCRException;

/**
 * This class implements the MCRParserInterface to use the Xerces 1.4.x XML
 * parser, which return a DOM.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRParserXerces14 implements MCRParserInterface, ErrorHandler
{

// the Xerces parser
DOMParser parser = new DOMParser();

// data for the configuration
private static boolean flagvalidation        = false;
private static boolean flagnamespaces        = true;
private static boolean flagschemasupport     = false;
private static boolean flagschemafullsupport = true;
private static boolean flagdeferreddom       = true;

private static String setvalidation          =
  "http://xml.org/sax/features/validation";
private static String setnamespaces          =
  "http://xml.org/sax/features/namespaces";
private static String setschemasupport       =
  "http://apache.org/xml/features/validation/schema";
private static String setschemafullsupport   =
  "http://apache.org/xml/features/validation/schema-full-checking";
private static String setdeferreddom         =
  "http://apache.org/xml/features/dom/defer-node-expansion";

/**
 * Constructor for the xerces parser 1.4.x.
 * Here was the configuration set for the XERCES parser.
 **/
public MCRParserXerces14()
  {
  try {
    parser.setFeature(setvalidation,flagvalidation);
    parser.setFeature(setnamespaces,flagnamespaces);
    parser.setFeature(setschemasupport,flagschemasupport);
    parser.setFeature(setschemafullsupport,flagschemafullsupport);
    parser.setFeature(setdeferreddom,flagdeferreddom);
    }
  catch (SAXNotRecognizedException e) {
    throw new MCRException(e.getMessage()); }
  catch (SAXNotSupportedException e) {
    throw new MCRException(e.getMessage()); }
  parser.setErrorHandler(this);
  }

/**
 * This metode parse the XML stream from an URI with XERCES parser and 
 * returns a DOM.
 *
 * @param uri			the URI of the XML input stream
 * @return			the parsed XML straem as a DOM
 **/
public Document parseURI(String uri) throws MCRException
  {
  try {
    parser.parse(uri);
    return parser.getDocument();
    }
  catch (Exception e) {
    throw new MCRException(e.getMessage()); }
  }

/**
 * This metode parse the XML data stream with xerces parser and 
 * returns a DOM.
 *
 * @param xml			the XML input stream
 * @return			the parsed XML straem as a DOM
 **/
public Document parseXML(String xml) throws MCRException
  {
  try {
    InputSource source = new InputSource((Reader)new StringReader(xml));
    parser.parse(source);
    return parser.getDocument();
    }
  catch (Exception e) {
    throw new MCRException(e.getMessage()); }
  }

/**
 * The error handler methode warning.
 **/
public void warning(SAXParseException ex)
  { 
  System.out.println("[Warning] "+getLocationString(ex)+": "+ex.getMessage());
  }

/**
 * The error handler methode error.
 **/
public void error(SAXParseException ex)
  { 
  System.out.println("[Error] "+getLocationString(ex)+": "+ex.getMessage());
  }

/**
 * The error handler methode fatal error.
 **/
public void fatalError(SAXParseException ex)
  { 
  System.out.println("[Fatal Error] "+getLocationString(ex)+": "+
    ex.getMessage());
  }

/** 
 * This methode returns a string of the location.
 *
 * @param ex   the SAXParseException exception
 * @return the location string
 **/
private String getLocationString(SAXParseException ex) {
  StringBuffer str = new StringBuffer();
  String systemId = ex.getSystemId();
  if (systemId != null) {
    int index = systemId.lastIndexOf('/');
    if (index != -1) 
      systemId = systemId.substring(index + 1);
    str.append(systemId);
    }
  str.append(':');
  str.append(ex.getLineNumber());
  str.append(':');
  str.append(ex.getColumnNumber());
  return str.toString();
  }

}

