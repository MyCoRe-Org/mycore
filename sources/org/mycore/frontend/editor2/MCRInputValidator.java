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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.regex.Pattern;
import java.util.Date; 

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.jdom.*;
import org.jdom.transform.JDOMSource;

/**
 * This class provides input validation methods for editor data.
 * 
 * TODO: Use Locale when parsing decimal values
 * 
 * @author Frank Lützenkirchen
 **/
public class MCRInputValidator
{
  /** Template stylesheet for checking XSL conditions **/ 
  private Document stylesheet = null;
  
  /** XSL transformer factory **/
  private TransformerFactory factory = null;                                                                                                             
  
  /** Creates a new, reusable input validator **/
  public MCRInputValidator()
  {
    stylesheet = prepareStylesheet();
    factory = TransformerFactory.newInstance();
  }
  
  /** Cache of reusable stylesheets for checking XSL conditions **/
  private MCRCache xslcondCache = new MCRCache( 20 );
  
  /**
   * Checks the input string against an XSL condition. The syntax of the condition string
   * is same as it would be usable in a xsl:if condition. The input string can be referenced by
   * "." or "text()" in the condition, for example a condition could be "starts-with(.,'http://')".
   * If input string is null, false is returned. 
   * 
   * @param input the string that should be validated 
   * @param condition the XSL condition as it would be used in xsl:when or xsl:if
   * @return false if input is null, otherwise the result of the test is returned
   * @throws MCRConfigurationException if XSL condition has syntax errors
   **/
  public boolean validateXSLCondition( String input, String condition )
  {
    if( input == null ) input = "";
    Document xml = new Document( new Element( "input" ).addContent( input ) );
    Source xmlsrc = new JDOMSource( xml );
    
    Document xsl = (Document)( xslcondCache.get( condition ) );
    if( xsl == null )
    {
      xsl = (Document)( stylesheet.clone() );
      Element when = xsl.getRootElement().getChild( "template" ).getChild( "choose" ).getChild( "when" );
      when.setAttribute( "test", condition );
      xslcondCache.put( condition, xsl );
    }

    try
    {
      Transformer transformer = factory.newTransformer( new JDOMSource( xsl ) );
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      transformer.transform( xmlsrc, new StreamResult( out ) );
      out.close();

      return "t".equals( out.toString( "UTF-8" ) );
    }
    catch( TransformerConfigurationException e )
    {
      String msg = "Could not build XSL transformer";
      throw new org.mycore.common.MCRConfigurationException( msg, e ); 
    }
    catch( UnsupportedEncodingException e )
    {
      String msg = "UTF-8 encoding seems not to be supported?";
      throw new org.mycore.common.MCRConfigurationException( msg, e ); 
    }
    catch( TransformerException e )
    {
      String msg = "Probably syntax error in this XSL condition: " + condition;
      throw new org.mycore.common.MCRConfigurationException( msg, e ); 
    }
    catch( IOException e )
    {
      String msg = "IOException in memory, this should never happen";
      throw new org.mycore.common.MCRConfigurationException( msg, e ); 
    }
  }
  
  /** Prepares a template stylesheet that is used for checking XSL conditions **/
  private Document prepareStylesheet()
  {
    Element stylesheet = new Element( "stylesheet" ).setAttribute( "version", "1.0" );
    Namespace xslns = Namespace.getNamespace( "xsl", "http://www.w3.org/1999/XSL/Transform" );
    stylesheet.setNamespace( xslns );

    Element output = new Element( "output", xslns );
    output.setAttribute( "method", "text" );
    stylesheet.addContent( output );

    Element template = new Element( "template", xslns ).setAttribute( "match", "/input" );
    stylesheet.addContent( template );

    Element choose = new Element( "choose", xslns );
    template.addContent( choose );

    Element when = new Element( "when", xslns );
    when.addContent( "t" );
    Element otherwise = new Element( "otherwise", xslns );
    otherwise.addContent( "f" );
    choose.addContent( when ).addContent( otherwise );
    
    return new Document( stylesheet );
  }
  
  /** Cache of reusable compiled regular expressions **/
  private MCRCache regexpCache = new MCRCache( 20 );
  
  /**
   * Checks the input string against a regular expression.
   * 
   * @see java.util.regex.Pattern#compile(java.lang.String) 
   * 
   * @param input the string that should be validated
   * @param regexp the regular expression using the syntax of the java.util.regex.Pattern class
   * @return false if input is null, otherwise the result of the test is returned
   **/
  public boolean validateRegularExpression( String input, String regexp )
  {
    if( input == null ) input = "";
    
    Pattern p = (Pattern)( regexpCache.get( regexp ) );
    if( p == null )
    {
      p = Pattern.compile( regexp );
      regexpCache.put( regexp, p );
    }
    
    return p.matcher( input ).matches();
  }
  
