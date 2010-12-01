/*
 * $Id$
 * $Revision: 5697 $ $Date: 30.11.2010 $
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
 * A {@link MCRUserInformation} implementation with no roles attached.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSystemUserInformation implements MCRUserInformation {

    private static MCRSystemUserInformation instance = new MCRSystemUserInformation();

    private MCRSystemUserInformation() {
    }

    /**
     * Always returns "SYSTEM" 
     */
    @Override
    public String getCurrentUserID() {
        return "SYSTEM";
    }

    /**
     * Always returns <em>false</em>
     */
    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    /**
     * @return the instance
     */
    public static MCRSystemUserInformation getInstance() {
        return instance;
    }

}
