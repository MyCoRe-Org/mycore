/*
 * $RCSfile: MCRClassificationBrowserData.java,v $
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

package org.mycore.datamodel.classifications;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * Instances of MCRClassificationBrowser contain the data of the currently
 * displayed navigation tree. MCRClassificationBrowser uses one
 * MCRClassificationBrowserData instance per browser session to store and update
 * the category lines to be displayed.
 * 
 * @author Anja Schaar
 * 
 */
public class MCRClassificationBrowserData {

    protected String pageName;

    protected String xslStyle;

    protected String uri;

    private MCRConfiguration config;

    private static final Logger LOGGER = Logger.getLogger(MCRClassificationBrowserData.class);

    private String startPath = "";

    private String actItemID = "";

    private String lastItemID = "";

    private String[] categFields;

    private String emptyLeafs = null;

    private String view = null;

    private String searchField = "";

    private boolean sort = false;

    private String objectType = null;

    private String[] objectTypeArray = null;

    private String restriction = null;

    private boolean comments = false;

    private RowCreator rowsCreator;

    int totalNumOfDocs = 0;

    public static Map<String, String> ClassUserTable = new Hashtable<String, String>();

    private static MCRSessionListener ClassUserTableCleaner = new MCRSessionListener() {

        public void sessionEvent(MCRSessionEvent event) {
            switch (event.getType()) {
            case destroyed:
                clearUserClassTable(event.getSession());
                break;
            default:
                LOGGER.debug("Skipping event: " + event.getType());
                break;
            }
        }

    };

    static {
        MCRSessionMgr.addSessionListener(ClassUserTableCleaner);
    }

    private MCRClassificationPool classificationPool;

    private MCRCategLinkService linkService;

    private MCRPermissionTool permissionTool;

    MCRClassificationBrowserData(final String u, final String mode, final String actclid, final String actEditorCategid,
            MCRClassificationPool classificationPool, MCRPermissionTool permissionTool, MCRCategLinkService linkService,
            RowCreator rowCreator) throws Exception {
        this.classificationPool = classificationPool;
        this.permissionTool = permissionTool;
        this.linkService = linkService;
        this.config = MCRConfiguration.instance();
        this.rowsCreator = rowCreator;
        init(u, mode, actclid, actEditorCategid);
    }

    public MCRClassificationBrowserData(final String u, final String mode, final String actclid, final String actEditorCategid)
            throws Exception {
        this(u, mode, actclid, actEditorCategid, getClassificationPool(), new MCRPermissionToolImpl(), MCRCategLinkServiceFactory
                .getInstance(), new RowCreator_NoLines(getClassificationPool(), MCRCategLinkServiceFactory.getInstance(), MCRConfiguration
                .instance()));
    }

