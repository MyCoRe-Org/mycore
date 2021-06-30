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
package org.mycore.access.xml.conditions;

import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.mcrimpl.MCRIPAddress;
import org.mycore.access.xml.MCRFacts;
import org.mycore.common.MCRSessionMgr;

public class MCRIPCondition extends MCRSimpleCondition {
    @SuppressWarnings("unused")
    private static Logger LOGGER = LogManager.getLogger();

    private MCRIPAddress checkIP = null;

    private MCRIPAddress currentIP = null;

    @Override
    public void parse(Element xml) {
        super.parse(xml);
        try {
            checkIP = new MCRIPAddress(this.value);
        } catch (UnknownHostException e) {
            checkIP = null;
        }
    }

    @Override
    public boolean matches(MCRFacts facts) {
        MCRIPCondition theCondi = (MCRIPCondition) facts.require(this.type);
        if (theCondi.currentIP != null && checkIP != null) {
            boolean result = checkIP.contains(theCondi.currentIP);
            if (result) {
                facts.add(this);
                return true;
            }
        }
        return false;
    }

    @Override
    public void setCurrentValue(MCRFacts facts) {
        try {
            this.value = MCRSessionMgr.getCurrentSession().getCurrentIP();
            currentIP = new MCRIPAddress(this.value);
        } catch (UnknownHostException e) {
            currentIP = null;
        }
    }
}
