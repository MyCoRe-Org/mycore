package org.mycore.wcms2.resources;

import java.util.Collection;
import java.util.Date;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.MCRLayoutUtilities;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.wcms2.access.MCRWCMSPermission;

import com.google.gson.JsonObject;

@Path("wcms/access")
@MCRRestrictedAccess(MCRWCMSPermission.class)
public class MCRWCMSAccessResource {

    @DELETE
    public String delete(@QueryParam("webPageID") String webPageID, @QueryParam("perm") String perm) {
        JsonObject returnObject = new JsonObject();
        if (!MCRLayoutUtilities.hasRule(perm, webPageID)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        MCRAccessStore accessStore = MCRAccessStore.getInstance();
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
        MCRAccessStore accessStore = MCRAccessStore.getInstance();
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
        MCRRuleStore store = MCRRuleStore.getInstance();
        Collection<String> ruleIds = store.retrieveAllIDs();
        for (String id : ruleIds) {
            MCRAccessRule rule = store.getRule(id);
            returnObject.addProperty(rule.getId(), rule.getDescription());
        }
        return returnObject.toString();
    }

}
