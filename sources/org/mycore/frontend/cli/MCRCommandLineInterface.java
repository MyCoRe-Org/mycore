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

    // **************************************
    // Built-in commands 
    // **************************************

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

    // *************************************************
    // Commands for document and legal entity management
    // *************************************************

    knownCommands[ numCommands++ ] = new MCRCommand(
      "delete object {0}",
      "mycore.commandline.MCRObjectCommands.delete String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "load object from file {0}",
      "mycore.commandline.MCRObjectCommands.loadFromFile String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "update object from file {0}",
      "mycore.commandline.MCRObjectCommands.updateFromFile String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "load all objects from directory {0}",
      "mycore.commandline.MCRObjectCommands.loadFromDirectory String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "update all objects from directory {0}",
      "mycore.commandline.MCRObjectCommands.updateFromDirectory String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "show object {0}",
      "mycore.commandline.MCRObjectCommands.show String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "get next object ID for base {0}",
      "mycore.commandline.MCRObjectCommands.getID String" );
 
    // ******************************
    // Commands for executing queries
    // ******************************

    knownCommands[ numCommands++ ] = new MCRCommand(
      "query local {0} {1}",
      "mycore.commandline.MCRQueryCommands.queryLocal String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "query remote {0} {1}",
      "mycore.commandline.MCRQueryCommands.queryRemote String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "query host {0} {1} {2}",
      "mycore.commandline.MCRQueryCommands.query String String String" );

    // **************************************
    // Commands for user and group management
    // **************************************

    knownCommands[ numCommands++ ] = new MCRCommand(
      "load users or groups from file {0}",
      "mycore.commandline.MCRUserCommands.loadFromFile String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "check users to groups consistency",
      "mycore.commandline.MCRUserCommands.checkConsistency" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "delete user {0}",
      "mycore.commandline.MCRUserCommands.deleteUser String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "delete group {0}",
      "mycore.commandline.MCRUserCommands.deleteGroup String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "list all users",
      "mycore.commandline.MCRUserCommands.listAllUsers" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "list all groups",
      "mycore.commandline.MCRUserCommands.listAllGroups" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "login as user {0} with password {1}",
      "mycore.commandline.MCRUserCommands.login String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "print group {0} as xml",
      "mycore.commandline.MCRUserCommands.printGroupAsXML String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "print group cache info",
      "mycore.commandline.MCRUserCommands.printGroupCacheInfo" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "print group {0} info",
      "mycore.commandline.MCRUserCommands.printGroupInfo String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "print user {0} as xml",
      "mycore.commandline.MCRUserCommands.printUserAsXML String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "print user cache info",
      "mycore.commandline.MCRUserCommands.printUserCacheInfo" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "print user {0} info",
      "mycore.commandline.MCRUserCommands.printUserInfo String" );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "set password for user {0} to {1}",
      "mycore.commandline.MCRUserCommands.setPassword String String" );
  }   

  public static void test( String a, String b )
  {
    System.out.println( "[" + a + "]" );
    System.out.println( "[" + b + "]" );
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
    catch( Exception ex )
    { 
      if( ex instanceof InvocationTargetException )
      {
        Throwable t = ( (InvocationTargetException)ex ).getTargetException();
        System.out.println( t );
      }
      else
      {
        System.out.println( ex ); 
        System.out.println( ex.getMessage() );
      }
    }
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

/*
   These are the original help text from Detlev's MCRUserTest class.
   They will be included in the help system in the near future.

      System.out.println("\n syntax: checkConsistency");
      System.out.println("\n This command checks the consistency between the user and group objects.");
      System.out.println(" MCRUserMgr.checkUserGroupConsistency(). is invoked.");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      System.out.println("\n syntax: deleteGroup [groupID]");
      System.out.println("\n This command takes a group ID as a parameter and passes this to ");
      System.out.println(" MCRUserMgr.deleteGroup(). See the javadoc documentation of MCRUserMgr.");
      System.out.println("\n syntax: deleteUser [userID]");
      System.out.println("\n This command takes a user ID as a parameter and passes this to ");
      System.out.println(" MCRUserMgr.deleteUser(). See the javadoc documentation of MCRUserMgr.");
      System.out.println("\n syntax: listAllUsers");
      System.out.println("\n This command invokes MCRUserMgr.getAllUserIDs() and lists all");
      System.out.println(" user IDs of the system. See the javadoc documentation of MCRUserMgr.");
      System.out.println("\n syntax: listAllGroups");
      System.out.println("\n This command invokes MCRUserMgr.getAllGroupIDs() and lists all");
      System.out.println(" group IDs of the system. See the javadoc documentation of MCRUserMgr.");
      System.out.println("\n syntax: loadFromFile [file|directory]");
      System.out.println("\n This command takes a filename or a directory as a parameter and passes");
      System.out.println(" this to MCRUserMgr.loadUsersOrGroupsFromFile(). See the javadoc documentation ");
      System.out.println(" of MCRUserMgr.");
      System.out.println("\n syntax: login");
      System.out.println("\n This command asks for a userID and a password and passes this to");
      System.out.println(" MCRUserMgr.login(). It returns information whether the login attempt");
      System.out.println(" was successful or not. See the javadoc documentation of MCRUserMgr.");
      System.out.println("\n syntax: printGroupAsXML [groupID]");
      System.out.println("\n This command prints out all group information as xml representation");
      System.out.println(" MCRUserMgr.getGroupAsXML() is invoked.");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      System.out.println("\n syntax: printGroupCacheInfo");
      System.out.println("\n This command prints out information about the group cache.");
      System.out.println(" MCRUserMgr.getGroupCacheInfo() is invoked.");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      System.out.println("\n syntax: printGroupInfo [groupID]");
      System.out.println("\n This command takes a groupID as a parameter and passes this to");
      System.out.println(" MCRUserMgr.getGroupInfo(). It then prints out the returned information.");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      System.out.println("\n syntax: printUserAsXML [userID]");
      System.out.println("\n This command prints out all user information as xml representation");
      System.out.println(" MCRUserMgr.getUserAsXML() is invoked.");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      System.out.println("\n syntax: printUserCacheInfo");
      System.out.println("\n This command prints out information about the user cache.");
      System.out.println(" MCRUserMgr.getUserCacheInfo() is invoked.");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      System.out.println("\n syntax: printUserInfo [userID]");
      System.out.println("\n This command takes a userID as a parameter and passes this to");
      System.out.println(" MCRUserMgr.getUserInfo(). It then prints out the returned information.");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      System.out.println("\n syntax: setPassword [userID]");
      System.out.println("\n This command takes a userID as a parameter and asks for a password.");
      System.out.println(" It then passes the information to MCRUserMgr.setPassword().");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
*/
