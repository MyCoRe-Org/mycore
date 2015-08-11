/*
 * 
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

import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRParseException;

public class MCRAccessRule implements org.mycore.access.MCRAccessRule {
    String id = "";

    String creator = "";

    Date creationTime = new Date();

    String rule = "";

    String description = "";

    MCRCondition parsedRule;

    static MCRRuleParser parser = new MCRRuleParser();

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
            if (userID.equals(MCRConstants.SUPER_USER_ID)) {
                Logger.getLogger(MCRAccessRule.class).debug("No rule defined, grant access to super user.");
                return true;
            }
            return false;
        }
        Logger.getLogger(this.getClass()).debug("new MCRAccessData");
        MCRAccessData data = new MCRAccessData(userID, date, ip);
        Logger.getLogger(this.getClass()).debug("new MCRAccessData done.");

        Logger.getLogger(this.getClass()).debug("evaluate MCRAccessData");
        boolean returns = parsedRule.evaluate(data);
        Logger.getLogger(this.getClass()).debug("evaluate MCRAccessData done.");
        return returns;
    }

    public MCRCondition getRule() {
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
            Logger.getLogger(MCRAccessRule.class).warn("Error while checking rule.", e);
            return false;
        }
        return checkAccess(userID, new Date(), mcripAddress);
    }
}
