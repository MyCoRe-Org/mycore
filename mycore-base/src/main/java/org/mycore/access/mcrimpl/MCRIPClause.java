/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import org.jdom2.Element;
import org.mycore.parsers.bool.MCRIPCondition;
import org.mycore.parsers.bool.MCRParseException;

/**
 * Implementation of a (ip xy) clause
 *
 * @author Matthias Kramm
 */
public class MCRIPClause implements MCRIPCondition {
    private MCRIPAddress ip;

    public MCRIPClause() {
    }

    public MCRIPClause(String ip) throws MCRParseException {
        set(ip);
    }

    @Override
    public void set(String ip) throws MCRParseException {
        try {
            this.ip = new MCRIPAddress(ip);
        } catch (java.net.UnknownHostException e) {
            throw new MCRParseException("Couldn't parse/resolve host " + ip);
        }
    }

    @Override
    public boolean evaluate(MCRAccessData data) {
        return data.getIp() != null && ip.contains(data.getIp());
    }

    @Override
    public String toString() {
        return "ip " + ip + " ";
    }

    @Override
    public Element toXML() {
        Element cond = new Element("condition");
        cond.setAttribute("field", "ip");
        cond.setAttribute("operator", "=");
        cond.setAttribute("value", ip.toString());
        return cond;
    }
}