    private void init(final String u, final String mode, final String actclid, final String actEditorCategid) throws Exception {
        uri = u;
        LOGGER.debug(" incomming Path " + uri);

        final String[] uriParts = uri.split("/"); // mySplit();
        LOGGER.info(" Start");
        final String browserClass = (uriParts.length <= 1 ? "default" : uriParts[1]);

        LOGGER.debug(" PathParts - classification " + browserClass);
        LOGGER.debug(" Number of PathParts =" + uriParts.length);

        String classifID = config.getString("MCR.ClassificationBrowser." + browserClass + ".Classification", actclid);
        String defaultPageName = config.getString("MCR.ClassificationBrowser.default.EmbeddingPage");
        pageName = config.getString("MCR.ClassificationBrowser." + browserClass + ".EmbeddingPage", defaultPageName);

        String defaultXSLStyle = config.getString("MCR.ClassificationBrowser.default.Style");
        xslStyle = config.getString("MCR.ClassificationBrowser." + browserClass + ".Style", defaultXSLStyle);

        String defaultSearchField = config.getString("MCR.ClassificationBrowser.default.searchField");
        searchField = config.getString("MCR.ClassificationBrowser." + browserClass + ".searchField", defaultSearchField);

        String defaultEmptyLeafs = config.getString("MCR.ClassificationBrowser.default.EmptyLeafs");
        emptyLeafs = config.getString("MCR.ClassificationBrowser." + browserClass + ".EmptyLeafs", defaultEmptyLeafs);

        String defaultView = config.getString("MCR.ClassificationBrowser.default.View");
        view = config.getString("MCR.ClassificationBrowser." + browserClass + ".View", defaultView);

        setObjectTypes(browserClass);

        sort = config.getBoolean("MCR.ClassificationBrowser." + browserClass + ".Sort", false);
        restriction = config.getString("MCR.ClassificationBrowser." + browserClass + ".Restriction", null);

        startPath = browserClass;

        if ("edit".equals(mode)) {
            pageName = config.getString("MCR.classeditor.EmbeddingPage");
            xslStyle = config.getString("MCR.classeditor.Style");
            sort = false;
            view = "tree";

            if (classifID.length() == 0) {
                return;
            }
        }

        if (emptyLeafs == null) {
            emptyLeafs = "yes";
        }
        if (view == null || !view.endsWith("flat")) {
            view = "tree";
        }
        LOGGER.debug("uriParts length: " + uriParts.length);
        clearPath(uriParts);
        MCRCategoryID id = MCRCategoryID.rootID(classifID);
        setClassification(id);

        rowsCreator.setBrowserClass(browserClass);
        rowsCreator.setUri(uri);
        rowsCreator.setView(view);
        rowsCreator.setCommented(comments);
        setActualPath(actEditorCategid);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(" SetClassification " + classifID);
            LOGGER.debug(" Empty nodes: " + emptyLeafs);
            LOGGER.debug(" View: " + view);
            LOGGER.debug(" Comment: " + comments);
            LOGGER.debug(" Doctypes: " + objectType);
            for (String element : objectTypeArray) {
                LOGGER.debug(" Type: " + element);
            }
            LOGGER.debug(" Restriction: " + restriction);
            LOGGER.debug(" Sort: " + sort);
        }
    }

    private void setObjectTypes(final String browserClass) {
        LOGGER.debug("setObjectTypes(" + browserClass + ")");
        try {
            // NOTE: read *.Doctype for compatiblity reasons
            objectType = config.getString("MCR.ClassificationBrowser." + browserClass + ".Objecttype", config.getString(
                    "MCR.ClassificationBrowser." + browserClass + ".Doctype", null));
        } catch (final org.mycore.common.MCRConfigurationException noDoctype) {
            objectType = config.getString("MCR.ClassificationBrowser.default.ObjectType", config
                    .getString("MCR.ClassificationBrowser.default.Doctype"));
        }

        if (objectType != null) {
            objectTypeArray = objectType.split(",");
        } else {
            objectTypeArray = new String[0];
        }
    }

    public String getUri() {
        return uri;
    }

    /**
     * Returns the pageName for the classification
     */
    public String getPageName() {
        return pageName;
    }

    /**
     * Returns the xslStyle for the classification
     */
    public String getXslStyle() {
        return xslStyle;
    }

    public MCRCategory getClassification() {
        return rowsCreator.getClassification();
    }

    @SuppressWarnings("unchecked")
    public org.jdom.Document loadTreeIntoSite(final org.jdom.Document cover, final org.jdom.Document browser) {
        final Element placeholder = cover.getRootElement().getChild("classificationBrowser");
        LOGGER.info(" Found Entry at " + placeholder);
        if (placeholder != null) {
            final List<Element> children = browser.getRootElement().getChildren();
            for (Element child : children) {
                placeholder.addContent((Element) child.clone());
            }
        }
        LOGGER.debug(cover);
        return cover;
    }

    private void setClassification(final MCRCategoryID classifID) throws Exception {
        rowsCreator.setClassification(classifID);
    }

    private void clearPath(final String[] uriParts) throws Exception {
        final String[] cati = new String[uriParts.length];
        String path = "";
        if (uriParts.length == 1) {
            path = "/" + uriParts[0];
        } else {
            path = "/" + uriParts[1];
        }
        int len = 0;
        // pfad bereinigen
        for (int k = 2; k < uriParts.length; k++) {
            LOGGER.debug(" uriParts[k]=" + uriParts[k] + " k=" + k);
            if (uriParts[k].length() > 0) {
                if (uriParts[k].equalsIgnoreCase("..") && len > 0) {
                    len--;
                } else {
                    cati[len] = uriParts[k];
                    len++;
                }
            }
        }

        // remove double entries from path
        // (if an entry appears the 2nd time it will not be displayed -> so we
        // can remove it here)
        final ArrayList<String> result = new ArrayList<String>();
        for (int i = 0; i < len; i++) {
            final String x = cati[i];
            if (result.contains(x)) {
                result.remove(x);
            } else {
                result.add(x);
            }
        }

        // reinitialisieren
        categFields = new String[result.size()];
        int j = 0;
        for (String uriPart : result) {
            categFields[j] = uriPart;
            j++;
            path += "/" + uriPart;
        }
        this.uri = path;
    }

    private void setActualPath(final String actEditorCategid) throws Exception {
        actItemID = lastItemID = "";

        for (String categID : categFields) {
            update(categID);
            lastItemID = actItemID;
            actItemID = categID;
        }
        if (actEditorCategid != null) {
            actItemID = lastItemID = actEditorCategid;
        }
    }

    public Document createXmlTreeforAllClassifications() throws Exception {
        LOGGER.debug("create XML tree for all classifications");
        final Element xDocument = new Element("classificationbrowse");
        final Element CreateClassButton = new Element("userCanCreate");
        if (MCRAccessManager.checkPermission("create-classification")) {
            CreateClassButton.addContent("true");
        } else {
            CreateClassButton.addContent("false");
        }

        xDocument.addContent(CreateClassButton);

        final Element xNavtree = new Element("classificationlist");
        xDocument.addContent(xNavtree);

        LOGGER.debug("query classification links");
        Map<MCRCategoryID, Boolean> linkMap = linkService.hasLinks(null);
        
        for (MCRCategoryID classID : this.classificationPool.getAllIDs()) {
            MCRCategory classif = this.classificationPool.getClassificationAsPojo(classID, false);
            LOGGER.debug("get classification " + classID);
            LOGGER.debug("get browse element");

            Element cli = getBrowseElement(classif);

            setPermissions(linkMap, classID, cli);

            setDoneFlag(classID, cli);

            String browserClass = config.getString("MCR.classeditor." + classif.getId().getRootID(), "default");
            cli.setAttribute("browserClass", browserClass);
            setObjectTypes(browserClass);
            xNavtree.addContent(cli);
        }
        return new Document(xDocument);
    }

    private void setDoneFlag(MCRCategoryID classID, Element cli) {
        LOGGER.debug("get browse element ... done");
        String userEditedValue = "false";

        String rootID = classID.getRootID();
        String sessionID = MCRSessionMgr.getCurrentSession().getID();
        String classUser = ClassUserTable.get(rootID);
        
        if (classUser != null && !classUser.equals(sessionID)) {
            MCRSession oldsession = MCRSessionMgr.getSession(classUser);
            if (null != oldsession) {
                cli.setAttribute("userEdited", oldsession.getCurrentUserID());
            } else {
                ClassUserTable.remove(rootID);
            }
        }

        cli.setAttribute("userEdited", userEditedValue);
    }

    private void setPermissions(Map<MCRCategoryID, Boolean> linkMap, MCRCategoryID classID, Element cli) {
        String editedValue = "true";
        String userCanEditValue = "false";
        String userCanDeleteValue = "false";

        if (!this.classificationPool.isEdited(classID)) {
            editedValue = "false";
            if (MCRAccessManager.checkPermission(classID.getRootID(), "writedb")) {
                userCanEditValue = "true";
            }

            final boolean mayDelete = MCRAccessManager.checkPermission(classID.getRootID(), "deletedb");
            if (mayDelete) {
                userCanDeleteValue = "true";
                LOGGER.debug("counting linked objects");
                boolean hasLinks = linkMap.get(classID);
                LOGGER.debug("counting linked objects ... done");
                cli.setAttribute("hasLinks", String.valueOf(hasLinks));
            }
        }

        cli.setAttribute("edited", editedValue);
        cli.setAttribute("userCanEdit", userCanEditValue);
        cli.setAttribute("userCanDelete", userCanDeleteValue);
    }

    private static Element getBrowseElement(MCRCategory classif) {
        Element ce = new Element("classification");
        ce.setAttribute("ID", classif.getId().getRootID());
        for (MCRLabel label : classif.getLabels()) {
            ce.addContent(createLabelElement(label));
        }
        return ce;
    }

    private static Element createLabelElement(MCRLabel label) {
        Element labelElement = new Element("label");
        String lang = label.getLang();
        String text = label.getText();
        String description = label.getDescription();

        if (lang != null) {
            labelElement.setAttribute("lang", lang, Namespace.XML_NAMESPACE);
        }
        if (text != null) {
            labelElement.setAttribute("text", text);
        }
        if (description != null) {
            labelElement.setAttribute("description", description);
        }

        return labelElement;
    }

    /**
     * Creates an XML representation of MCRClassificationBrowserData
     * 
     * @author Anja Schaar
     * 
     */
    public Document createXmlTree(String lang) throws Exception {

        LOGGER.debug("Show tree for classification:" + getClassification().getId());
        LOGGER.debug("Got classification");
        MCRCategory classification = classificationPool.getClassificationAsPojo(getClassification().getId(), true);
        String rootID = classification.getId().getRootID();
        MCRLabel label = classification.getLabel(lang);

        if (label == null) {
            label = classification.getCurrentLabel();
        }

        Element docRoot = new Element("classificationBrowse");
        docRoot.addContent(createElement("classifID", rootID));
        docRoot.addContent(createUserEditedTag(rootID));
        docRoot.addContent(createSessionTag(rootID));
        docRoot.addContent(createElement("currentSession", MCRSessionMgr.getCurrentSession().getID()));
        docRoot.addContent(createElement("label", label.getText()));
        docRoot.addContent(createElement("description", label.getDescription()));
        docRoot.addContent(createElement("cntDocuments", String.valueOf(totalNumOfDocs)));
        docRoot.addContent(createElement("showComments", String.valueOf(showComments())));
        docRoot.addContent(createElement("uri", uri));
        docRoot.addContent(createElement("startPath", startPath));
        docRoot.addContent(createElement("searchField", searchField));

        createEditButtonsTag(docRoot, rootID);

        // data as XML from outputNavigationTree
        Element xNavtree = createNavigationTree(rootID);

        LOGGER.debug("process tree lines: build xml tree");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        rowsCreator.createRows(lang, xNavtree);
        stopWatch.stop();
        LOGGER.info("create rows time: " + stopWatch.getTime());

        LOGGER.debug("Building XML document");

        docRoot.addContent(xNavtree);

        if ("true".equals(sort)) {
            docRoot = sortMyTree(docRoot);
        }
        //MCRUtils.writeJDOMToSysout(doc);
        return new Document(docRoot);
    }

    private Element createNavigationTree(String rootID) {
        Element xNavtree = new Element("navigationtree");
        xNavtree.setAttribute("classifID", rootID);
        xNavtree.setAttribute("categID", actItemID);
        xNavtree.setAttribute("predecessor", lastItemID);
        xNavtree.setAttribute("emptyLeafs", emptyLeafs);
        xNavtree.setAttribute("view", view);

        xNavtree.setAttribute("doctype", createDoctypeString());
        xNavtree.setAttribute("restriction", restriction != null ? restriction : "");
        xNavtree.setAttribute("searchField", searchField);
        return xNavtree;
    }

    private String createDoctypeString() {
        StringBuffer sb = new StringBuffer();
        if (objectTypeArray.length > 1) {
            sb.append("(");
        }
        for (int i = 0; i < objectTypeArray.length; i++) {
            String str = "(objectType+=+" + objectTypeArray[i] + ")";
            sb.append(str);
            if ((objectTypeArray.length > 1) && (i < objectTypeArray.length - 1)) {
                sb.append("+or+");
            }
        }
        if (objectTypeArray.length > 1) {
            sb.append(")");
        }
        return sb.toString();
    }

    private void createEditButtonsTag(Element xDocument, String rootID) {
        LOGGER.debug("now we check this right for the current user");
        // now we check this right for the current user
        String createClassificationPerm = "true";
        String writedbPerm = "true";
        String deletedbPerm = "true";

        if (classificationPool.isEdited(getClassification().getId()) == false) {
            createClassificationPerm = String.valueOf(permissionTool.checkPermission("create-classification"));
            writedbPerm = String.valueOf(permissionTool.checkPermission(rootID, "writedb"));
            deletedbPerm = String.valueOf(permissionTool.checkPermission(rootID, "deletedb"));
        }

        xDocument.addContent(createElement("userCanCreate", createClassificationPerm));
        xDocument.addContent(createElement("userCanEdit", writedbPerm));
        xDocument.addContent(createElement("userCanDelete", deletedbPerm));
    }

    private Element createSessionTag(String rootID) {
        String classUser = "";
        if (ClassUserTable.containsKey(rootID)) {
            classUser = ClassUserTable.get(rootID);
        }

        return createElement("session", classUser);
    }

    private Element createUserEditedTag(String rootID) {
        String currentUserID;
        if (ClassUserTable.containsKey(rootID)) {
            currentUserID = MCRSessionMgr.getSession(ClassUserTable.get(rootID)).getCurrentUserID();
        } else {
            currentUserID = "false";
        }
        return createElement("userEdited", currentUserID);
    }

    private Element createElement(String tagName, String content) {
        Element xCurrentSessionID = new Element(tagName);
        xCurrentSessionID.addContent(content);
        return xCurrentSessionID;
    }

    public void update(final String categID) throws Exception {
        rowsCreator.update(categID);
    }

    /**
     * Returns true if category comments for the classification currently
     * displayed should be shown.
     */
    public boolean showComments() {
        return comments;
    }

    // don't use it works not really good

    private final Element sortMyTree(final Element xDocument) {
        Element xDoc = (Element) xDocument.clone();
        final Element navitree = ((Element) xDoc.getChild("navigationtree"));
        // separate
        ArrayList<String> itemname = new ArrayList<String>();
        ArrayList<Integer> itemlevel = new ArrayList<Integer>();
        ArrayList<Element> itemelm = new ArrayList<Element>();
        @SuppressWarnings("unchecked")
        List<Element> navitreelist = navitree.getChildren();
        for (int i = 0; i < navitreelist.size(); i++) {
            final Element child = navitreelist.get(i);
            final Element col1 = (Element) (child.getChildren().get(0));
            final Element col2 = (Element) (child.getChildren().get(1));
            final String sText = col2.getText();
            int level = 0;
            try {
                level = col1.getAttribute("lineLevel").getIntValue();
            } catch (final Exception ignored) {
            }
            itemname.add(sText);
            itemlevel.add(new Integer(level));
            itemelm.add((Element) child.clone());
            navitree.removeContent(child);
            i--;
        }
        int[] itemnum = new int[itemname.size()];
        for (int i = 0; i < itemname.size(); i++) {
            itemnum[i] = i;
        }
        // sort
        sortMyTreePerLevel(0, itemname.size(), 1, itemname, itemlevel, itemnum);
        // write back
        for (int i = 0; i < itemnum.length; i++) {
            navitree.addContent(itemelm.get(itemnum[i]));
        }
        return xDoc;
    }

    private final void sortMyTreePerLevel(int von, int bis, int level, ArrayList<String> itemname, ArrayList<Integer> itemlevel,
            int[] itemnum) {
        if (von == bis)
            return;
        // System.out.println("$$$>" + von + " " + bis);
        for (int i = von; i < bis - 1; i++) {
            // System.out.println("III>" + i + " " + itemname.get(itemnum[i]));
            if (itemlevel.get(itemnum[i]) != level) {
                // System.out.println("%%%> inner sort");
                int start = i;
                int stop = start;
                while (stop + 1 < bis && itemlevel.get(itemnum[stop + 1]).intValue() > level) {
                    stop++;
                }
                // System.out.println("--------------------");
                sortMyTreePerLevel(start, stop + 1, level + 2, itemname, itemlevel, itemnum);
                // System.out.println("--------------------");
                i = stop + 1;
                if (i >= bis)
                    continue;
            }
            String aktitem = itemname.get(itemnum[i]);
            for (int j = i + 1; j < bis; j++) {
                if (level != itemlevel.get(itemnum[j]).intValue())
                    continue;
                // System.out.println("%%%>" + i + " " + aktitem + " " + j + " "
                // + itemname.get(itemnum[j]) + " " + level + " " +
                // itemlevel.get(itemnum[j]).intValue());
                if (aktitem.compareTo(itemname.get(itemnum[j])) > 0) {
                    // must switch
                    // System.out.println("%%%> swap " + i + " with " + j);
                    int[] swap = itemnum.clone();
                    itemnum[i] = itemnum[j];
                    int ii = i;
                    for (int jj = j; jj < bis; jj++) {
                        // System.out.println("* " + ii + " " + jj);
                        itemnum[ii] = swap[jj];
                        ii++;
                    }
                    for (int jj = i; jj < j; jj++) {
                        // System.out.println("# " + ii + " " + jj);
                        itemnum[ii] = swap[jj];
                        ii++;
                    }
                    j = i;
                    aktitem = itemname.get(itemnum[i]);
                }
            }
        }
    }

    /**
     * @return Returns the pool.
     */
    public static MCRClassificationPool getClassificationPool() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        Object cp = session.get("MCRClassificationPool.instance");
        if (cp != null && cp instanceof MCRClassificationPool) {
            return (MCRClassificationPool) cp;
        }
        MCRClassificationPool classPool = new MCRClassificationPool();
        session.put("MCRClassificationPool.instance", classPool);
        return classPool;
    }

    public static void clearUserClassTable(MCRSession session) {
        final String curSessionID = session.getID();
        final Iterator<Map.Entry<String, String>> it = ClassUserTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            if (entry.getValue().equals(curSessionID)) {
                LOGGER.info("Release classification " + entry.getKey() + " lock.");
                it.remove();
            }
        }
    }
}
