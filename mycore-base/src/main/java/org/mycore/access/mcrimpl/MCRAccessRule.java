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

import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.jdom2.Element;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRParseException;

public class MCRAccessRule implements org.mycore.access.MCRAccessRule {
    private String id = "";

    private String creator = "";

    private Date creationTime = new Date();

    String rule = "";

    private String description = "";

    private MCRCondition<MCRAccessData> parsedRule;

    private static MCRRuleParser parser = new MCRRuleParser();

    public MCRAccessRule(String id, String creator, Date creationTime, String rule, String description)
        throws MCRParseException {
        setId(id);
        setCreator(creator);
        setCreationTime(creationTime);
        setRule(rule);
        setDescription(description);

    }

    public boolean checkAccess(String userID, Date date, MCRIPAddress ip) {
        if (parsedRule == null) {
            if (userID.equals(MCRSystemUserInformation.getSuperUserInstance().getUserID())) {
                LogManager.getLogger(MCRAccessRule.class).debug("No rule defined, grant access to super user.");
                return true;
            }
            return false;
        }
        LogManager.getLogger(this.getClass()).debug("new MCRAccessData");
        MCRAccessData data = new MCRAccessData(userID, date, ip);
        LogManager.getLogger(this.getClass()).debug("new MCRAccessData done.");

        LogManager.getLogger(this.getClass()).debug("evaluate MCRAccessData");
        boolean returns = parsedRule.evaluate(data);
        LogManager.getLogger(this.getClass()).debug("evaluate MCRAccessData done.");
        return returns;
    }

    public MCRCondition<MCRAccessData> getRule() {
        return parsedRule;
    }

    public void setRule(String rule) {
        this.rule = rule;
        parsedRule = rule == null ? null : parser.parse(rule);
    }

    public String getRuleString() {
        if (rule == null) {
            return "";
        }
        return rule;
    }

    public Date getCreationTime() {
        return new Date(creationTime.getTime());
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime == null ? null : new Date(creationTime.getTime());
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Element getRuleElement() {
        Element el = new Element("mcraccessrule");
        el.addContent(new Element("id").setText(id));
        el.addContent(new Element("creator").setText(id));
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        el.addContent(new Element("creationdate").setText(df.format(creationTime)));
        el.addContent(new Element("rule").setText(rule));
        el.addContent(new Element("description").setText("" + description));
        return el;
    }

    @Override
    public boolean validate() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        String userID = session.getUserInformation().getUserID();
        MCRIPAddress mcripAddress;
        try {
            mcripAddress = new MCRIPAddress(session.getCurrentIP());
        } catch (UnknownHostException e) {
            LogManager.getLogger(MCRAccessRule.class).warn("Error while checking rule.", e);
            return false;
        }
        return checkAccess(userID, new Date(), mcripAddress);
    }
}
