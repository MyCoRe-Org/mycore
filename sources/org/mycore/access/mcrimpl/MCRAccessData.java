/*
 * $RCSfile$
 * $Revision$ $Date$
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

package org.mycore.access.mcrimpl;

import java.util.Date;

import org.mycore.user2.MCRUser;

public class MCRAccessData {
    private MCRUser user;

    private Date date;

    private MCRIPAddress ip;

    MCRAccessData(MCRUser user, Date date, MCRIPAddress ip) {
        this.user = user;
        this.date = date;
        this.ip = ip;
    }

    public MCRAccessData() {
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public MCRIPAddress getIp() {
        return ip;
    }

    public void setIp(MCRIPAddress ip) {
        this.ip = ip;
    }

    public MCRUser getUser() {
        return user;
    }

    public void setUser(MCRUser user) {
        this.user = user;
    }
}
