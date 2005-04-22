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

package org.mycore.user;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * This class holds a lot of static check methods to test some access
 * values in the current instance. The methods of this class can be used
 * by the access controll of commandline or servlets.
 *
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */

public class MCRAccessChecker {
	private static Logger LOGGER=Logger.getLogger(MCRAccessChecker.class);
  /**
   * The method check the given IP against an IP address area as peer of
   * IP part and subnet mask.
   * 
   * @param ip the IP they should be checked
   * @param subnetIP the IP address area 
   * @param subnetMask the IP subnet mask of the IP address area
   * @return true if the IP is in the net address range, else false
   **/ 
  public static final boolean isInetAddressInSubnet(String ip, String subnetIP,
    String subnetMask) throws UnknownHostException {
  	if (LOGGER.isDebugEnabled()){
  		StringBuffer buf=new StringBuffer();
  		buf.append(MCRAccessChecker.class.getName())
			.append("\nIP: ").append(ip)
			.append("\nsubnetIP: ").append(subnetIP)
			.append("\nsubnetMask: ").append(subnetMask);
  		LOGGER.debug(buf.toString());
  	}
    if ((ip == null) || ((ip = ip.trim()).length() ==0)) {
      return false; }
    if ((subnetIP == null) || ((subnetIP = subnetIP.trim()).length() ==0)) {
      return false; }
    if ((subnetMask == null) || ((subnetMask = subnetMask.trim()).length() ==0)) {
      return false; }
    InetAddress ipAddr = InetAddress.getByName(ip);
    InetAddress ipSubAddr = InetAddress.getByName(subnetIP);
    InetAddress subnetMaskAddr = InetAddress.getByName(subnetMask);
  	if (LOGGER.isDebugEnabled()){
  		StringBuffer buf=new StringBuffer();
  		buf.append(MCRAccessChecker.class.getName())
			.append("\nIPAddr: ").append(ipAddr.getHostAddress())
			.append("\nsubnetIPAddr: ").append(ipSubAddr.getHostAddress())
			.append("\nsubnetMaskAddr: ").append(subnetMaskAddr.getHostAddress());
  		LOGGER.debug(buf.toString());
  	}
    int length = (ipAddr.getAddress().length
      + ipSubAddr.getAddress().length + subnetMaskAddr.getAddress().length) / 3;
    if (length == 4) {
     // subnet1 is the subnet of ipSub and subnet2 is the subnet of ip
     // assigned to the subnetMask
     byte[] subnet1 = new byte[] { 0, 0, 0, 0 };
     byte[] subnet2 = new byte[] { 0, 0, 0, 0 };
     for (int i = 0; i < 4; i++) {
       subnet1[i] = (byte) (ipSubAddr.getAddress()[i] & subnetMaskAddr.getAddress()[i]);
       subnet2[i] = (byte) (ipAddr.getAddress()[i] & subnetMaskAddr.getAddress()[i]);
       if (subnet1[i] != subnet2[i]) return false;
     	if (LOGGER.isDebugEnabled()){
    		StringBuffer buf=new StringBuffer();
    		buf.append(MCRAccessChecker.class.getName())
  			.append("\ni: ").append(i)
  			.append("\nsubnet1[i]: ").append(((int)subnet1[i]) & 0xff)
  			.append("\nsubnet2[i]: ").append(((int)subnet2[i]) & 0xff);
    		LOGGER.debug(buf.toString());
    	}
       }
     return true; //ip is in subnet ipSub/subnetMask
     }
   return false;
   }

  /**
   * The method check the given privilege against the privilege of the user.
   *
   * @param privilege   the given privilege
   * @param userid      the user with his privileges
   * @return true if the user has the privilege, else false
   **/
  public static final boolean hasUserThePrivilege(String privilege, String userid)
    {
    if ((privilege == null) || ((privilege = privilege.trim()).length() ==0)) {
      return false; }
    if ((userid == null) || ((userid = userid.trim()).length() ==0)) {
      return false; }
    ArrayList privs = MCRUserMgr.instance().retrieveAllPrivsOfTheUser(userid);
    return privs.contains(privilege);
    }

  /**
   * The method check that the given user is in the same group or in a
   * group that is a member of the users group.
   *
   * @param user the user which should be checked
   * @param list the list of group users in which the user should be a member of himself group.
   * return true if the user is in the same group or subgroup like one of the list
   **/
  public static final boolean isUserLikeListedUsers(String user,HashSet list)
    {
    // true if the user is the administrator
    if (user.equals("administrator")) { return true; }
    // true if the user is in the owner list
    if (list.contains(user)) { return true; }
    // all other users must have the 'editor' privilege
    if (!hasUserThePrivilege("editor",user)) { return false; }
    // Determine the list of all groups the current user is a member of,
    // including the implicit ones.
    MCRUser currentUser = MCRUserMgr.instance().retrieveUser(user);
    ArrayList allCurrentUserGroupIDs = currentUser.getAllGroupIDs();
    // For all authors (users in the object servflags) we now check if the current
    // user directly is a member of the primary group of the author or implicitly
    // is a member of a group which itself is a member of the primary group of
    // the author.
    for (Iterator it = list.iterator(); it.hasNext();) {
      String primaryGroupID = MCRUserMgr.instance().
        getPrimaryGroupIDOfUser((String)it.next());
      MCRGroup primaryGroup = MCRUserMgr.instance().
        retrieveGroup(primaryGroupID);
      if (primaryGroup.hasUserMember(currentUser)) { return true; }
      ArrayList memberGroupIDsOfPrimaryGroup = primaryGroup.getMemberGroupIDs();
      for (int j = 0; j < allCurrentUserGroupIDs.size(); j++) {
        if (primaryGroup.hasGroupMember((String)allCurrentUserGroupIDs.get(j))) { return true; }
        if (memberGroupIDsOfPrimaryGroup.contains((String)allCurrentUserGroupIDs.get(j))) { return true; }
        }
      }
    // access deny
    return false;
    }
  }
