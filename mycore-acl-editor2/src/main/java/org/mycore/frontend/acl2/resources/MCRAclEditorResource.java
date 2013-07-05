package org.mycore.frontend.acl2.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.services.i18n.MCRTranslation;
import org.xml.sax.SAXException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Path("ACLE")
public class MCRAclEditorResource {
    
    @Context HttpServletRequest request;
    @Context HttpServletResponse response;
    
    @GET
    @Path("start")
    public void start() throws IOException, JDOMException, TransformerException, SAXException{
        if (MCRAccessManager.getAccessImpl().checkPermission("use-aclEditor")) {
            InputStream guiXML = getClass().getResourceAsStream("/META-INF/resources/modules/acl-editor2/gui/xml/webpage.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document webPage = saxBuilder.build(guiXML);
            XPathExpression<Object> xpath = XPathFactory.instance().compile("/MyCoReWebPage/section/div[@id='jportal_acl_editor_module']");
            Object node = xpath.evaluateFirst(webPage);
            MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
            String lang = mcrSession.getCurrentLanguage();
            if(node != null){
                Element mainDiv = (Element) node;
                mainDiv.setAttribute("lang", lang);
            }
            MCRLayoutService.instance().doLayout(request, response, new MCRJDOMContent(webPage));
        }
        else{
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, MCRTranslation.translate("component.session-listing.page.text"));
        }
    }
    
    @GET
    @Path("objectid")
    public void objectid() throws IOException, JDOMException, TransformerException, SAXException{
        if (MCRAccessManager.getAccessImpl().checkPermission("use-aclEditor")) {
            InputStream guiXML = getClass().getResourceAsStream("/META-INF/resources/modules/acl-editor2/gui/xml/objectid.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document webPage = saxBuilder.build(guiXML);
            MCRLayoutService.instance().doLayout(request, response, new MCRJDOMContent(webPage));
        }
        else{
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, MCRTranslation.translate("component.session-listing.page.text"));
        }
    }
    
