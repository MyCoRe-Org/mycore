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

class MCRParseException extends org.mycore.common.MCRException
{
  MCRParseException( String expected, int pos, String query )
  {
    super( expected + " expected at position " + pos + 
    	   " (marked with >< here) in query condition \"" + 
    	   query.substring(0,pos) + "><" + query.substring( pos ) + "\"" );
    
  }
}

interface MCRQueryCondition
{
  public String toString();
  public Element toXML();
}

class MCRAndOrCondition implements MCRQueryCondition
{
  private List children;
  private String type;
  
  public final static String AND = "and";
  public final static String OR  = "or";
  
  public MCRAndOrCondition( String type, MCRQueryCondition firstChild )
  {
    if( ! ( type.equals( AND ) || type.equals( OR ) ) )
      throw new IllegalArgumentException( "and|or expected as condition type" );
    
    this.type = type;
    this.children = new ArrayList();
    this.children.add( firstChild );
  }
  
  public void addChild( MCRQueryCondition child )
  { this.children.add( child ); }
  
  public List getChildren()
  { return children; }
  
  public String getType()
  { return type; }
  
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    for( int i = 0; i < children.size(); i++ )
    {
      sb.append( "(" ).append( children.get(i) ).append( ")" );
      if( i < ( children.size() - 1 ) )
        sb.append( " " + type + " " );
    }
    return sb.toString();
  }
  
  public Element toXML()
  {
    Element cond = new Element( type );
    for( int i = 0; i < children.size(); i++ )
    {
      MCRQueryCondition child = (MCRQueryCondition)( children.get(i) );
      cond.addContent( child.toXML() );
    }
    return cond;
  }
}

class MCRNotCondition implements MCRQueryCondition
{
  private MCRQueryCondition child;
  
  public MCRNotCondition( MCRQueryCondition child )
  { this.child = child; }
  
  public MCRQueryCondition getChild()
  { return child; }
  
  public String toString()
  { return "not (" + child + ")"; }
  
  public Element toXML()
  {
    Element not = new Element( "not" );
    not.addContent( child.toXML() );
    return not;
  }
}

class MCRSimpleCondition implements MCRQueryCondition
{
  private String field;
  private String operator;
  private String value;
  
  public MCRSimpleCondition(String field, String operator, String value) 
  {
    this.field = field;
    this.operator = operator;
    this.value = value;
  }
  
  public String getField()    { return field; }
  public String getOperator() { return operator; }
  public String getValue()    { return value; }
  
  public String toString()
  { return field + " " + operator + " \"" + value + "\""; }
  
  public Element toXML()
  {
    Element condition = new Element( "condition" );
    condition.setAttribute( "field", field );
    condition.setAttribute( "operator", operator );
    condition.setAttribute( "value", value );
    return condition;
  }
}
