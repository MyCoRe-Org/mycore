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
package org.mycore.access.facts.condition;

import java.net.UnknownHostException;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.fact.MCRIpAddressFact;
import org.mycore.access.mcrimpl.MCRIPAddress;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.annotation.MCRProperty;

public class MCRIPCondition extends MCRAbstractFactCondition<MCRIPAddress, MCRIpAddressFact> {

    private static Logger LOGGER = LogManager.getLogger();

    @MCRProperty(name = "IP", required = false)
    private String defaultIP;

    public void parse(Element xml) {
        super.parse(xml);
        if (StringUtils.isEmpty(getTerm()) && StringUtils.isNotEmpty(defaultIP)) {
            setTerm(defaultIP);
        }
    }

    @Override
    public Optional<MCRIpAddressFact> computeFact(MCRFactsHolder facts) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        try {
            MCRIPAddress checkIP = new MCRIPAddress(getTerm());

            MCRIPAddress currentIP = new MCRIPAddress(session.getCurrentIP());

            if (checkIP.contains(currentIP)) {
                MCRIpAddressFact fact = new MCRIpAddressFact(getFactName(), getTerm());
                fact.setValue(currentIP);
                facts.add(fact);
                return Optional.of(fact);
            }
        } catch (UnknownHostException e) {
            LOGGER.error("Unknown IP Address", e);
        }
        return Optional.empty();
    }
}
