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
import java.util.StringTokenizer;
import java.util.Vector;
import mycore.common.*;
import mycore.user.*;
import org.w3c.dom.DOMException;

/**
 * The only purpose of this class is to test the functionality of the user manager
 * component of MyCoRe, i.e. the classes MCRUserMgr etc., which are found in the
 * package mycore.user. There are two ways to use this program:
 * <p>
 * I. You can simply start it as a console program and type "help". This gives you
 * a list of known commands. For more information about what the different commands
 * are doing you can type "help" followed by the command name.
 * <p>
 * II. Software testing typically means that you are invoking the same commands over
 * and over again. Therefore a minimal batch system is available to: simply type in
 * the commands you would like to test into a file and start this program with the
 * file name as parameter. Check the output of every command. After execution of
 * every command you will be asked to press "return" to continue.
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
final public class MCRUserMgrTest
{
  /** The standard input for entering commands by the user */
  private static BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

  /**
   * main() takes arguments provided by the user and invokes corresponding methods of
   * MCRUserMgr in order to test the functionality. See the documentation of the
   * different methods, respectively.
   *
   * @param args command to invoke
   */
  public static void main(String[] args)
  {
    System.out.println("\n *** This is the MyCoRe user manager test program. ***");

    if (args.length != 0)     // There is a file with commands for a batch job!
    {
      doBatchJob(args[0]);
      System.out.println("\nDone.");
      System.exit(0);
    }

    String commandLine;
    while(true)
    {
      // This is a software-test application. We are testing the exceptions thrown
      // by the application itself, too. Hence this loop shall not be ended by any
      // thrown exception and we include the following try-catch block.

      try {
        commandLine = readFromPrompt();
        if (commandLine.toLowerCase().equals("quit") || commandLine.toLowerCase().equals("exit"))
          break;
        processCommand(commandLine);
      }

      catch (Exception e) {
        System.out.println("\nException occured: "+e.getMessage());
      }
    } // end while

    System.out.println("\nDone.");
    System.exit(0);
  }

  /**
   * The program can be invoked in "batch mode". For this the provided file must
   * contain valid command sequences.
   *
   * @param filename the filename containing commands
   */
  private static void doBatchJob(String filename)
  {
    Vector commands = new Vector();
    String line;

    try {
      BufferedReader br = new BufferedReader(new FileReader(filename));
      while ((line = br.readLine()) != null) commands.add(line);
      br.close();

      for (int i=0; i<commands.size(); i++)
      {
        // This is a software-test application. We are testing the exceptions thrown
        // by the application itself, too. Hence this loop shall not be ended by any
        // thrown exception and we include another try-catch block.

        try {
          System.out.println("\n>>>>> Processing command: "+(String)commands.elementAt(i)+" <<<<<");
          processCommand((String)commands.elementAt(i));
        }
        catch (Exception e) {
          System.out.println("\nException occured: "+e.getMessage());
        }
        finally {
          System.out.println("\nPress <return> to continue...");
          line = console.readLine();
        }
      } // end for
    } // end try

    catch (FileNotFoundException e) {
      System.out.println("\nBatch file "+filename+" not found!");
    }

    catch (Exception e) {
      System.out.println("\nException occured: "+e.getMessage());
    }
  }

  /**
   * Display a prompt and read a command line.
   * @return returns command typed in at the prompt
   */
  public static String readFromPrompt() throws Exception
  {
    System.out.println();
    String commandLine = "";

    do {
      System.out.print( "MyCoRe:> " );
      commandLine = console.readLine();
    }
    while((commandLine = commandLine.trim()).length() == 0);
    return commandLine;
  }

  /**
   * Process the command.
   * @param commandLine the command to be processed
   */
  public static void processCommand(String commandLine) throws Exception
  {
    StringTokenizer st = new StringTokenizer(commandLine, " ");
    String command = st.nextToken();

    if (command.toLowerCase().equals("help")) {
      if (st.hasMoreTokens()) {
        String helpfor = st.nextToken();
        usage(helpfor.toLowerCase());
        return;
      }
      else usage();
      return;
    }
    else if (command.toLowerCase().equals("checkconsistency")) {
      checkConsistency();
      return;
    }
    else if (command.toLowerCase().equals("deletegroup")) {
      if (st.hasMoreTokens()) {
        String groupID = st.nextToken();
        deleteGroup(groupID);
      }
      else usage();
      return;
    }
    else if (command.toLowerCase().equals("deleteuser")) {
      if (st.hasMoreTokens()) {
        String userID = st.nextToken();
        deleteUser(userID);
      }
      else usage();
      return;
    }
    else if (command.toLowerCase().equals("listallusers")) {
      listAllUsers();
      return;
    }
    else if (command.toLowerCase().equals("listallgroups")) {
      listAllGroups();
      return;
    }
    else if (command.toLowerCase().equals("loadfromfile")) {
      if (st.hasMoreTokens()) {
        String filename = st.nextToken();
        loadFromFile(filename);
      }
      else usage();
      return;
    }
    else if (command.toLowerCase().equals("login")) {
      login();
      return;
    }
    else if (command.toLowerCase().equals("printgroupasxml")) {
      if (st.hasMoreTokens()) {
        String groupID = st.nextToken();
        printGroupAsXML(groupID);
      }
      else usage();
      return;
    }
    else if (command.toLowerCase().equals("printgroupcacheinfo")) {
      printGroupCacheInfo();
      return;
    }
    else if (command.toLowerCase().equals("printgroupinfo")) {
      if (st.hasMoreTokens()) {
        String groupID = st.nextToken();
        printGroupInfo(groupID);
      }
      else usage();
      return;
    }
    else if (command.toLowerCase().equals("printuserasxml")) {
      if (st.hasMoreTokens()) {
        String userID = st.nextToken();
        printUserAsXML(userID);
      }
      else usage();
      return;
    }
    else if (command.toLowerCase().equals("printusercacheinfo")) {
      printUserCacheInfo();
      return;
    }
    else if (command.toLowerCase().equals("printuserinfo")) {
      if (st.hasMoreTokens()) {
        String userID = st.nextToken();
        printUserInfo(userID);
      }
      else usage();
      return;
    }
    else if (command.toLowerCase().equals("setpassword")) {
      if (st.hasMoreTokens()) {
        String userID = st.nextToken();
        setPassword(userID);
      }
      else usage();
      return;
    }
    else{
      System.out.println("Command not known! Try \"help\" for information.");
      return;
    }
  }

  /** print out usage and help information */
  public static void usage()
  {
    System.out.println("\nAll known commands are listed below. Please type help <command>");
    System.out.println("for more information.\n");

    System.out.println("General commands:\n");
    System.out.println(" exit");
    System.out.println(" quit");
    System.out.println(" checkConsistency");
    System.out.println(" loadFromFile [file|directory]");

    System.out.println("\nCommands dealing with users:\n");
    System.out.println(" deleteUser [userID]");
    System.out.println(" listAllUsers");
    System.out.println(" login");
    System.out.println(" printUserAsXML [userID]");
    System.out.println(" printUserCacheInfo");
    System.out.println(" printUserInfo [userID]");
    System.out.println(" setPassword [userID]");

    System.out.println("\nCommands dealing with groups:\n");
    System.out.println(" deleteGroup [groupID]");
    System.out.println(" listAllGroups");
    System.out.println(" printGroupAsXML [groupID]");
    System.out.println(" printGroupCacheInfo");
    System.out.println(" printGroupInfo [groupID]");
  }

  /**
   * print out detailed help information
   * @param helpfor the command for which extended information is needed
   */
  public static void usage(String helpfor)
  {
    if (helpfor.equals("exit")) {
      System.out.println("\n Exits the program. Same as \"quit\".");
      return;
    }
    else if (helpfor.equals("checkconsistency")) {
      System.out.println("\n syntax: checkConsistency");
      System.out.println("\n This command checks the consistency between the user and group objects.");
      System.out.println(" MCRUserMgr.checkUserGroupConsistency(). is invoked.");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      return;
    }
    else if (helpfor.equals("deletegroup")) {
      System.out.println("\n syntax: deleteGroup [groupID]");
      System.out.println("\n This command takes a group ID as a parameter and passes this to ");
      System.out.println(" MCRUserMgr.deleteGroup(). See the javadoc documentation of MCRUserMgr.");
      return;
    }
    else if (helpfor.equals("deleteuser")) {
      System.out.println("\n syntax: deleteUser [userID]");
      System.out.println("\n This command takes a user ID as a parameter and passes this to ");
      System.out.println(" MCRUserMgr.deleteUser(). See the javadoc documentation of MCRUserMgr.");
      return;
    }
    else if (helpfor.equals("listallusers")) {
      System.out.println("\n syntax: listAllUsers");
      System.out.println("\n This command invokes MCRUserMgr.getAllUserIDs() and lists all");
      System.out.println(" user IDs of the system. See the javadoc documentation of MCRUserMgr.");
      return;
    }
    else if (helpfor.equals("listallgroups")) {
      System.out.println("\n syntax: listAllGroups");
      System.out.println("\n This command invokes MCRUserMgr.getAllGroupIDs() and lists all");
      System.out.println(" group IDs of the system. See the javadoc documentation of MCRUserMgr.");
      return;
    }
    else if (helpfor.equals("loadfromfile")) {
      System.out.println("\n syntax: loadFromFile [file|directory]");
      System.out.println("\n This command takes a filename or a directory as a parameter and passes");
      System.out.println(" this to MCRUserMgr.loadUsersOrGroupsFromFile(). See the javadoc documentation ");
      System.out.println(" of MCRUserMgr.");
      return;
    }
    else if (helpfor.equals("login")) {
      System.out.println("\n syntax: login");
      System.out.println("\n This command asks for a userID and a password and passes this to");
      System.out.println(" MCRUserMgr.login(). It returns information whether the login attempt");
      System.out.println(" was successful or not. See the javadoc documentation of MCRUserMgr.");
      return;
    }
    else if (helpfor.equals("printgroupasxml")) {
      System.out.println("\n syntax: printGroupAsXML [groupID]");
      System.out.println("\n This command prints out all group information as xml representation");
      System.out.println(" MCRUserMgr.getGroupAsXML() is invoked.");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      return;
    }
    else if (helpfor.equals("printgroupcacheinfo")) {
      System.out.println("\n syntax: printGroupCacheInfo");
      System.out.println("\n This command prints out information about the group cache.");
      System.out.println(" MCRUserMgr.getGroupCacheInfo() is invoked.");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      return;
    }
    else if (helpfor.equals("printgroupinfo")) {
      System.out.println("\n syntax: printGroupInfo [groupID]");
      System.out.println("\n This command takes a groupID as a parameter and passes this to");
      System.out.println(" MCRUserMgr.getGroupInfo(). It then prints out the returned information.");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      return;
    }
    else if (helpfor.equals("printuserasxml")) {
      System.out.println("\n syntax: printUserAsXML [userID]");
      System.out.println("\n This command prints out all user information as xml representation");
      System.out.println(" MCRUserMgr.getUserAsXML() is invoked.");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      return;
    }
    else if (helpfor.equals("printusercacheinfo")) {
      System.out.println("\n syntax: printUserCacheInfo");
      System.out.println("\n This command prints out information about the user cache.");
      System.out.println(" MCRUserMgr.getUserCacheInfo() is invoked.");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      return;
    }
    else if (helpfor.equals("printuserinfo")) {
      System.out.println("\n syntax: printUserInfo [userID]");
      System.out.println("\n This command takes a userID as a parameter and passes this to");
      System.out.println(" MCRUserMgr.getUserInfo(). It then prints out the returned information.");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      return;
    }
    else if (helpfor.equals("quit")) {
      System.out.println("\n Quits the program. Same as \"exit\".");
      return;
    }
    else if (helpfor.equals("setpassword")) {
      System.out.println("\n syntax: setPassword [userID]");
      System.out.println("\n This command takes a userID as a parameter and asks for a password.");
      System.out.println(" It then passes the information to MCRUserMgr.setPassword().");
      System.out.println(" See the javadoc documentation of MCRUserMgr.");
      return;
    }
    else {
      System.out.println("\nNo help information for this command available!");
      return;
    }
  }

  /** This method invokes MCRUserMgr.checkUserGroupConsistency() */
  private static final void checkConsistency() throws Exception
  {
    MCRUserMgr.instance().checkUserGroupConsistency();
  }

  /**
   * This method invokes MCRUserMgr.deleteGroup()
   * @param groupID the ID of the group which will be deleted
   */
  private static final void deleteGroup(String groupID) throws Exception
  {
    MCRUserMgr.instance().deleteGroup(groupID);
    System.out.println("Group ID " + groupID + " deleted!");
  }

  /**
   * This method invokes MCRUserMgr.deleteUser()
   * @param userID the ID of the user which will be deleted
   */
  private static final void deleteUser(String userID) throws Exception
  {
    MCRUserMgr.instance().deleteUser(userID);
    System.out.println("User ID " + userID + " deleted!");
  }

  /** This method invokes MCRUserMgr.getAllUserIDs() */
  private static final void listAllUsers() throws Exception
  {
    Vector users = new Vector(MCRUserMgr.instance().getAllUserIDs());
    System.out.println();
    for (int i=0; i<users.size(); i++)
      System.out.println(users.elementAt(i));
  }

  /** This method invokes MCRUserMgr.getAllGroupIDs() */
  private static final void listAllGroups() throws Exception
  {
    Vector groups = new Vector(MCRUserMgr.instance().getAllGroupIDs());
    System.out.println();
    for (int i=0; i<groups.size(); i++)
      System.out.println(groups.elementAt(i));
  }

  /**
   * This method invokes MCRUserMgr.loadUsersFromFile()
   * @param filename name of a file or directory containing user or group
   *                 information in XML files
   */
  private static final void loadFromFile(String filename) throws Exception
  {
    System.out.println("Loading user data from file/directory: "+filename);
    MCRUserMgr.instance().loadUsersOrGroupsFromFile(filename);
  }

  /** This method asks for a user ID and a password and then invokes MCRUserMgr.login() */
  private static final void login() throws Exception
  {
    System.out.print("\nEnter user ID  :");
    String userID = console.readLine();
    userID = userID.trim();
    System.out.print("Enter password :");
    String passwd = console.readLine();
    passwd = passwd.trim();

    if (MCRUserMgr.instance().login(userID, passwd))
      System.out.println("User successfully logged in!");
    else
      System.out.println("Access denied! Wrong password?");
  }

  /**
   * This method calls MCRUserMgr.getGroupAsXML()
   * @param groupID the ID of the group for which the XML-representation is needed
   */
  private static final void printGroupAsXML(String groupID) throws Exception
  { System.out.println("\n"+MCRUserMgr.instance().getGroupAsXML(groupID, "\n")); }

  /**
   * This method calls MCRUserMgr.getGroupInfo()
   * @param groupID the ID of the group for which the group information is needed
   */
  private static final void printGroupInfo(String groupID) throws Exception
  { System.out.println("\n"+MCRUserMgr.instance().getGroupInfo(groupID)); }

  /** This method invokes MCRUserMgr.getGroupCacheInfo() */
  private static final void printGroupCacheInfo() throws Exception
  { System.out.println("\n"+MCRUserMgr.instance().getGroupCacheInfo()); }

  /**
   * This method calls MCRUserMgr.getUserAsXML()
   * @param userID the ID of the user for which the XML-representation is needed
   */
  private static final void printUserAsXML(String userID) throws Exception
  { System.out.println("\n"+MCRUserMgr.instance().getUserAsXML(userID, "\n")); }

  /**
   * This method calls MCRUserMgr.getUserInfo()
   * @param userID the ID of the user for which the user information is needed
   */
  private static final void printUserInfo(String userID) throws Exception
  { System.out.println("\n"+MCRUserMgr.instance().getUserInfo(userID)); }

  /** This method invokes MCRUserMgr.getUserCacheInfo() */
  private static final void printUserCacheInfo() throws Exception
  { System.out.println("\n"+MCRUserMgr.instance().getUserCacheInfo()); }

  /**
   * This method invokes MCRUserMgr.setPassword()
   * @param userID the ID of the user for which the password will be set
   */
  private static final void setPassword(String userID) throws Exception
  {
    System.out.print("Enter password :");
    String passwd = console.readLine();
    passwd = passwd.trim();

    MCRUserMgr.instance().setPassword(userID, passwd);
  }
} // end of class MCRUserMgrTest


