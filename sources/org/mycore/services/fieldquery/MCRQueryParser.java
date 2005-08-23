package org.mycore.services.fieldquery;

import java.util.List;
import java.util.ArrayList;
import org.jdom.Element;
import org.jdom.output.*;

public class MCRQueryParser 
{
  private int pos;
  private int last;
  private String expr;
  
  private final static String NOT = "not";
  private final static char QUOTE = '\"';

  private MCRQueryParser( String expr )
  { 
    this.expr = expr.trim();
    this.last = this.expr.length() - 1;
    this.pos  = 0;
  }
  
  public static MCRQueryCondition parse( Element condition )
  {
    String name = condition.getName();
    if( name.equals( "condition" ) )
    {
      String field = condition.getAttributeValue( "field" );
      String opera = condition.getAttributeValue( "operator" );
      String value = condition.getAttributeValue( "value" );
      return new MCRSimpleCondition( field, opera, value );
    }
    else if( name.equals( "not" ) )
    {
      Element child = (Element)( condition.getChildren().get( 0 ) );
      return new MCRNotCondition( parse( child ) );
    }
    else
    {
      List conditions = new ArrayList();
      List children = condition.getChildren();
      
      for( int i = 0; i < children.size(); i++ )
      {
        Element child = (Element)( children.get( i ) );
        conditions.add( parse( child ) );
      }
      MCRQueryCondition first = (MCRQueryCondition)( conditions.get(0) );
      MCRAndOrCondition cond = new MCRAndOrCondition( name, first );
      for( int i = 1; i < conditions.size(); i++ )
      {
        MCRQueryCondition next = (MCRQueryCondition)( conditions.get( i ) );
        cond.addChild( next );
      }
      return cond;
    }
  }
  
  public static MCRQueryCondition parse( String queryExpression )
  {
    MCRQueryParser parser = new MCRQueryParser( queryExpression );
    return parser.parseCondition();
  }
  
  private void parseWhitespace( boolean optional )
  { 
    int begin = pos;
    while( (pos<=last) && Character.isWhitespace(expr.charAt(pos))) pos++;
    
    if( ( ! optional ) && ( pos == begin ) )
      throw new MCRParseException( "whitespace", pos, expr );
  }

  private String parseIdentifier( String type )
  {
    int begin = pos;
    while( ( pos <= last ) && Character.isLetterOrDigit(expr.charAt(pos)) ) pos++;
    
    if( begin == pos ) 
      throw new MCRParseException( type, pos, expr );
    
    return expr.substring( begin, pos );
  }
  
  private String parseToken( String type )
  {
    int begin = pos;
    while( ( pos <= last ) && ! Character.isWhitespace(expr.charAt(pos)) ) pos++;
    
    if( begin == pos ) 
      throw new MCRParseException( type, pos, expr );
    
    return expr.substring( begin, pos );
  }
  
  // field operator value
  private MCRSimpleCondition parseSingleCond()
  {
    String field = parseIdentifier( "field name" );
    parseWhitespace( false );
    String operator = parseToken( "operator" );
    parseWhitespace( false );
    String value = parseValue();
    
    return new MCRSimpleCondition( field, operator, value );
  }
  
  private MCRQueryCondition parseCondition()
  {
    if( expr.substring( pos ).startsWith( NOT ) )
      return parseNot();
    else if ( expr.substring( pos ).startsWith( "(" ))
      return parseAndOr( null );
    else
      return parseSingleCond();
  }
  
  //( condition ) [ and ( condition )]*
  //( condition ) [ or ( condition )]*
  private MCRAndOrCondition parseAndOr( MCRAndOrCondition cond )
  {
    expect( "(" );
    parseWhitespace( true );
    MCRQueryCondition child = parseCondition();
    parseWhitespace( true );
    expect( ")" );
    
    parseWhitespace( true );
    
    if( pos >= last )
    {
      if( cond == null )
        throw new MCRParseException( "and/or", pos, expr );
      else
      {
        cond.addChild( child );
        return cond;
      }
    }
    else
    {
      String type = parseIdentifier( "and/or" );
      if( ! ( type.equals( MCRAndOrCondition.AND ) || 
              type.equals( MCRAndOrCondition.OR ) ) )
        throw new MCRParseException( "and/or", pos, expr );
      
      if( cond == null ) 
        cond = new MCRAndOrCondition( type, child );
      else if( ! type.equals( cond.getType() ) )
        throw new MCRParseException( cond.getType(), pos, expr );
      else cond.addChild( child );
      
      parseWhitespace( true );
      return parseAndOr( cond );
    }
  }
  
  // not ( condition )
  private MCRNotCondition parseNot()
  {
    expect( NOT );
    parseWhitespace( true );
    expect( "(" );
    parseWhitespace( true );
    MCRQueryCondition child = parseCondition();
    parseWhitespace( true );
    expect( ")" );
    
    return new MCRNotCondition( child );
  }
  
  private String parseValue()
  {
    if( expr.charAt( pos ) == QUOTE )
    {
      int begin = ++pos;
      while( (pos<=last) &&  ! ( expr.charAt(pos)==QUOTE ) ) pos++;
      
      if( pos == last )
        throw new MCRParseException( "\"", pos, expr );
      
      return expr.substring( begin, pos++ ).trim();
    }
    else return parseIdentifier( "value" );
  }
  
  private void expect( String expected )
  {
    if( ! expr.substring( pos ).startsWith( expected ) )
      throw new MCRParseException( expected, pos, expr );
    pos += expected.length();
  }
  
  public static void main( String[] args )
  {
    MCRQueryCondition cond;
    String query;
    XMLOutputter out = new XMLOutputter( Format.getPrettyFormat() );
    
    query = "title contains Optik";
    cond  = MCRQueryParser.parse( query );
    System.out.println( "input: " + query );
    System.out.println( "parsed: " + cond );
    System.out.println( out.outputString( cond.toXML() ) );
    System.out.println();
    
    query = " not (  title   contains  \"Magnetische Wellen\"\t\t)  ";
    cond  = MCRQueryParser.parse( query );
    System.out.println( "input: " + query );
    System.out.println( "parsed: " + cond );
    System.out.println( out.outputString( cond.toXML() ) );
    System.out.println();

    query = "(title contains Optik ) and ( x = y) and (a  < b)";
    cond  = MCRQueryParser.parse( query );
    System.out.println( "input: " + query );
    System.out.println( "parsed: " + cond );
    System.out.println( out.outputString( cond.toXML() ) );
    System.out.println();
  }
}

