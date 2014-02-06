package org.mycore.multitenancy.wcms;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.navigation.Navigation;
import org.mycore.datamodel.navigation.NavigationManager;
import org.mycore.frontend.MCRLayoutUtilities;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.multitenancy.wcms.navigation.NavigationProvider;
import org.mycore.multitenancy.wcms.navigation.WCMSContentManager;
import org.mycore.multitenancy.wcms.navigation.WCMSNavigationManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonStreamParser;

/**
 * Base servlet to handle wcms2 requests.
 *
 * @author Matthias Eichner
 */
public class MCRWCMSServlet extends MCRServlet {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRWCMSServlet.class);

    private File navigationFile;

    private Navigation navigation;

    private WCMSNavigationManager navigationManager;

    private WCMSContentManager contentManager;

    protected enum Type {
        getNavigation, save, getContent, getTemplateList, access, ruleList
    }

    protected enum ErrorType {
        unexpected, unknownType, noPermission
    }

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            String pathToNavigation = MCRConfiguration.instance().getString("MCR.navigationFile");
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = docBuilder.parse(pathToNavigation);
            if(doc.getElementsByTagName("menu").getLength() == 0){
                NodeList nodeList = doc.getFirstChild().getChildNodes();
                for (int i = 0; i < nodeList.getLength(); i++){
                    if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE){
                        ((org.w3c.dom.Element) nodeList.item(i)).setAttribute("id", nodeList.item(i).getNodeName());
                        doc.renameNode(nodeList.item(i), null, "menu");
                    }
                }
                this.navigationFile = new File(pathToNavigation);
                NavigationManager nm = new NavigationManager();
                this.navigation = nm.load(doc);
            }
            else{
                this.navigationFile = new File(pathToNavigation);
                NavigationManager nm = new NavigationManager();
                this.navigation = nm.load(this.navigationFile);
            }
            this.navigationManager = new WCMSNavigationManager();
            this.contentManager = new WCMSContentManager();
        } catch (Exception exc) {
            LOGGER.error(exc);
        }
    }

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        // by default the return value is an empty json object
        Object returnValue = new JsonObject();
        HttpServletResponse response = job.getResponse();
        response.setContentType("text/json");

        // get the type and handle request
        String typeString = job.getRequest().getParameter("type");
        try {
            Type type = Type.valueOf(typeString);
            if (type == Type.getNavigation) {
                returnValue = handleGetNavigation();
            } else if (type == Type.save) {
                returnValue = handleSave(job.getRequest());
            } else if (type == Type.getContent) {
                returnValue = handleGetContent(job.getRequest());
                response.setContentType("text/xml");
            } else if (type == Type.getTemplateList) {
                returnValue = handleGetTemplateList(job.getRequest());
            } else if (type == Type.access) {
                returnValue = handleAccess(job.getRequest());
            } else if (type == Type.ruleList) {
                returnValue = loadRuleList(job.getRequest());
            }
        } catch (IllegalArgumentException iae) {
            LOGGER.error("Unknown type " + typeString, iae);
            returnValue = getError(ErrorType.unknownType, new JsonPrimitive(typeString));
        } catch (Exception exc) {
            LOGGER.error("An unexpected error occur", exc);
            returnValue = getError(ErrorType.unexpected);
        }
        // send return message
        response.setCharacterEncoding(MCRConfiguration.instance().getString("MCR.Request.CharEncoding", "UTF-8"));
        response.getWriter().print(returnValue);
    }

    /**
     * Returns the navigation.xml as json.
     * 
     * @return
     * @throws Exception
     */
    protected JsonObject handleGetNavigation() throws Exception {
        synchronized (this.navigation) {
            return this.navigationManager.toJSON(this.navigation);
        }
    }

    /**
     * Returns the content of a webpage as json.
     * 
     * @param request
     * @return
     */
    protected JsonObject handleGetContent(HttpServletRequest request) {
        String webpageId = request.getParameter("webpageId");
        try {
            return this.contentManager.getContent(webpageId);
        } catch (Exception exc) {
            LOGGER.error("An unexpected error occur", exc);
            return getError(ErrorType.unexpected);
        }
    }

    /**
     * Retrieves the navigation hierarchy and all items from the request input stream
     * and saves it as a new navigation.xml. The content of the items are also stored.
     * 
     * @param request incoming http request
     */
    protected JsonObject handleSave(HttpServletRequest request) {
        JsonObject returnObject = new JsonObject();
        try {
            JsonElement parsedElement = parseRequest(request);
            if (parsedElement == null || !parsedElement.isJsonObject()) {
                LOGGER.warn("Cannot save navigation.xml. The content of the incoming data stream is not supported.");
                return null;
            }
            JsonObject saveObject = parsedElement.getAsJsonObject();
            // get navigation
            Navigation newNavigation = this.navigationManager.fromJSON(saveObject);
            // save navigation
            NavigationManager navigationManager = new NavigationManager();
            navigationManager.save(newNavigation, this.navigationFile);
            this.navigation = newNavigation;
            // save content
            JsonArray items = saveObject.get(NavigationProvider.JSON_ITEMS).getAsJsonArray();
            returnObject = this.contentManager.save(items);
        } catch (Exception exc) {
            LOGGER.error("while saving navigation or content", exc);
            return getError(ErrorType.unexpected);
        }
        return returnObject;
    }

    protected JsonObject handleAccess(HttpServletRequest request) throws Exception {
        JsonObject returnObject = new JsonObject();

        if (MCRAccessManager.checkPermission("manage-wcmsaccess")) {
            String action = request.getParameter("action");
            String webPageId = request.getParameter("webPageId");
            String perm = request.getParameter("perm");
            String ruleId = request.getParameter("ruleId");

            MCRAccessStore accessStore = MCRAccessStore.getInstance();

            if (action.equals("addedit")) {
                if (MCRLayoutUtilities.hasRule(perm, webPageId)) {
                    MCRRuleMapping ruleMap = accessStore.getAccessDefinition(perm, MCRLayoutUtilities.getWebpageACLID(webPageId));
                    ruleMap.setRuleId(ruleId);
                    accessStore.updateAccessDefinition(ruleMap);
                } else {
                    MCRRuleMapping ruleMap = new MCRRuleMapping();
                    ruleMap.setCreator(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
                    ruleMap.setCreationdate(new Date());
                    ruleMap.setPool(perm);
                    ruleMap.setRuleId(ruleId);
                    ruleMap.setObjId(MCRLayoutUtilities.getWebpageACLID(webPageId));
                    accessStore.createAccessDefinition(ruleMap);
                }
                JsonObject doneObject = new JsonObject();
                returnObject.addProperty("type", "editDone");
                returnObject.add("edit", doneObject);
                doneObject.addProperty("ruleId", MCRLayoutUtilities.getRuleID(perm, webPageId));
                doneObject.addProperty("ruleDes", MCRLayoutUtilities.getRuleDescr(perm, webPageId));

            } else if (action.equals("delete")) {
                if (MCRLayoutUtilities.hasRule(perm, webPageId)) {
                    MCRRuleMapping ruleMap = accessStore.getAccessDefinition(perm, MCRLayoutUtilities.getWebpageACLID(webPageId));
                    accessStore.deleteAccessDefinition(ruleMap);

                    JsonObject doneObject = new JsonObject();
                    returnObject.addProperty("type", "editDone");
                    returnObject.add("edit", doneObject);
                    doneObject.addProperty("ruleId", "");
                    doneObject.addProperty("ruleDes", "");
                } else {
                    return getError(ErrorType.unknownType, new JsonPrimitive("no rule to delete"));
                }
            } else {
                return getError(ErrorType.unknownType, new JsonPrimitive("wrong action"));
            }
        }

        else {
            return getError(ErrorType.noPermission, new JsonPrimitive("insufficient permission"));
        }
        return returnObject;

    }

    protected JsonObject loadRuleList(HttpServletRequest request) throws Exception {
        JsonObject returnObject = new JsonObject();
        MCRRuleStore store = MCRRuleStore.getInstance();
        Collection<String> ruleIds = store.retrieveAllIDs();
        for (String id : ruleIds) {
            MCRAccessRule rule = store.getRule(id);
            returnObject.addProperty(rule.getId(), rule.getDescription());
        }
        return returnObject;
    }

    /**
     * Internal method to get json element from http request.
     * 
     * @param request
     * @return
     * @throws IOException
     */
    private JsonElement parseRequest(HttpServletRequest request) throws IOException {
        // read data from stream
        InputStream in = request.getInputStream();
        InputStreamReader reader = new InputStreamReader(in);
        // parse it with gson
        JsonStreamParser parser = new JsonStreamParser(reader);
        synchronized (parser) { // synchronize on an object shared by threads
            if (parser.hasNext())
                return parser.next();
        }
        return null;
    }

    /**
     * Returns the template list.
     * 
     * @param request
     * @return json array containing all templates
     */
    protected JsonArray handleGetTemplateList(HttpServletRequest request) {
        String templatePath = "/templates/master/";
        Set<String> resourcePaths = this.getServletContext().getResourcePaths(templatePath);
        JsonArray returnArr = new JsonArray();
        if (resourcePaths != null) {
            for (String resourcepath : resourcePaths) {
                resourcepath = resourcepath.substring(templatePath.length(), resourcepath.length() - 1);
                returnArr.add(new JsonPrimitive(resourcepath));
            }
        }
        return returnArr;
    }

    /**
     * Creates an json error object.
     * 
     * @param errorType type of the error
     * @return { type: "error", errorType: "typeOfError" }
     */
    private JsonObject getError(ErrorType errorType) {
        return getError(errorType, null);
    }

    /**
     * Creates an json error object.
     * 
     * @param errorType type of the error
     * @param description describes the error
     * @return { type: "error", errorType: "typeOfError", description: ... }
     */
    private JsonObject getError(ErrorType errorType, JsonElement description) {
        JsonObject error = new JsonObject();
        error.addProperty("type", "error");
        error.addProperty("errorType", errorType.name());
        if (description != null)
            error.add("description", description);
        return error;
    }

    //    private JsonObject getAccess(Item item, JsonObject returnObject){
    //        JsonObject accessObject = new JsonObject();
    //        String webPageId = item.getHref();
    //        
    //        if (MCRLayoutUtilities.hasRule("read", webPageId)){
    //            JsonObject readObject = new JsonObject();
    //            accessObject.add("read", readObject);
    //            readObject.addProperty("ruleID", MCRLayoutUtilities.getRuleID("read", webPageId));
    //            readObject.addProperty("ruleDes", MCRLayoutUtilities.getRuleDescr("read", webPageId));
    //        }
    //        else{
    //            accessObject.addProperty("read", "");
    //        }
    //        if (MCRLayoutUtilities.hasRule("write", webPageId)){
    //            JsonObject writeObject = new JsonObject();
    //            accessObject.add("write", writeObject);
    //            writeObject.addProperty("ruleID", MCRLayoutUtilities.getRuleID("write", webPageId));
    //            writeObject.addProperty("ruleDes", MCRLayoutUtilities.getRuleDescr("write", webPageId));  
    //        }
    //        else{
    //            accessObject.addProperty("write", "");
    //        }
    ////        if (item.getChildren() != null){
    ////            for (int i = 0; i < item.getChildren().size(); i++){
    ////                returnObject.add(webPageId,getAccess((Item) item.getChildren().get(i), returnObject));
    ////            }
    ////        }
    //        return accessObject;
    //    }
}