/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.access.mcrimpl;

import java.util.Date;

import org.mycore.common.MCRUserInformation;

public class MCRAccessData {
    private String userID;

    private MCRUserInformation userInformation;

    private Date date;

    private MCRIPAddress ip;

    MCRAccessData(MCRUserInformation userInfo, Date date, MCRIPAddress ip) {
        this.userInformation = userInfo;
        this.date = date;
        this.ip = ip;
    }

    public MCRAccessData() {
    }

    public Date getDate() {
        return new Date(date.getTime());
    }

    public void setDate(Date date) {
        this.date = new Date(date.getTime());
    }

    public MCRIPAddress getIp() {
        return ip;
    }

    public void setIp(MCRIPAddress ip) {
        this.ip = ip;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public MCRUserInformation getUserInformation() {
        return userInformation;
    }

    public void setUserInformation(MCRUserInformation userInformation) {
        this.userInformation = userInformation;
    }

}
