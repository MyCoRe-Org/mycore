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

import java.io.*;
import java.text.*;
import java.util.*;
import java.lang.reflect.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.*;

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

    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "process {0}",
      "org.mycore.frontend.cli.MCRCommandLineInterface.readCommandsFile String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "help",
      "org.mycore.frontend.cli.MCRCommandLineInterface.listKnownCommands" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "exit",
      "org.mycore.frontend.cli.MCRCommandLineInterface.exit" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "quit",
      "org.mycore.frontend.cli.MCRCommandLineInterface.exit" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "! {0}",
      "org.mycore.frontend.cli.MCRCommandLineInterface.executeShellCommand String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "change to user {0} with {1}",
      "org.mycore.frontend.cli.MCRCommandLineInterface.changeToUser String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "login {0}",
      "org.mycore.frontend.cli.MCRCommandLineInterface.login String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "whoami",
      "org.mycore.frontend.cli.MCRCommandLineInterface.whoami" );

    // *************************************************
    // Commands for object management
    // *************************************************

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
      "save object {0} to {1}",
      "org.mycore.frontend.cli.MCRObjectCommands.save String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "get next object ID for base {0}",
      "org.mycore.frontend.cli.MCRObjectCommands.getID String" );

    // *************************************************
    // Commands for derivate management
    // *************************************************

    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "delete derivate {0}",
      "org.mycore.frontend.cli.MCRDerivateCommands.delete String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "delete derivate from {0} to {1}",
      "org.mycore.frontend.cli.MCRObjectCommands.delete String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "load derivate from file {0}",
      "org.mycore.frontend.cli.MCRDerivateCommands.loadFromFile String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "update derivate from file {0}",
      "org.mycore.frontend.cli.MCRDerivateCommands.updateFromFile String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "load all derivates from directory {0}",
      "org.mycore.frontend.cli.MCRDerivateCommands.loadFromDirectory String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "update all derivates from directory {0}",
      "org.mycore.frontend.cli.MCRDerivateCommands.updateFromDirectory String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "save derivate {0}",
      "org.mycore.frontend.cli.MCRDerivateCommands.save String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "save derivate {0} to {1}",
      "org.mycore.frontend.cli.MCRDerivateCommands.save String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "get next derivate ID for base {0}",
      "org.mycore.frontend.cli.MCRDerivateCommands.getID String" );

    // **************************************
    // Commands for classification management
    // **************************************

    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "delete classification {0}",
      "org.mycore.frontend.cli.MCRClassificationCommands.delete String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "load classification from file {0}",
      "org.mycore.frontend.cli.MCRClassificationCommands.loadFromFile String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "update classification from file {0}",
      "org.mycore.frontend.cli.MCRClassificationCommands.updateFromFile String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "load all classifications from directory {0}",
      "org.mycore.frontend.cli.MCRClassificationCommands.loadFromDirectory String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "update all classifications from directory {0}",
      "org.mycore.frontend.cli.MCRClassificationCommands.updateFromDirectory String"
      );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "save classification {0} to {1}",
      "org.mycore.frontend.cli.MCRClassificationCommands.save String String" );

    // *************************************
    // Commands for executing configurations
    // *************************************

    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "create database for {0}",
      "org.mycore.frontend.cli.MCRBaseCommands.createDataBase String" );

    // ******************************
    // Commands for executing queries
    // ******************************

    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "query local {0} {1}",
      "org.mycore.frontend.cli.MCRQueryCommands.queryLocal String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "query remote {0} {1}",
      "org.mycore.frontend.cli.MCRQueryCommands.queryRemote String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "query host {0} {1} {2}",
      "org.mycore.frontend.cli.MCRQueryCommands.query String String String" );

    // **************************************
    // Commands for user and group management
    // **************************************

    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "init superuser",
      "org.mycore.frontend.cli.MCRUserCommands.initSuperuser MCRSession" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "check user data consistency",
      "org.mycore.frontend.cli.MCRUserCommands.checkConsistency MCRSession" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "create user data from file {0}",
      "org.mycore.frontend.cli.MCRUserCommands.createUserFromFile MCRSession String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "create group data from file {0}",
      "org.mycore.frontend.cli.MCRUserCommands.createGroupFromFile MCRSession String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "update user data from file {0}",
      "org.mycore.frontend.cli.MCRUserCommands.updateUserFromFile MCRSession String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "update group data from file {0}",
      "org.mycore.frontend.cli.MCRUserCommands.updateGroupFromFile MCRSession String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "update privileges data from file {0}",
      "org.mycore.frontend.cli.MCRUserCommands.updatePrivilegesFromFile MCRSession String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "delete user {0}",
      "org.mycore.frontend.cli.MCRUserCommands.deleteUser MCRSession String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "delete group {0}",
      "org.mycore.frontend.cli.MCRUserCommands.deleteGroup MCRSession String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "list all users",
      "org.mycore.frontend.cli.MCRUserCommands.listAllUsers MCRSession" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "list all groups",
      "org.mycore.frontend.cli.MCRUserCommands.listAllGroups MCRSession" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "list all privileges",
      "org.mycore.frontend.cli.MCRUserCommands.listAllPrivileges MCRSession" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "save all users to file {0}",
      "org.mycore.frontend.cli.MCRUserCommands.saveAllUsersToFile MCRSession String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "save all groups to file {0}",
      "org.mycore.frontend.cli.MCRUserCommands.saveAllGroupsToFile MCRSession String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "save all privileges to file {0}",
      "org.mycore.frontend.cli.MCRUserCommands.saveAllPrivilegesToFile MCRSession String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "save user {0} to file {1}",
      "org.mycore.frontend.cli.MCRUserCommands.saveUserToFile MCRSession String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "save group {0} to file {1}",
      "org.mycore.frontend.cli.MCRUserCommands.saveGroupToFile MCRSession String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "set password for user {0} to {1}",
      "org.mycore.frontend.cli.MCRUserCommands.setPassword MCRSession String String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "set user management to read only mode",
      "org.mycore.frontend.cli.MCRUserCommands.setLock MCRSession" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "set user management to read/write mode",
      "org.mycore.frontend.cli.MCRUserCommands.unLock MCRSession" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "show user {0}",
      "org.mycore.frontend.cli.MCRUserCommands.showUser MCRSession String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "show group {0}",
      "org.mycore.frontend.cli.MCRUserCommands.showGroup MCRSession String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "enable user {0}",
      "org.mycore.frontend.cli.MCRUserCommands.enableUser MCRSession String" );
    knownCommands[ numCommands++ ] = new MCRCommand(session,
      "disable user {0}",
      "org.mycore.frontend.cli.MCRUserCommands.disableUser MCRSession String" );

    // Read external commands
    String externals = config.getString("MCR.external_command_classes","");
    if (externals.length()==0) return;
    StringTokenizer st = new StringTokenizer(externals,",");
    while (st.hasMoreTokens()) {
      String classname = st.nextToken();
      logger.debug("Will load commands from the external class "+classname);
      Object obj = new Object();
      try {
        obj = Class.forName(classname).newInstance();
        ArrayList ar = ((MCRExternalCommandInterface)obj).getPossibleCommands();
        for (int i=0;i<ar.size();i+=2) {
          knownCommands[ numCommands++ ] = new MCRCommand(session,
            (String)ar.get(i),(String)ar.get(i+1));
          logger.debug("Add command '"+(String)ar.get(i)+"'");
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
    PropertyConfigurator.configure(config.getLoggingProperties());
    session = MCRSessionMgr.getCurrentSession();

    logger.info( "" );
    logger.info( "MyCoRe Command Line Interface. Type 'help' to get help!" );
    logger.info( "Initializing: " );

    try{ 
      initCommands(); }
    catch( MCRException ex ) {
      logger.debug( ex.getStackTraceAsString() );
      logger.error( ex.getMessage() );
      logger.error( "" );
      System.exit( 1 );
      }
    logger.info( "Done." );
    logger.info( "" );

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
      logger.info( "MyCoRe:> " );
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
        if( knownCommands[ i ].invoke( command ) ) return; 
      }
      logger.error( "Command not understood. Enter 'help' to get a list of commands." );
    }
    catch( Throwable t1 ) 
    { t1.printStackTrace();
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
    logger.error( mex.getClass().getName() );
    logger.error( mex.getMessage () );
    logger.debug( mex.getStackTraceAsString() );
    logger.error( "" );

    if( mex.getException() != null ) logException( mex.getException() );
  }

 /** 
  * Outputs an Exception to the logger. 
  **/
  private static void logException( Exception ex )
  {
    logger.error( ex.getClass().getName() ); 
    logger.error( ex.getMessage () ); 
    logger.debug( MCRException.getStackTraceAsString( ex ) );
    logger.error( "" );  
  }

 /**  
  * Outputs a Throwable to the logger.  
  **/
  private static void logThrowable( Throwable t )
  {
    logger.error( t.getClass().getName() );  
    logger.error( t.getMessage () );  
    logger.error( "" ); 
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
    logger.info( "Reading commands from file " + file );
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
    logger.info( "The following is a list of known commands:\n" );
    for( int i = 0; i < numCommands; i++ ) {
      logger.info(knownCommands[ i ].showSyntax()); }
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
    { logger.info("You are user "+session.getCurrentUserID()); }

 /**
  * This command change the user of the session context to a new user.
  * 
  * @param newuser the new user ID
  * @param password the password of the new user
  **/
  public static void changeToUser( String user, String password )
    {
    logger.info("The old user is "+session.getCurrentUserID());
    if (org.mycore.user.MCRUserMgr.instance().login(user.trim(),password.trim())) {
      session.setCurrentUserID(user); 
      logger.info("The new user is "+session.getCurrentUserID());
      }
    else {
      logger.error("The password was false, no changes of session context!"); }
    }

 /**
  * This command change the user of the session context to a new user.
  * 
  * @param newuser the new user ID
  **/
  public static void login( String user )
    {
    logger.info("The old user is "+session.getCurrentUserID());
    String password = "";
    do {
      logger.info( "Enter the password for user "+user+" > " );
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
    logger.info(sb.toString());
    }

 /**
  * Exits the command line interface. This method implements the "exit" and
  * "quit" commands.
  */
  public static void exit()
    {
    logger.info( "Goodbye, and remember: \"Alles wird gut.\"\n" );
    System.exit( 0 );
    }
  }

