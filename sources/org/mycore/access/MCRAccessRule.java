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

package org.mycore.access;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jdom.Element;
import org.mycore.user.MCRUser;

import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRParseException;

public class MCRAccessRule {
    String id = "";

    String creator = "";

    Date creationTime = new Date();

    String rule = "";

    String description = "";

    MCRCondition parsedRule;

    static MCRRuleParser parser = new MCRRuleParser();

    public MCRAccessRule(String id, String creator, Date creationTime, String rule, String description) throws MCRParseException {
        this.id = id;
        this.creator = creator;
        this.creationTime = creationTime;
        this.rule = rule;
        this.description = description;

        if (this.rule != null) {
            this.parsedRule = parser.parse(this.rule);
        }
    }

    public boolean checkAccess(MCRUser user, Date date, MCRIPAddress ip) {
        if (this.parsedRule == null) {
            return true;
        }

        MCRAccessData data = new MCRAccessData(user, date, ip);

        return this.parsedRule.evaluate(data);
    }

    public MCRCondition getRule() {
        return this.parsedRule;
    }

    /**
     * rule
     * 
     * @param rule
     */
    public void setRule(String rule) {
        this.rule = rule;
    }

    
    public String getRuleString()
    {
        if (rule==null)
            return "";
        else
            return rule;
    }

    /**
     * creationtime
     * 
     * @return
     */
    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * creator
     * 
     * @return
     */
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     * description
     * 
     * @return
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * id
     * 
     * @return
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * method returns element representation of mcraccessrule
     * @return
     */
    public Element getRuleElement(){
        Element el = new Element("mcraccessrule");
        el.addContent(new Element("id").setText(this.id));
        el.addContent(new Element("creator").setText(this.id));
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        el.addContent(new Element("creationdate").setText(df.format(this.creationTime)));
        el.addContent(new Element("rule").setText(this.rule));
        el.addContent(new Element("description").setText(""+this.description));
        return el;
    }
}