  /**
   * Checks an input string for minimum and/or maximum length. The minimum and maximum
   * length must be given as a string that contains the actual int number, both arguments
   * are optional if one of the limits should not be checked.
   * 
   * @param input the input string thats length should be checked
   * @param smin minimum length as a string, or null if min lenght should not be checked 
   * @param smax maximum length as a string, or null if max length should not be checked
   * @return true, if the string matches the given min and max lengths
   */
  public boolean validateLength( String input, String smin, String smax )
  {
    if( input == null ) input = "";
    int min = ( smin  == null ? Integer.MIN_VALUE : Integer.parseInt( smin ) ); 
    int max = ( smax  == null ? Integer.MAX_VALUE : Integer.parseInt( smax ) ); 
    return ( input.length() >= min ) && ( input.length() <= max );
  }
  
  /** Cache of reusable DateFormat objects **/
  private MCRCache formatCache = new MCRCache( 20 );
  
  /** 
   * Returns a reusable DateFormat object for the given format string. 
   * That object may come from a cache.
   **/
  private DateFormat getDateTimeFormat( String format )
  { 
    DateFormat df = (DateFormat)( formatCache.get( format ) );
    if( df == null )
    {
      df = new SimpleDateFormat( format );
      df.setLenient( false );
      formatCache.put( format, df );
    }
    return df;
  }
  
  /**
   * Checks if input is null or empty or just contains whitespace.
   * 
   * @param input the string to be checked
   * @return false if input is null or empty or just blanks
   **/
  public boolean validateRequired( String input )
  { return ( ( input != null ) || ( input.trim().length() > 0 ) ); }
 
  /**
   * Checks input for correct data type and minimum/maximum value. 
   * Possible data types are string, integer, decimal or datetime.
   * The min and max arguments are optional and must be expressed as strings.
   * The min and max value are used inclusive in the allowed range of values. 
   * If no check for min or max value should be performed, null can be given for that argument. 
   * For datetime input, the format of the string must be given, 
   * for other data types null should be used.
   * 
   * Usage examples:
   * <ul>
   * <li>validateMinMaxType( input, "integer", "15", "20", null )</li>
   * <li>validateMinMaxType( input, "datetime", "01.01.2000", null, "dd.MM.yyyy" )</li>
   * <li>validateMinMaxType( input, "decimal", "3.1", "4.0", null )</li>
   * </ul>
   * 
   * @see java.text.SimpleDateFormat
   * 
   * @param input the input string to check
   * @param type one of "string", "integer", "decimal" or "datetime"
   * @param min the minimum value as a string, or null if min should not be tested
   * @param max the maximum value as a string, or null if max should not be tested
   * @param format the format of datetime input, as a java.text.SimpleDateFormat pattern
   * @return true if input matches the given data type, min, max value and date time format
   **/
  public boolean validateMinMaxType( String input, String type, String min, String max, String format )
  {
    if( input == null ) input="";
    
    if( type.equals( "string" ) )
    {
      boolean ok = true;
      if( min != null ) ok = ( min.compareTo( input ) <= 0 );
      if( max != null ) ok = ok && ( max.compareTo( input ) >= 0 );
      return ok;
    }
    else if( type.equals( "integer" ) )
    {
      try
      {
        long lmin = ( min == null ? Long.MIN_VALUE : Long.parseLong( min ) );
        long lmax = ( max == null ? Long.MAX_VALUE : Long.parseLong( max ) );
        long lval = Long.parseLong( input );
        return ( lmin <= lval ) && ( lmax >= lval );
      }
      catch( NumberFormatException ex ){ return false; }
    }
    else if( type.equals( "decimal" ) )
    {
      try
      {
        double dmin = ( min == null ? Double.MIN_VALUE : Double.parseDouble( min ) );
        double dmax = ( max == null ? Double.MAX_VALUE : Double.parseDouble( max ) );
        double dval = Double.parseDouble( input );
        return ( dmin <= dval ) && ( dmax >= dval );
      }
      catch( NumberFormatException ex ){ return false; }
    }
    else if( type.equals( "datetime" ) )
    {
      try
      {
        DateFormat df = getDateTimeFormat( format );
        Date dval = df.parse( input );
      
        if( min != null )
        {
          Date dmin = df.parse( min );
          if( dmin.after( dval ) ) return false;
        }
        if( max != null )
        {
          Date dmax = df.parse( max );
          if( dmax.before( dval ) ) return false;
        }
        return true;
      }
      catch( ParseException ex ){ return false; }
    }
    else return false;
  }
}
