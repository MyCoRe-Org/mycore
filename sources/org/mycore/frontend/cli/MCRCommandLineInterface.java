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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

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
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRCommandLineInterface
  {

  /** The Logger **/
  static Logger logger=Logger.getLogger(MCRCommandLineInterface.class.getName());

  /** The configuration **/
  private static MCRConfiguration config = null;

  /** The total number of known commands */
  protected static int numCommands = 0;

  /** The array holding all known commands */
  protected static MCRCommand[] knownCommands = new MCRCommand[ 200 ];

  /** A queue of commands waiting to be executed */
  protected static Vector commandQueue = new Vector();

  /** The standard input console where the user enters commands */
  protected static BufferedReader console = new BufferedReader( new InputStreamReader( System.in ) );

  /** The current session */
  private static MCRSession session = null;

 /**
  * Reads command definitions from a configuration file
  * and builds the MCRCommand instances
  **/
  protected static void initCommands()
    {
    // **************************************
    // Built-in commands
    // **************************************

    knownCommands[ numCommands++ ] = new MCRCommand(
      "process {0}",
      "org.mycore.frontend.cli.MCRCommandLineInterface.readCommandsFile String",
      "Execute the shell command {0}."
      );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "help {0}",
      "org.mycore.frontend.cli.MCRCommandLineInterface.listKnownCommands String",
      "List the helpt text for the command under {0}."
      );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "help",
      "org.mycore.frontend.cli.MCRCommandLineInterface.listKnownCommands",
      "List current all possible commands."
      );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "exit",
      "org.mycore.frontend.cli.MCRCommandLineInterface.exit",
      "Stop the commandline tool."
      );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "quit",
      "org.mycore.frontend.cli.MCRCommandLineInterface.exit",
      "Stop the commandline tool."
      );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "! {0}",
      "org.mycore.frontend.cli.MCRCommandLineInterface.executeShellCommand String",
      "Execute the shell command {0}."
      );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "change to user {0} with {1}",
      "org.mycore.frontend.cli.MCRCommandLineInterface.changeToUser String String",
      "Change the user {0} with the given password in {1}."
      );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "login {0}",
      "org.mycore.frontend.cli.MCRCommandLineInterface.login String",
      "Start the login dialog for the user {0}."
      );
    knownCommands[ numCommands++ ] = new MCRCommand(
      "whoami",
      "org.mycore.frontend.cli.MCRCommandLineInterface.whoami",
      "Print the current user."
      );

    // *************************************************
    // Commands for object management
    // *************************************************

