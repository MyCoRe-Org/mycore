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

/**
 * This class holds a lot of static check methods to test some access
 * values in the current instance. The methods of this class can be used
 * by the access controll of commandline or servlets.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */

public class MCRAccessChecker
  {
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
    if ((ip == null) || ((ip = ip.trim()).length() ==0)) {
      return false; }
    if ((subnetIP == null) || ((subnetIP = subnetIP.trim()).length() ==0)) {
      return false; }
    if ((subnetMask == null) || ((subnetMask = subnetMask.trim()).length() ==0)) {
      return false; }
    InetAddress ipAddr = InetAddress.getByName(ip);
    InetAddress ipSubAddr = InetAddress.getByName(subnetIP);
    InetAddress subnetMaskAddr = InetAddress.getByName(subnetMask);
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

  }
