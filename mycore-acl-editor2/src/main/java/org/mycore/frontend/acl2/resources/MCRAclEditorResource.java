package org.mycore.frontend.acl2.resources;

import java.io.InputStream;
import java.util.Collection;
import java.util.Date;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Path("ACLE")
public class MCRAclEditorResource {

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    private static final MCRRuleStore RULE_STORE = MCRRuleStore.getInstance();

    private static final MCRAccessStore ACCESS_STORE = MCRAccessStore.getInstance();

    @Context
    HttpServletRequest request;

    @Context
    HttpServletResponse response;

    @Context
    ServletContext context;

    @GET
    @Path("start")
    @MCRRestrictedAccess(MCRAclEditorPermission.class)
    @Produces(MediaType.TEXT_HTML)
    public InputStream start() throws Exception {
        return transform("/META-INF/resources/modules/acl-editor2/gui/xml/webpage.xml");
    }

    protected InputStream transform(String xmlFile) throws Exception {
        InputStream guiXML = getClass().getResourceAsStream(xmlFile);
        if (guiXML == null) {
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        }
        SAXBuilder saxBuilder = new SAXBuilder();
        Document webPage = saxBuilder.build(guiXML);
        XPathExpression<Object> xpath = XPathFactory.instance().compile(
            "/MyCoReWebPage/section/div[@id='mycore-acl-editor2']");
        Object node = xpath.evaluateFirst(webPage);
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String lang = mcrSession.getCurrentLanguage();
        if (node != null) {
            Element mainDiv = (Element) node;
            mainDiv.setAttribute("lang", lang);
            String bsPath = CONFIG.getString("MCR.bootstrap.path", "");
            if (!bsPath.equals("")) {
                bsPath = MCRFrontendUtil.getBaseURL() + bsPath;
                Element item = new Element("link").setAttribute("href", bsPath).setAttribute("rel", "stylesheet")
                    .setAttribute("type", "text/css");
                mainDiv.addContent(0, item);
            }
        }
        MCRContent content = MCRJerseyUtil.transform(webPage, request);
        return content.getInputStream();
    }

