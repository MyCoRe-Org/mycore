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

package mycore.user;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Vector;
import mycore.common.MCRConfiguration;
import mycore.common.MCRException;

/**
 * This class implements the interface MCRUserStore and uses a flat file for
 * persistent storage of MyCoRe user and group information, respectively. This is
 * only a quick implementation for the "proof of concept". Concrete implementations
 * of MyCoRe systems will not use flat files as user databases but relational
 * databases such as DB2 or LDAP servers.
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUserStoreFile implements MCRUserStore
{
  /** filename for the flat file database containing user information */
  private String dbUsersFileName;

  /** filename for the flat file database containing group information */
  private String dbGroupsFileName;

  /** filename for the flat file database containing privilege information */
  private String dbPrivsFileName;

  /** temporary file name */
  private String dbTmpFileName;

  /**
   * The constructor opens the database files. The paths and names of the files
   * must be provided by the values <code>MCR.userstore_users_file_name</code>,
   * <code>MCR.userstore_groups_file_name</code>,
   * <code>MCR.userstore_privileges_file_name</code>
   * and <code>MCR.userstore_tmp_file_name</code> in <code>mycore.properties</code>.
   */
  public MCRUserStoreFile()
  {
    dbUsersFileName  = MCRConfiguration.instance().getString("MCR.userstore_users_file_name");
    dbGroupsFileName = MCRConfiguration.instance().getString("MCR.userstore_groups_file_name");
    dbPrivsFileName  = MCRConfiguration.instance().getString("MCR.userstore_privileges_file_name");
    dbTmpFileName    = MCRConfiguration.instance().getString("MCR.userstore_tmp_file_name");
  }

  /**
   * This method creates a MyCoRe user object in the persistent datastore.
   * @param newUserID    a String representing the user ID of the new user
   * @param newUser      the new user object to be stored
   */
  public synchronized void createUser(String newUserID, MCRUser newUser)
         throws MCRException, IOException, Exception
  {
    String newUserXML = newUser.toXML("");
    createUserOrGroup(newUserID, newUserXML, dbUsersFileName);
  }

  /**
   * This method creates a MyCoRe group object in the persistent datastore.
   * @param newGroupID   a String representing the group ID of the new group
   * @param newGroup     the new group object to be stored
   */
  public synchronized void createGroup(String newGroupID, MCRGroup newGroup)
         throws MCRException, IOException, Exception
  {
    String newGroupXML = newGroup.toXML("");
    createUserOrGroup(newGroupID, newGroupXML, dbGroupsFileName);
  }

  /**
   * This method creates a MyCoRe privilege set object in the persistent datastore.
   * @param privilegeSet the privilege set object
   */
  public synchronized void createPrivilegeSet(MCRPrivilegeSet privilegeSet)
                           throws MCRException, IOException
  {
    String newPrivSetXML = privilegeSet.toXML("");
    PrintWriter pw  = new PrintWriter(new FileWriter(dbPrivsFileName, true));
    StringBuffer sb = new StringBuffer();

    sb.append(newPrivSetXML.replace('\n', ' '))
      .toString();

    pw.println(sb);
    pw.flush();
    pw.close();
  }

  /**
   * This method deletes a MyCoRe user object from the persistent datastore.
   * @param delUserID    a String representing the MyCoRe user object which is to be deleted
   */
  public synchronized void deleteUser(String delUserID) throws MCRException, IOException
  { deleteUserOrGroup(delUserID, dbUsersFileName); }

  /**
   * This method deletes a MyCoRe group object in the persistent datastore.
   * @param delGroupID   a String representing the MyCoRe group object which is to be deleted
   */
  public synchronized void deleteGroup(String delGroupID) throws MCRException, IOException
  { deleteUserOrGroup(delGroupID, dbGroupsFileName); }

  /**
   * This method tests if a MyCoRe user object is available in the persistent datastore.
   * @param userID        a String representing the MyCoRe user object which is to be looked for
   */
  public synchronized boolean existsUser(String userID) throws MCRException, IOException
  { return existsUserOrGroup(userID, dbUsersFileName); }

  /**
   * This method tests if a MyCoRe privilege set object is available in the persistent datastore.
   */
  public boolean existsPrivilegeSet() throws MCRException, IOException
  {
    String test = retrievePrivilegeSet();
    if ((test != null) && (test != ""))
      return true;
    else return false;
  }

  /**
   * This method tests if a MyCoRe group object is available in the persistent datastore.
   * @param groupID       a String representing the MyCoRe group object which is to be looked for
   */
  public synchronized boolean existsGroup(String groupID) throws MCRException, IOException
  { return existsUserOrGroup(groupID, dbGroupsFileName); }

  /**
   * This method gets all user IDs and returns them as a Vector of strings.
   * @return   Vector of strings including the user IDs of the system
   */
  public synchronized Vector getAllUserIDs() throws MCRException, IOException
  { return getAllUserOrGroupIDs(dbUsersFileName); }

  /**
   * This method gets all group IDs and returns them as a Vector of strings.
   * @return   Vector of strings including the group IDs of the system
   */
  public synchronized Vector getAllGroupIDs() throws MCRException, IOException
  { return getAllUserOrGroupIDs(dbGroupsFileName); }

  /**
   * This method retrieves a MyCoRe user object from the persistent datastore.
   * @param userID       a String representing the MyCoRe user object which is to be retrieved
   * @return             the requested user object
   */
  public synchronized MCRUser retrieveUser(String userID)
                      throws MCRException, IOException, Exception
  {
    String userXML = retrieveUserOrGroup(userID, dbUsersFileName);
    if (userXML != null) {
      MCRUser user = new MCRUser(userXML, false);  // needs not to be created in MCRUserMgr
      return user;
    }
    else return null;
  }

  /**
   * This method retrieves a MyCoRe group object from the persistent datastore.
   * @param groupID      a String representing the MyCoRe group object which is to be retrieved
   * @return             the requested group object
   */
  public synchronized MCRGroup retrieveGroup(String groupID)
                      throws MCRException, IOException, Exception
  {
    String groupXML = retrieveUserOrGroup(groupID, dbGroupsFileName);
    if (groupXML != null) {
      MCRGroup group = new MCRGroup(groupXML, false); // needs not to be created in MCRUserMgr
      return group;
    }
    else return null;
  }

  /**
   * This method retrieves a MyCoRe privilege set object from the persistent datastore.
   * @return  the requested privilege set object as XML string representation
   */
  public String retrievePrivilegeSet() throws MCRException, IOException
  {
    BufferedReader br = new BufferedReader(new FileReader(dbPrivsFileName));
    String line = br.readLine();
    br.close();

    if ((line != null) && (line != ""))
      return line;
    else return null;
  }

  /**
   * This method updates a MyCoRe user object in the persistent datastore.
   * @param userID     a String representing the MyCoRe user object which is to be updated
   * @param user       the user to be updated
   */
  public synchronized void updateUser(String userID, MCRUser user)
                           throws MCRException, IOException, Exception
  {
    deleteUser(userID);           // this will be done a different way if we do not use
    createUser(userID, user);     // a flat file as database
  }

  /**
   * This method updates a MyCoRe privilege set object in the persistent datastore.
   * @param privilegeSet the privilege set object to be updated
   */
  public void updatePrivilegeSet(MCRPrivilegeSet privilegeSet) throws MCRException, IOException
  {
    // Well, don't look at this! This is only a proof of concept!

    try
    {
      File f = new File(dbPrivsFileName);
      while (true)
      {
        f.delete();
        if (!f.exists()) break;
        Thread.sleep(100);  // workaround (I ran into a timeout problem here...)
        System.out.println(dbPrivsFileName+" not yet deleted!");
      }
      createPrivilegeSet(privilegeSet);
    }

    catch  (Exception e) {
      throw new MCRException("MCRUserStoreFile.updatePrivilegeSet(): "+e.getMessage());
    }
  }

  /**
   * This method updates a MyCoRe group object in the persistent datastore.
   * @param groupID    a String representing the MyCoRe group object which is to be updated
   * @param group      the group to be updated
   */
  public synchronized void updateGroup(String groupID, MCRGroup group)
                           throws MCRException, IOException, Exception
  {
    deleteGroup(groupID);         // this will be done a different way if we do not use
    createGroup(groupID, group);  // a flat file as database
  }

  /**
   * This method creates either a user object or a group object in the datastore. It
   * is used by createUser() and createGroup().
   *
   * @param newID       a String representing the user or group ID of the new object
   * @param newXML      a String representing the user or group as an XML Stream
   * @param dbFileName  filename for the flat file database (user or group)
   */
  private void createUserOrGroup(String newID, String newXML, String dbFileName)
               throws MCRException, IOException
  {
    PrintWriter pw  = new PrintWriter(new FileWriter(dbFileName, true));
    StringBuffer sb = new StringBuffer();

    sb.append(newID.trim()).append("::")
      .append(newXML.replace('\n', ' '))
      .toString();

    pw.println(sb);
    pw.flush();
    pw.close();
  }

  /**
   * This method deletes a MyCoRe user or group object in the persistent datastore.
   *
   * @param delID       a String representing the MyCoRe user or group object which is to be deleted
   * @param dbFileName  filename for the flat file database (user or group)
   */
  private void deleteUserOrGroup(String delID, String dbFileName)
               throws MCRException, IOException
  {
    // A trivial method is applied: the given flat file is read in line by line and
    // written back to a temporary file line by line. Only the line representing the
    // user or group ID which is to be deleted ist not written back. Finally the temporary
    // file is moved to the database file. Remember, this flat file implementation is
    // used only for a proof of concept.

    try
    {
      BufferedReader br = new BufferedReader(new FileReader(dbFileName));
      PrintWriter pw    = new PrintWriter(new FileWriter(dbTmpFileName, true));
      String line;

      while ((line = br.readLine()) != null)
      {
        if (line.startsWith("#")) continue;    // skip this line - it is a comment
        StringTokenizer st = new StringTokenizer(line, "::");
        String persistentID = st.nextToken();  // the first token is the userID, the
                                               // second token is the XML representation
        if (!persistentID.equals(delID)) {
          pw.println(line);
        }
      }

      pw.flush();
      br.close();
      pw.close();

      File f = new File(dbFileName);
      while (true)
      {
        f.delete();
        if (!f.exists()) break;
        Thread.sleep(100);  // workaround (I ran into a timeout problem here...)
        System.out.println(dbFileName+" not yet deleted!");
      }
      f = new File(dbTmpFileName);
      f.renameTo(new File(dbFileName));
    }
    catch  (Exception e) {
      throw new MCRException("MCRUserStoreFile.deleteUserOrGroup(): "+e.getMessage());
    }
  }

  /**
   * This method tests, if a given ID (userID or groupID) exists in the datastore. It
   * is used by existsUser() and existsGroup().
   *
   * @param ID          a String representing the MyCoRe user or group object which is to be looked for
   * @param dbFileName  filename for the flat file database (user or group)
   * @return            a boolean value, true if the object exists
   */
  private boolean existsUserOrGroup(String ID, String dbFileName) throws IOException
  {
    // In this simple flat-file datastore implementation this method is essentially
    // the same as the method retrieveUserOrGroup(). The only difference is that not
    // the XML representation of the user or group object is returned but the boolean true.

    BufferedReader br = new BufferedReader(new FileReader(dbFileName));
    String line;
    while ((line = br.readLine()) != null)
    {
      if (line.startsWith("#")) continue;    // skip this line - it is a comment
      StringTokenizer st = new StringTokenizer(line, "::");
      String persistentID = st.nextToken();  // the first token is the ID, the
                                             // second token is the XML representation
      if (persistentID.equals(ID)) {
        br.close();
        return true;  // yep, we found the requested ID (userID or groupID)
      }
    }
    br.close();
    return false;
  }

  /**
   * This method gets all IDs (user or group) from the given database file and returns
   * them as a Vector of strings.
   *
   * @param dbFileName filename for the flat file database (user or group)
   * @return returns all user or group IDs as a Vector of strings
   */
  private Vector getAllUserOrGroupIDs(String dbFileName) throws IOException
  {
    Vector vecIDs     = new Vector();
    BufferedReader br = new BufferedReader(new FileReader(dbFileName));
    String line;
    while ((line = br.readLine()) != null)
    {
      if (line.startsWith("#")) continue;    // skip this line - it is a comment
      StringTokenizer st = new StringTokenizer(line, "::");
      vecIDs.add(st.nextToken());  // the first token is the ID
    }
    br.close();
    return vecIDs;
  }

  /**
   * This method retrieves a MyCoRe user or group object from the persistent datastore.
   *
   * @param ID          a String representing the MyCoRe user or group object which is to be retrieved
   * @param dbFileName  filename for the flat file database (user or group)
   * @return            an XML representation of the user or group object
   */
  private String retrieveUserOrGroup(String ID, String dbFileName)
                 throws MCRException, IOException
  {
    BufferedReader br = new BufferedReader(new FileReader(dbFileName));
    String line;
    while ((line = br.readLine()) != null)
    {
      if (line.startsWith("#")) continue;    // skip this line - it is a comment
      StringTokenizer st = new StringTokenizer(line, "::");
      String persistentID = st.nextToken();  // the first token is the ID, the
                                             // second token is the XML representation
      if (persistentID.equals(ID)) {
        br.close();
        return st.nextToken();  // yep, we found the requested ID
      }
    }
    br.close();
    return null;
  }
}
