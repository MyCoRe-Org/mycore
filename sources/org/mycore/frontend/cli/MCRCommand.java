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

package org.mycore.frontend.cli;

import java.text.*;
import java.util.*;
import java.lang.reflect.*;
import org.mycore.common.*;

/** 
 * Represents a command understood by the command line interface. A command
 * has an external input syntax that the user uses to invoke the command and
 * points to a method in a class that implements the command.
 *
 * @see MCRCommandLineInterface
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRCommand
{
  /** The input format used for invoking this command */
  protected MessageFormat messageFormat;

  /** The java method that implements this command */
  protected Method method;

  /** The types of the invocation parameters */
  protected Class[] parameterTypes;

  /** The number of invocation parameters */
  protected int numParameters;

  /** The class providing the implementation method */
  protected String className;

  /** The method implementing this command */
  protected String methodName;

  /** The beginning of the message format up to the first parameter */
  protected String suffix;

 /** 
  * Creates a new MCRCommand.
  *
  * @param format          the command syntax, e.g. "save document {0} to directory {1}"
  * @param methodSignature the method to invoke, e.g. "miless.commandline.DocumentCommands.saveDoc int String"
  **/
  MCRCommand( String format, String methodSignature )
  {
    StringTokenizer st = new StringTokenizer( methodSignature, " " );

    String token      = st.nextToken();
    int    point      = token.lastIndexOf( "." );

    className      = token.substring( 0, point );
    methodName     = token.substring( point + 1 );
    numParameters  = st.countTokens();
    parameterTypes = new Class[ numParameters ];
    messageFormat  = new MessageFormat( format );

    for( int i = 0; i < numParameters; i++ )
    {
      token = st.nextToken();
      Format f;

      if( token.equals( "int" ) ) 
      {
        parameterTypes[ i ] = Integer.TYPE; 
        f = NumberFormat.getNumberInstance();
      }
      else if( token.equals( "String" ) ) 
      {
        parameterTypes[ i ] = String.class;
        f = null;
      }
      else 
      {
        throw new MCRConfigurationException
        ( "Error while parsing command definitions for command line interface:\n" +
          "Unsupported argument type '" + token + "' in command " + methodSignature );
      }

      messageFormat.setFormat( i, f );
    }  

    int pos = format.indexOf( "{" );
    suffix = ( pos == -1 ? format : format.substring( 0, pos ) );
  }

 /** 
  * Returns the method implementing the command behavior.
  *
  * @return The method to be invoked for executing the command
  * @throws ClassNotFoundException when the class that implements the method was not found
  * @throws NoSuchMethodException When the method specified in the constructor was not found
  **/
  protected Method getMethod()
    throws ClassNotFoundException, NoSuchMethodException
  {
    if( method == null )
      method = Class.forName( className ).getMethod( methodName, parameterTypes );

    return method;
  }

 /** 
  * Parses an input string and tries to match it with the message format used to
  * invoke this command.
  *
  * @param commandLine The input from the command line
  * @return null, if the input does not match the message format; otherwise an array holding the parameter values from the command line
  **/
  protected Object[] parseCommandLine( String commandLine )
  {
    try{ return messageFormat.parse( commandLine ); }
    catch( ParseException ex ){ return null; }
  }

 /** 
  * Transforms the parameters found by the MessageFormat parse method into
  * such that can be used to invoke the method implementing this command
  *
  * @param commandParameters The parameters as returned by the <code>parseCommandLine</code> method
  * @return The parameters that can be used to invoke the implementing method
  */
  protected Object[] buildInvocationParameters( Object[] commandParameters )
  {
    Object[] parameters = new Object[ numParameters ];
    for( int i = 0; i < numParameters; i++ )
    {
      if( parameterTypes[ i ] == Integer.TYPE )
        parameters[ i ] = new Integer( ( (Number)commandParameters[ i ] ).intValue() );
      else // if( parameterTypes[ i ] == String.class )
        parameters[ i ] = commandParameters[ i ];
    }
    return parameters;
  }

 /** 
  * Tries to invoke the method that implements the behavior of this command given
  * the user input from the command line. This is only done when the command line
  * syntax matches the syntax used by this command.
  *
  * @return true, if the command syntax matched and the command was invoked, false otherwise
  * @param input The command entered by the user at the command prompt
  * @throws IllegalAccessException when the method can not be invoked
  * @throws InvocationTargetException when an exception is thrown by the invoked method
  * @throws ClassNotFoundException when the class providing the method could not be found
  * @throws NoSuchMethodException when the method specified does not exist
  **/
  public boolean invoke( String input )
    throws IllegalAccessException, InvocationTargetException,
           ClassNotFoundException, NoSuchMethodException
  {
    if( ! input.startsWith( suffix ) ) return false;

    Object[] commandParameters = parseCommandLine( input );
    if( commandParameters == null ) return false;

    getMethod().invoke( null, buildInvocationParameters( commandParameters ) );
    return true;
  }

 /** 
  * Shows the input syntax to be used for invoking this command from the 
  * command prompt.
  **/
  public void showSyntax()
  {
    System.out.println( messageFormat.toPattern() );
  }
}
