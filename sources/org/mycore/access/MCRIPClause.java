/**
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
 **/

package org.mycore.access;

import java.util.Date;
import org.mycore.user.MCRUser;
import org.mycore.user.MCRGroup;

/**
 * Implementation of a (ip xy) clause
 * 
 * @author   Matthias Kramm
 **/

class MCRIPClause implements MCRAccessCtrlDefinition
{
    private MCRIPAddress ip;

    MCRIPClause(String ip) throws MCRParseException
    {
	try {
	    this.ip = new MCRIPAddress(ip);
	} catch(java.net.UnknownHostException e) {
	    throw new MCRParseException("Couldn't parse/resolve host "+ip);
	}
    }

    public boolean hasAccess(MCRUser user, Date date, MCRIPAddress ip)
    {
	return this.ip.contains(ip);
    }
    
    public String toString()
    {
	return "ip "+ip+"\n";
    }
};