    @GET
    @MCRRestrictedAccess(MCRAclEditorPermission.class)
    public String list() {
        Collection<String> ruleIDs = RULE_STORE.retrieveAllIDs();
        JsonArray jsonARules = new JsonArray();
        JsonObject jsonObj = new JsonObject();
        for (String id : ruleIDs) {
            MCRAccessRule rule = RULE_STORE.getRule(id);
            JsonObject jsonO = new JsonObject();
            jsonO.addProperty("ruleID", id);
            jsonO.addProperty("desc", rule.getDescription());
            jsonO.addProperty("ruleSt", rule.getRuleString());
            jsonARules.add(jsonO);
        }
        jsonObj.add("rules", jsonARules);
        JsonArray jsonAAccess = new JsonArray();
        Collection<String> ids = ACCESS_STORE.getDistinctStringIDs();
        for (String id : ids) {
            Collection<String> pools = ACCESS_STORE.getPoolsForObject(id);
            for (String pool : pools) {
                JsonObject jsonO = new JsonObject();
                jsonO.addProperty("accessID", id);
                jsonO.addProperty("accessPool", pool);
                jsonO.addProperty("rule", ACCESS_STORE.getRuleID(id, pool));
                jsonAAccess.add(jsonO);
            }
        }
        jsonObj.add("access", jsonAAccess);
        return jsonObj.toString();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @MCRRestrictedAccess(MCRAclEditorPermission.class)
    public Response add(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
        String accessID = jsonObject.get("accessID").getAsString();
        String accessPool = jsonObject.get("accessPool").getAsString();
        String rule = jsonObject.get("rule").getAsString();

        if (RULE_STORE.existsRule(rule) && !accessID.equals("") && !accessPool.equals("")) {
            MCRRuleMapping accessRule = createRuleMap(accessID, accessPool, rule);

            if (!ACCESS_STORE.existsRule(accessID, accessPool)) {
                ACCESS_STORE.createAccessDefinition(accessRule);
                return Response.ok().build();
            } else {
                return Response.status(Status.CONFLICT).build();
            }
        } else {
            return Response.status(Status.CONFLICT).build();
        }
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @MCRRestrictedAccess(MCRAclEditorPermission.class)
    public String remove(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
        JsonArray jsonArray = jsonObject.getAsJsonArray("access");
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject accessAsJsonObject = jsonArray.get(i).getAsJsonObject();
            String accessID = accessAsJsonObject.get("accessID").getAsString();
            String accessPool = accessAsJsonObject.get("accessPool").getAsString();

            if (ACCESS_STORE.existsRule(accessID, accessPool)) {
                MCRRuleMapping accessRule = ACCESS_STORE.getAccessDefinition(accessPool, accessID);

                if (!accessRule.getObjId().equals("")) {
                    ACCESS_STORE.deleteAccessDefinition(accessRule);
                    accessAsJsonObject.addProperty("success", "1");
                } else {
                    accessAsJsonObject.addProperty("success", "0");
                }
            } else {
                accessAsJsonObject.addProperty("success", "0");
            }
        }
        return jsonObject.toString();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @MCRRestrictedAccess(MCRAclEditorPermission.class)
    public Response edit(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
        String accessIDOld = jsonObject.get("accessIDOld").getAsString();
        String accessPoolOld = jsonObject.get("accessPoolOld").getAsString();
        String mode = jsonObject.get("mode").getAsString();
        String accessIDNew = jsonObject.get("accessIDNew").getAsString();
        String accessPoolNew = jsonObject.get("accessPoolNew").getAsString();
        String accessRuleNew = jsonObject.get("accessRuleNew").getAsString();

        if (!ACCESS_STORE.existsRule(accessIDNew, accessPoolNew) || mode.equals("rule")) {
            if (ACCESS_STORE.existsRule(accessIDOld, accessPoolOld) && RULE_STORE.existsRule(accessRuleNew)
                && !accessIDNew.equals("") && !accessPoolNew.equals("")) {
                MCRRuleMapping accessRule = createRuleMap(accessIDNew, accessPoolNew, accessRuleNew);
                MCRRuleMapping oldAccessRule = ACCESS_STORE.getAccessDefinition(accessPoolOld, accessIDOld);

                if (oldAccessRule != null && !oldAccessRule.getObjId().equals("")) {
                    if (mode.equals("rule")) {
                        ACCESS_STORE.updateAccessDefinition(accessRule);
                    } else {
                        ACCESS_STORE.deleteAccessDefinition(oldAccessRule);
                        ACCESS_STORE.createAccessDefinition(accessRule);
                    }
                } else {
                    ACCESS_STORE.createAccessDefinition(accessRule);
                }
                return Response.ok().build();
            } else {
                return Response.status(Status.CONFLICT).build();
            }
        } else {
            return Response.status(Status.CONFLICT).build();
        }
    }

    @POST
    @Path("rule")
    @MCRRestrictedAccess(MCRAclEditorPermission.class)
    @Consumes(MediaType.APPLICATION_JSON)
    public String addRule(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
        String ruleDesc = jsonObject.get("ruleDesc").getAsString();
        String ruleText = jsonObject.get("ruleText").getAsString();
        MCRAccessRule accessRule;

        try {
            accessRule = createAccessRule(ruleDesc, ruleText);
        } catch (Exception e) {
            return "";
        }
        RULE_STORE.createRule(accessRule);
        return accessRule.getId();
    }

    @DELETE
    @Path("rule")
    @MCRRestrictedAccess(MCRAclEditorPermission.class)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeRule(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
        String ruleID = jsonObject.get("ruleID").getAsString();

        if (!ACCESS_STORE.isRuleInUse(ruleID)) {
            RULE_STORE.deleteRule(ruleID);
            return Response.ok().build();
        } else {
            return Response.status(Status.CONFLICT).build();
        }
    }

    @PUT
    @Path("rule")
    @MCRRestrictedAccess(MCRAclEditorPermission.class)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response editRule(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
        String ruleID = jsonObject.get("ruleID").getAsString();
        String ruleDesc = jsonObject.get("ruleDesc").getAsString();
        String ruleText = jsonObject.get("ruleText").getAsString();
        String uid = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();

        if (RULE_STORE.existsRule(ruleID)) {
            try {
                MCRAccessRule accessRule = new MCRAccessRule(ruleID, uid, new Date(), ruleText, ruleDesc);
                RULE_STORE.updateRule(accessRule);
                return Response.ok().build();
            } catch (Exception e) {
                return Response.status(Status.CONFLICT).build();
            }
        } else {
            return Response.status(Status.CONFLICT).build();
        }
    }

    @PUT
    @Path("multi")
    @MCRRestrictedAccess(MCRAclEditorPermission.class)
    @Consumes(MediaType.APPLICATION_JSON)
    public String editMulti(String data) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(data).getAsJsonObject();
        JsonArray jsonArray = jsonObject.getAsJsonArray("access");
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject accessAsJsonObject = jsonArray.get(i).getAsJsonObject();
            String accessID = accessAsJsonObject.get("accessID").getAsString();
            String accessPool = accessAsJsonObject.get("accessPool").getAsString();
            String accessRule = accessAsJsonObject.get("accessRule").getAsString();

            if (ACCESS_STORE.existsRule(accessID, accessPool) && RULE_STORE.existsRule(accessRule)) {
                MCRRuleMapping newAccessRule = createRuleMap(accessID, accessPool, accessRule);
                MCRRuleMapping oldAccessRule = ACCESS_STORE.getAccessDefinition(accessPool, accessID);

                if (oldAccessRule != null && !oldAccessRule.getObjId().equals("")) {
                    ACCESS_STORE.updateAccessDefinition(newAccessRule);
                    accessAsJsonObject.addProperty("success", "1");
                } else {
                    ACCESS_STORE.createAccessDefinition(newAccessRule);
                    accessAsJsonObject.addProperty("success", "1");
                }
            } else {
                accessAsJsonObject.addProperty("success", "0");
            }
        }
        return jsonObject.toString();
    }

    private MCRRuleMapping createRuleMap(String accessID, String accessPool, String rule) {
        String uid = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        MCRRuleMapping accessRule = new MCRRuleMapping();
        accessRule.setObjId(accessID);
        accessRule.setPool(accessPool);
        accessRule.setRuleId(rule);
        accessRule.setCreationdate(new Date());
        accessRule.setCreator(uid);
        return accessRule;
    }

    private MCRAccessRule createAccessRule(String ruleDesc, String ruleText) {
        int freeRuleID = RULE_STORE.getNextFreeRuleID("SYSTEMRULE");
        String ruleID = "0000000000" + String.valueOf(freeRuleID);
        ruleID = ruleID.substring(ruleID.length() - "0000000000".length());
        String newRuleID = "SYSTEMRULE" + ruleID;
        String uid = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();

        return new MCRAccessRule(newRuleID, uid, new Date(), ruleText, ruleDesc);
    }
}