    @GET
    public String list(){
        MCRRuleStore ruleStore = MCRRuleStore.getInstance();
        Collection<String> ruleIDs = ruleStore.retrieveAllIDs();
        JsonArray jsonARules = new JsonArray();
        JsonObject jsonObj = new JsonObject();
        for (String id : ruleIDs){
            MCRAccessRule rule = ruleStore.getRule(id);
            JsonObject jsonO = new JsonObject();
            jsonO.addProperty("ruleID", id);
            jsonO.addProperty("desc", rule.getDescription());
            jsonO.addProperty("ruleSt", rule.getRuleString());
            jsonARules.add(jsonO);
        }
        jsonObj.add("rules", jsonARules);
        JsonArray jsonAAccess = new JsonArray();
        MCRAccessStore accessStore = MCRAccessStore.getInstance();
        Collection<String> ids = accessStore.getDistinctStringIDs();
        for (String id : ids){
           Collection<String> pools = accessStore.getPoolsForObject(id);
           for(String pool : pools){
               JsonObject jsonO = new JsonObject();
               jsonO.addProperty("accessID", id);
               jsonO.addProperty("accessPool", pool);
               jsonO.addProperty("rule", accessStore.getRuleID(id, pool));
               jsonAAccess.add(jsonO);
           }
        }
        jsonObj.add("access", jsonAAccess);
        return jsonObj.toString();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
        String accessID = jsonObject.get("accessID").getAsString();
        String accessPool = jsonObject.get("accessPool").getAsString();
        String rule = jsonObject.get("rule").getAsString();
        
        MCRRuleMapping accessRule = createRuleMap(accessID, accessPool, rule);
        
        MCRAccessStore accessStore = MCRAccessStore.getInstance();
        
        if(!accessStore.existsRule(accessID, accessPool)){
            accessStore.createAccessDefinition(accessRule);
            return Response.ok().build();
        }
        else{
            return Response.status(409).build();
        }
    }
    
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public String remove(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
        JsonArray jsonArray = jsonObject.getAsJsonArray("access");
        for (int i = 0; i < jsonArray.size(); i++){
            JsonObject accessAsJsonObject = jsonArray.get(i).getAsJsonObject();
            String accessID = accessAsJsonObject.get("accessID").getAsString();
            String accessPool = accessAsJsonObject.get("accessPool").getAsString();
            
            MCRAccessStore accessStore = MCRAccessStore.getInstance();
            MCRRuleMapping accessRule = accessStore.getAccessDefinition(accessPool, accessID);
            
            if(!accessRule.getObjId().equals("")){
                accessStore.deleteAccessDefinition(accessRule);
                accessAsJsonObject.addProperty("success", "1");
            }
            else{
                accessAsJsonObject.addProperty("success", "0");
            }
        }
        return jsonObject.toString();
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response edit(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
        String accessIDOld = jsonObject.get("accessIDOld").getAsString();
        String accessPoolOld = jsonObject.get("accessPoolOld").getAsString();
        String mode = jsonObject.get("mode").getAsString();
        String accessIDNew = jsonObject.get("accessIDNew").getAsString();
        String accessPoolNew = jsonObject.get("accessPoolNew").getAsString();
        String accessRuleNew = jsonObject.get("accessRuleNew").getAsString();
        
        MCRAccessStore accessStore = MCRAccessStore.getInstance();
                
        if (!accessStore.existsRule(accessIDNew, accessPoolNew) || mode.equals("rule")){
            
            MCRRuleMapping accessRule = createRuleMap(accessIDNew, accessPoolNew, accessRuleNew);
            MCRRuleMapping oldAccessRule = accessStore.getAccessDefinition(accessPoolOld, accessIDOld);
                    
            if (oldAccessRule != null && !oldAccessRule.getObjId().equals("")){
                if(mode.equals("rule")){
                    accessStore.updateAccessDefinition(accessRule);
                }
                else{
                    accessStore.deleteAccessDefinition(oldAccessRule);
                    accessStore.createAccessDefinition(accessRule);
                }
            }
            else{
                accessStore.createAccessDefinition(accessRule);
            }
            return Response.ok().build();
        }
        else{
            return Response.status(409).build();
        }
    }
    
    @POST
    @Path("rule")
    @Consumes(MediaType.APPLICATION_JSON)
    public String addRule(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
        String ruleDesc = jsonObject.get("ruleDesc").getAsString();
        String ruleText = jsonObject.get("ruleText").getAsString();
        
        MCRRuleStore ruleStore = MCRRuleStore.getInstance();      
        MCRAccessRule accessRule = createAccessRule(ruleDesc, ruleText);
        ruleStore.createRule(accessRule);
                
        return accessRule.getId();
    }
    
    @DELETE
    @Path("rule")    
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeRule(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
        String ruleID = jsonObject.get("ruleID").getAsString();
        
        MCRRuleStore ruleStore = MCRRuleStore.getInstance();
        if (ruleStore.existsRule(ruleID)){
            ruleStore.deleteRule(ruleID);
            return Response.ok().build();
        }
        else{
            return Response.status(409).build();
        }
    }
    
    @PUT
    @Path("rule")    
    @Consumes(MediaType.APPLICATION_JSON)
    public Response editRule(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
        String ruleID = jsonObject.get("ruleID").getAsString();
        String ruleDesc = jsonObject.get("ruleDesc").getAsString();
        String ruleText = jsonObject.get("ruleText").getAsString();
        String uid = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        
        MCRRuleStore ruleStore = MCRRuleStore.getInstance();      
        MCRAccessRule accessRule = new MCRAccessRule(ruleID, uid, new Date(), ruleText, ruleDesc);
        ruleStore.updateRule(accessRule);
        
        return Response.ok().build();
    }
    
    @POST
    @Path("objectid")   
    @Consumes(MediaType.APPLICATION_JSON)
    public String getObjectID(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
        String accessID = jsonObject.get("accessID").getAsString();
        String accessPool = jsonObject.get("accessPool").getAsString();
        MCRAccessStore accessStore = MCRAccessStore.getInstance();
        String accessRuleID = accessStore.getRuleID(accessID, accessPool);
        
        JsonObject jsonObj = new JsonObject();
        if(accessRuleID != null){
            jsonObj.addProperty("accessRuleID", accessRuleID);
            MCRRuleStore ruleStore = MCRRuleStore.getInstance();
            Collection<String> ruleIDs = ruleStore.retrieveAllIDs();
            JsonArray jsonARules = new JsonArray();
            for (String id : ruleIDs){
                MCRAccessRule rule = ruleStore.getRule(id);
                JsonObject jsonO = new JsonObject();
                jsonO.addProperty("ruleID", id);
                jsonO.addProperty("desc", rule.getDescription());
                jsonO.addProperty("ruleSt", rule.getRuleString());
                jsonARules.add(jsonO);
            }
            jsonObj.add("rules", jsonARules);
        }
        else{
            jsonObj.addProperty("accessRuleID", "null");
        }

        return jsonObj.toString();
    }
    
    
    
    private MCRRuleMapping createRuleMap(String accessID, String accessPool, String rule){
        String uid = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        MCRRuleMapping accessRule = new MCRRuleMapping();
        accessRule.setObjId(accessID);
        accessRule.setPool(accessPool);
        accessRule.setRuleId(rule);
        accessRule.setCreationdate(new Date());
        accessRule.setCreator(uid);
        return accessRule;
    }
    
    private MCRAccessRule createAccessRule(String ruleDesc, String ruleText){
        MCRRuleStore ruleStore = MCRRuleStore.getInstance();
        int freeRuleID = ruleStore.getNextFreeRuleID("SYSTEMRULE");
        String ruleID = "0000000000" + String.valueOf(freeRuleID);
        ruleID = ruleID.substring(ruleID.length() - "0000000000".length());
        String newRuleID = "SYSTEMRULE" + ruleID;
        String uid = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        
        return new MCRAccessRule(newRuleID, uid, new Date(), ruleText, ruleDesc);
    }
}