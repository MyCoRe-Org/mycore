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

package org.mycore.wcms2.resources;

import java.util.Collection;
import java.util.Date;

import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.MCRLayoutUtilities;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.wcms2.access.MCRWCMSPermission;

import com.google.gson.JsonObject;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

@Path("wcms/access")
@MCRRestrictedAccess(MCRWCMSPermission.class)
public class MCRWCMSAccessResource {

    @DELETE
    public String delete(@QueryParam("webPageID") String webPageID, @QueryParam("perm") String perm) {
        JsonObject returnObject = new JsonObject();
        if (!MCRLayoutUtilities.hasRule(perm, webPageID)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        MCRAccessStore accessStore = MCRAccessStore.obtainInstance();
        MCRRuleMapping ruleMap = accessStore.getAccessDefinition(perm, MCRLayoutUtilities.getWebpageACLID(webPageID));
        accessStore.deleteAccessDefinition(ruleMap);
        JsonObject doneObject = new JsonObject();
        returnObject.addProperty("type", "editDone");
        returnObject.add("edit", doneObject);
        return returnObject.toString();
    }

    @POST
    public String createOrUpdate(@QueryParam("webPageID") String webPageID, @QueryParam("perm") String perm,
        @QueryParam("ruleID") String ruleID) {
        MCRAccessStore accessStore = MCRAccessStore.obtainInstance();
        JsonObject returnObject = new JsonObject();
        if (MCRLayoutUtilities.hasRule(perm, webPageID)) {
            MCRRuleMapping ruleMap = accessStore.getAccessDefinition(perm,
                MCRLayoutUtilities.getWebpageACLID(webPageID));
            ruleMap.setRuleId(ruleID);
            accessStore.updateAccessDefinition(ruleMap);
        } else {
            MCRRuleMapping ruleMap = new MCRRuleMapping();
            ruleMap.setCreator(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
            ruleMap.setCreationdate(new Date());
            ruleMap.setPool(perm);
            ruleMap.setRuleId(ruleID);
            ruleMap.setObjId(MCRLayoutUtilities.getWebpageACLID(webPageID));
            accessStore.createAccessDefinition(ruleMap);
        }
        JsonObject doneObject = new JsonObject();
        returnObject.addProperty("type", "editDone");
        returnObject.add("edit", doneObject);
        doneObject.addProperty("ruleId", MCRLayoutUtilities.getRuleID(perm, webPageID));
        doneObject.addProperty("ruleDes", MCRLayoutUtilities.getRuleDescr(perm, webPageID));
        return returnObject.toString();
    }

    @GET
    public String getRuleList() {
        JsonObject returnObject = new JsonObject();
        MCRRuleStore store = MCRRuleStore.obtainInstance();
        Collection<String> ruleIds = store.retrieveAllIDs();
        for (String id : ruleIds) {
            MCRAccessRule rule = store.getRule(id);
            returnObject.addProperty(rule.getId(), rule.getDescription());
        }
        return returnObject.toString();
    }

}
