/*
 * $Id$
 * $Revision: 5697 $ $Date: 26.11.2010 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common;

/**
 * Encapsulates informations about the current authenticated user.
 * 
 * A instance of this interface is always bound to {@link MCRSession}
 * and can be requested via {@link MCRSession#getUserInformation()}.
 * An implementer of this interface should bind the instance to the session via
 * {@link MCRSession#setUserInformation(MCRUserInformation)}.
 * @author Thomas Scheffler (yagee)
 *
 */
public interface MCRUserInformation {

    public static final String ATT_PRIMARY_GROUP = "primaryGroup";

    public static final String ATT_REAL_NAME = "realName";

    public static final String ATT_EMAIL = "eMail";

    /**
     * The UserID is the information that is used in <em>user</em> clauses of the ACL System.
     */
    public String getUserID();

    /**
     * The role information is used in <em>group</em> clauses of the ACL System.
     */
    public boolean isUserInRole(String role);

    /**
     * Get additional attributes if they are provided by the underlying user system
     * @param attribute user attribute name
     * @return attribute value as String or null if no value is defined;
     */
    public String getUserAttribute(String attribute);

}
