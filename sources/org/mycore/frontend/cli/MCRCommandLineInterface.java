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

package mycore.commandline;

import java.io.*;
import java.text.*;
import java.util.*;
import java.lang.reflect.*;
import mycore.common.*;

/** 
 * The main class implementing the MyCoRe command line interface. With
 * the command line interface, you can import, export, update and delete
 * documents and other data from/to the filesystem. Metadata is imported
 * from and exported to XML files. The command line interface is for
 * administrative purposes and to be used on the server side. It
 * implements an interactive command prompt and understands a set of commands.
 * Each command is an instance of the class <code>MCRCommand</code>. 
 *
 * @see MCRCommand
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRCommandLineInterface
{
  /** The total number of known commands */
  protected static int numCommands = 0;

  /** The array holding all known commands */
  protected static MCRCommand[] knownCommands = new MCRCommand[ 200 ];

  /** A queue of commands waiting to be executed */
  protected static Vector commandQueue = new Vector();

  /** The standard input console where the user enters commands */
  protected static BufferedReader console = new BufferedReader( new InputStreamReader( System.in ) );

 /** 
  * Reads command definitions from a configuration file 
  * and builds the MCRCommand instances 
  **/
  protected static void initCommands()
    throws NoSuchMethodException, ClassNotFoundException
  {
    MCRConfiguration config = MCRConfiguration.instance();

    //MCRParserInterface parser = (MCRParserInterface)
    //  ( config.getInstanceOf( "MCR.parser_class_name" ) );
    // ToDo: dom = parser.parseURI( filename ); usw.

    knownCommands[ numCommands++ ] = new MCRCommand( 
      "process {0}", 
      "mycore.commandline.MCRCommandLineInterface.readCommandsFile String" );
    knownCommands[ numCommands++ ] = new MCRCommand( 
      "help", 
      "mycore.commandline.MCRCommandLineInterface.listKnownCommands" );
    knownCommands[ numCommands++ ] = new MCRCommand( 
      "exit", 
      "mycore.commandline.MCRCommandLineInterface.exit" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "quit",
      "mycore.commandline.MCRCommandLineInterface.exit" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "! {0}",
      "mycore.commandline.MCRCommandLineInterface.executeShellCommand String" );
  }   

 /** 
  * The main method that either shows up an interactive command prompt or
  * reads a file containing a list of commands to be processed
  */
  public static void main( String[] args )
  {
    System.out.println( "MyCoRe Command Line Interface. Type 'help' to get help!" );
    System.out.print( "Initializing: " );

    try{ initCommands(); }
    catch( Exception ex )
    { 
      System.out.println();
      System.out.println( ex ); 
      System.exit( 1 );
    }
    System.out.println( "done." );

    if( args.length > 0 )
    {
      StringBuffer cmd = new StringBuffer();

      for( int i = 0; i < args.length; i++ )
        cmd.append( args[ i ] ).append( " " );

      commandQueue.addElement( cmd.toString().trim() );
      commandQueue.addElement( "exit" );
    }

    String command;

    while( true )
    {
      if( commandQueue.isEmpty() )
      {
        command = readCommandFromPrompt();
      }
      else
      {
        command = (String) commandQueue.firstElement();
        commandQueue.removeElementAt( 0 );
      }
      processCommand( command );
    }
  }
 
 /** 
  * Shows up a command prompt.
  *
  * @return The command entered by the user at stdin
  */
  protected static String readCommandFromPrompt()
  {
    System.out.println();
    String line = "";
    do
    {
      System.out.print( "MyCoRe:> " );
      try{ line = console.readLine(); }catch( IOException ex ){}
    }
    while( ( line = line.trim() ).length() == 0 );

    return line;
  }

 /** 
  * Processes a command entered by searching a matching command
  * in the list of known commands and executing its method.
  *
  * @param command The command string to be processed
  */
  protected static void processCommand( String command )
  {
    try
    {
      for( int i = 0; i < numCommands; i++ )
        if( knownCommands[ i ].invoke( command ) )
          return;
      
      System.out.println( "Command not understood. Enter 'help' to get a list of commands." );
    }
    catch( Exception ex ){ System.out.println( ex ); }
  }

 /** 
  * Reads a file containing a list of commands to be executed and adds 
  * them to the commands queue for processing.  This method implements
  * the "process ..." command.
  *
  * @param file The file holding the commands to be processed
  * @throws IOException when the file could not be read
  * @throws FileNotFoundException when the file was not found
  */
  public static void readCommandsFile( String file )
    throws IOException, FileNotFoundException
  {
    BufferedReader reader = new BufferedReader( new FileReader( file ) );
    System.out.println( "Reading commands from file " + file );

    String line; 
    int pos = 0;
    while( ( line = reader.readLine() ) != null )
    {
      line = line.trim();
      if( line.startsWith( "#" ) || ( line.length() == 0 ) )
        continue;
      else
        commandQueue.insertElementAt( line, pos++ );
    }
    reader.close();
  }

 /** 
  * Shows a list of commands understood by the command line interface and
  * shows their input syntax. This method implements the "help" command
  */
  public static void listKnownCommands()
  {
    System.out.println( "The following is a list of known commands:\n" );

    for( int i = 0; i < numCommands; i++ )
      knownCommands[ i ].showSyntax();
  }

 /** 
  * Executes simple shell commands from inside the command line
  * interface and shows their output. This method implements commands
  * entered beginning with exclamation mark, like "! ls -l /temp"
  *
  * @param command the shell command to be executed
  * @throws IOException when an IO error occured while catching the output returned by the command
  * @throws InterruptedException when the external command execution was interrupted
  * @throws SecurityException when the command could not be executed for security reasons
  */
  public static void executeShellCommand( String command )
    throws IOException, SecurityException, InterruptedException
  {
    Process p = Runtime.getRuntime().exec( command );

    showOutput( p.getInputStream() );
    showOutput( p.getErrorStream() );
  }
  
 /** 
  * Catches the output read from an input stream and prints it line by line
  * on standard out. This is used to catch the stdout and stderr stream output
  * when executing an external shell command.
  */
  protected static void showOutput( InputStream in )
    throws IOException
  {
    int c;
    while( ( c = in.read() ) != -1 )
      System.out.print( (char)c );
  }

 /** 
  * Exits the command line interface. This method implements the "exit" and
  * "quit" commands.
  */
  public static void exit()
  {
    System.out.println( "Goodbye, and remember: \"Alles wird gut.\"" );
    System.exit( 0 );
  }
}