/*
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "delete object from {0} to {1}",
      "org.mycore.frontend.cli.MCRObjectCommands.deleteFromTo String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "delete object {0}",
      "org.mycore.frontend.cli.MCRObjectCommands.delete String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "load object from file {0}",
      "org.mycore.frontend.cli.MCRObjectCommands.loadFromFile String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "update object from file {0}",
      "org.mycore.frontend.cli.MCRObjectCommands.updateFromFile String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "load all objects from directory {0}",
      "org.mycore.frontend.cli.MCRObjectCommands.loadFromDirectory String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "update all objects from directory {0}",
      "org.mycore.frontend.cli.MCRObjectCommands.updateFromDirectory String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "save object from {0} to {1} to directory {2}",
      "org.mycore.frontend.cli.MCRObjectCommands.save String String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "save object of {0} to directory {1}",
      "org.mycore.frontend.cli.MCRObjectCommands.save String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "get last object ID for base {0}",
      "org.mycore.frontend.cli.MCRObjectCommands.getLastID String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "get next object ID for base {0}",
      "org.mycore.frontend.cli.MCRObjectCommands.getNextID String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "show last object ID for base {0}",
      "org.mycore.frontend.cli.MCRObjectCommands.showLastID String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "show next object ID for base {0}",
      "org.mycore.frontend.cli.MCRObjectCommands.showNextID String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "check file {0}",
      "org.mycore.frontend.cli.MCRObjectCommands.checkXMLFile String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "repair metadata search of type {0}",
      "org.mycore.frontend.cli.MCRObjectCommands.repairMetadataSearch String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "repair metadata search of ID {0}",
      "org.mycore.frontend.cli.MCRObjectCommands.repairMetadataSearchForID String" );
*/


    // Read internal commands
    String internals = config.getString("MCR.internal_command_classes","");
    if (internals.length()==0) return;
    StringTokenizer st = new StringTokenizer(internals,",");
    while (st.hasMoreTokens()) {
      String classname = st.nextToken();
      logger.debug("Will load commands from the internal class "+classname);
      Object obj = new Object();
      try {
        obj = Class.forName(classname).newInstance();
        ArrayList ar = ((MCRExternalCommandInterface)obj).getPossibleCommands();
        for (int i=0;i<ar.size();i++) {
          knownCommands[ numCommands++ ] = ((MCRCommand)ar.get(i));
          logger.debug("Add command '"+knownCommands[ numCommands-1 ].showSyntax()+"'");
          }
        }
      catch (ClassNotFoundException e) {
        logger.error(classname+" ClassNotFoundException"); }
      catch (IllegalAccessException e) {
        logger.error(classname+" IllegalAccessException"); }
      catch (InstantiationException e) {
        logger.error(classname+" InstantiationException"); }
      }

    // Read external commands
    String externals = config.getString("MCR.external_command_classes","");
    if (externals.length()==0) return;
    st = new StringTokenizer(externals,",");
    while (st.hasMoreTokens()) {
      String classname = st.nextToken();
      logger.debug("Will load commands from the external class "+classname);
      Object obj = new Object();
      try {
        obj = Class.forName(classname).newInstance();
        ArrayList ar = ((MCRExternalCommandInterface)obj).getPossibleCommands();
        for (int i=0;i<ar.size();i++) {
          knownCommands[ numCommands++ ] = ((MCRCommand)ar.get(i));
          logger.debug("Add command '"+knownCommands[ numCommands-1 ].showSyntax()+"'");
          }
        }
      catch (ClassNotFoundException e) {
        logger.error(classname+" ClassNotFoundException"); }
      catch (IllegalAccessException e) {
        logger.error(classname+" IllegalAccessException"); }
      catch (InstantiationException e) {
        logger.error(classname+" InstantiationException"); }
      }
    }

 /**
  * The main method that either shows up an interactive command prompt or
  * reads a file containing a list of commands to be processed
  */
  public static void main( String[] args )
    {
    config = MCRConfiguration.instance();
    session = MCRSessionMgr.getCurrentSession();

    System.out.println();
    System.out.println("MyCoRe 1.1 Command Line Interface.");
    System.out.println();
    System.out.println("MyCoRe Type 'help' to get help!" );
    System.out.println("MyCoRe Initializing: " );

    try{
      initCommands(); }
    catch( MCRException ex ) {
      logger.debug( ex.getStackTraceAsString() );
      logger.error(ex.getMessage());
      System.out.println();
      System.exit( 1 );
      }
    System.out.println( "MyCoRe Done." );
    System.out.println();

    if( args.length > 0 ) {
      StringBuffer cmd = new StringBuffer();
      for( int i = 0; i < args.length; i++ ) {
        int j = args[ i ].indexOf(";;");
        if (j!=-1) {
          cmd.append( args[ i ].substring(0,j) ).append( " " );
          commandQueue.addElement( cmd.toString().trim() );
          cmd = new StringBuffer();
          continue;
          }
        cmd.append( args[ i ] ).append( " " );
        }
      if (cmd.toString().trim().length() != 0) {
        commandQueue.addElement( cmd.toString().trim() ); }
      commandQueue.addElement( "exit" );
      }

    String command;
    while( true ) {
      if( commandQueue.isEmpty() ) {
        command = readCommandFromPrompt(); }
      else {
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
    String line = "";
    do {
      System.out.print( "MyCoRe > " );
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
      {
        if( knownCommands[ i ].invoke( command ) ) {
          System.out.println("MyCoRe Done."); return; }
      }
      System.out.println( "MyCoRe Command not understood. Enter 'help' to get a list of commands." );
    }
    catch( Throwable t1 )
    { //t1.printStackTrace();
      if( t1 instanceof MCRException )
        logMCRException( (MCRException)t1 );
      else if( ( t1 instanceof InvocationTargetException ) || ( t1 instanceof ExceptionInInitializerError ) )
      {
        Throwable t2 = null;
        if( t1 instanceof InvocationTargetException )
          t2 = ( (InvocationTargetException)t1 ).getTargetException();
        else
          t2 = ( (ExceptionInInitializerError)t1 ).getException();

        if( t2 instanceof MCRException )
          logMCRException( (MCRException)t2 );
        else if( t2 instanceof Exception )
          logException( (Exception)t2 );
        else // it is any other Throwable
          logThrowable( t2 );
      }
      else logThrowable( t1 );
    }
  }

 /**
  * Outputs an MCRException to the logger.
  **/
  private static void logMCRException( MCRException mex )
  {
    logger.debug( mex.getStackTraceAsString() );
    logger.debug( mex.getClass().getName() );
    logger.error(mex.getMessage() );
    logger.error(" ");

    if( mex.getException() != null ) logException( mex.getException() );
  }

 /**
  * Outputs an Exception to the logger.
  **/
  private static void logException( Exception ex )
  {
    logger.error(ex.getClass().getName());
    logger.error(ex.getMessage ());
    logger.debug( MCRException.getStackTraceAsString( ex ) );
    logger.error(" ");
  }

 /**
  * Outputs a Throwable to the logger.
  **/
  private static void logThrowable( Throwable t )
  {
    logger.error(t.getClass().getName() );
    logger.error(t.getMessage () );
    logger.error(" ");
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
    System.out.println( "MyCoRe Reading commands from file " + file );
    String line;
    int pos = 0;
    while( ( line = reader.readLine() ) != null ) {
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
    System.out.println( "MyCoRe "+"The following is a list of known commands:\n" );
    for( int i = 0; i < numCommands; i++ ) {
      System.out.println( "MyCoRe "+knownCommands[ i ].showSyntax()); }
    }
 
 /**
  * Shows the help text of one command.
  *
  * @param the command
  */
  public static void listKnownCommands(String com)
    {
    boolean test = false;
    for( int i = 0; i < numCommands; i++ ) {
      if (knownCommands[ i ].showSyntax().indexOf(com) != -1) {
        System.out.println("MyCoRe help for command \'"+knownCommands[ i ].showSyntax()+"\'");
        System.out.println();
        System.out.println("       "+knownCommands[ i ].getHelpText());
        System.out.println();
        test = true;
        }
      }
    if (!test)
      System.out.println("MyCoRe Unknown command.");
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
  * The method print the current user.
  **/
  public static void whoami()
    { System.out.println( "MyCoRe You are user "+session.getCurrentUserID()); }

 /**
  * This command change the user of the session context to a new user.
  *
  * @param newuser the new user ID
  * @param password the password of the new user
  **/
  public static void changeToUser( String user, String password )
    {
    System.out.println( "MyCoRe The old user is "+session.getCurrentUserID());
    if (org.mycore.user.MCRUserMgr.instance().login(user.trim(), password.trim())) {
      session.setCurrentUserID(user);
      System.out.println( "MyCoRe The new user is "+session.getCurrentUserID());
      }
    else {
      logger.error("The password was wrong, no changes of session context!"); }
    }

 /**
  * This command change the user of the session context to a new user.
  *
  * @param newuser the new user ID
  **/
  public static void login( String user )
    {
    System.out.println( "MyCoRe The old user is "+session.getCurrentUserID());
    String password = "";
    do {
      System.out.print( "MyCoRe Enter the password for user "+user+" > " );
      try{ password = console.readLine(); }catch( IOException ex ){}
      }
    while( ( password = password.trim() ).length() == 0 );
    changeToUser(user,password);
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
    StringBuffer sb = new StringBuffer(1024);
    while( ( c = in.read() ) != -1 ) {
      sb.append( (char)c ); }
    System.out.println( "MyCoRe "+sb.toString());
    }

 /**
  * Exits the command line interface. This method implements the "exit" and
  * "quit" commands.
  */
  public static void exit()
    {
    System.out.println( "MyCoRe Goodbye, and remember: \"Alles wird gut.\"\n" );
    System.exit( 0 );
    }
  }

