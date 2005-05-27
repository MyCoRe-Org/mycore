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

package org.mycore.backend.hibernate;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Date;

import org.mycore.user.MCRPrivilege;

/**
 * Class which extends MCRPrivilege with setters and getters in order to be
 * usable for Hibernate.
 * 
 * @see org.mycore.user.MCRPrivilege
 * 
 * @author Matthias Kramm
 */
class MCRPrivilegeExt extends MCRPrivilege {
    MCRPrivilegeExt() {
        super(null, null);
    }

    MCRPrivilegeExt(MCRPrivilege u) {
        super(u);
    }

    /** @return sets the name of the privilege */
    void setName(String privName) {
        this.privName = privName;
    }

    /** @return sets the description of the privilege */
    void setDescription(String privDescription) {
        this.privDescription = privDescription;
    }
}