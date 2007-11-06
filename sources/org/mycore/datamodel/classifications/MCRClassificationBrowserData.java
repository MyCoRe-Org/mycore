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

package org.mycore.datamodel.classifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Set;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;
import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassificationItem;
import org.mycore.datamodel.classifications.MCRClassificationObject;
import org.mycore.datamodel.classifications.MCRLabel;
import org.mycore.datamodel.classifications.MCRClassificationQuery;

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
    protected boolean showComments;

    protected String pageName;

    protected String xslStyle;

    protected String uri;

    private static MCRConfiguration config;

    private static final Logger LOGGER = Logger.getLogger(MCRClassificationBrowserData.class);

    private static final MCRAccessInterface AI = MCRAccessManager.getAccessImpl();

    // private Vector lines;
    private ArrayList lines;

    private MCRClassificationItem classif;

    private String startPath = "";

    private String actItemID = "";

    private String lastItemID = "";

    private String[] categFields;

    private String emptyLeafs = null;

    private String view = null;

    private String comments = null;

    private String searchField = "";

    private String sort = null;

    private String objectType = null;

    private String[] objectTypeArray = null;

    private String restriction = null;

    int maxlevel = 0;

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

    public MCRClassificationBrowserData(final String u, final String mode, final String actclid, final String actEditorCategid) throws Exception {
        uri = u;
        config = MCRConfiguration.instance();
        LOGGER.info(" incomming Path " + uri);

        final String[] uriParts = uri.split("/"); // mySplit();
        LOGGER.info(" Start");
        String classifID = null;
        final String browserClass = (uriParts.length <= 1 ? "default" : uriParts[1]);
        LOGGER.debug(" PathParts - classification " + browserClass);
        LOGGER.debug(" Number of PathParts =" + uriParts.length);
        try {
            classifID = config.getString("MCR.ClassificationBrowser." + browserClass + ".Classification");
        } catch (final org.mycore.common.MCRConfigurationException noClass) {
            classifID = actclid;
        }
        try {
            pageName = config.getString("MCR.ClassificationBrowser." + browserClass + ".EmbeddingPage");
        } catch (final org.mycore.common.MCRConfigurationException noPagename) {
            pageName = config.getString("MCR.ClassificationBrowser.default.EmbeddingPage");
        }
        try {
            xslStyle = config.getString("MCR.ClassificationBrowser." + browserClass + ".Style");
        } catch (final org.mycore.common.MCRConfigurationException noStyle) {
            xslStyle = config.getString("MCR.ClassificationBrowser.default.Style");
        }
        try {
            searchField = config.getString("MCR.ClassificationBrowser." + browserClass + ".searchField");
        } catch (final org.mycore.common.MCRConfigurationException noSearchfield) {
            searchField = config.getString("MCR.ClassificationBrowser.default.searchField");
        }

        try {
            emptyLeafs = config.getString("MCR.ClassificationBrowser." + browserClass + ".EmptyLeafs");
        } catch (final org.mycore.common.MCRConfigurationException noEmptyLeafs) {
            emptyLeafs = config.getString("MCR.ClassificationBrowser.default.EmptyLeafs");
        }
        try {
            view = config.getString("MCR.ClassificationBrowser." + browserClass + ".View");
        } catch (final org.mycore.common.MCRConfigurationException noView) {
            view = config.getString("MCR.ClassificationBrowser.default.View");
        }
        setObjectTypes(browserClass);
        try {
            sort = config.getString("MCR.ClassificationBrowser." + browserClass + ".Sort");
            comments = config.getString("MCR.ClassificationBrowser." + browserClass + ".Comments");
            restriction = config.getString("MCR.ClassificationBrowser." + browserClass + ".Restriction");
        } catch (final org.mycore.common.MCRConfigurationException ig) {
            // ignore for this parameters, the are optionally
            ;
        }
        startPath = browserClass;

        if ("edit".equals(mode)) {
            pageName = config.getString("MCR.classeditor.EmbeddingPage");
            xslStyle = config.getString("MCR.classeditor.Style");
            sort = "false";
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
        if (comments == null) {
            comments = "false";
        }
        LOGGER.info("uriParts length: " + uriParts.length);
        clearPath(uriParts);
        setClassification(classifID);
        setActualPath(actEditorCategid);

        showComments = comments.endsWith("true") ? true : false;

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
        try {
            // NOTE: read *.Doctype for compatiblity reasons
            objectType = config.getString("MCR.ClassificationBrowser." + browserClass + ".Objecttype", config.getString("MCR.ClassificationBrowser."
                    + browserClass + ".Doctype", null));
        } catch (final org.mycore.common.MCRConfigurationException noDoctype) {
            objectType = config.getString("MCR.ClassificationBrowser.default.ObjectType", config.getString("MCR.ClassificationBrowser.default.Doctype"));
        }

        if (objectType != null) {
            objectTypeArray = objectType.split(",");
        } else {
            objectTypeArray = new String[1];
            objectTypeArray[0] = "document";
            LOGGER.warn("No object type was found - document was set");
        }
    }

    public String getUri() {
        return uri;
    }

    /**
     * Returns true if category comments for the classification currently
     * displayed should be shown.
     */
    public boolean showComments() {
        return showComments;
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

    public MCRClassificationItem getClassification() {
        return classif;
    }

    public org.jdom.Document loadTreeIntoSite(final org.jdom.Document cover, final org.jdom.Document browser) {
        final Element placeholder = cover.getRootElement().getChild("classificationBrowser");
        LOGGER.info(" Found Entry at " + placeholder);
        if (placeholder != null) {
            final List children = browser.getRootElement().getChildren();
            for (int j = 0; j < children.size(); j++) {
                final Element child = (Element) ((Element) children.get(j)).clone();
                placeholder.addContent(child);
            }
        }
        LOGGER.debug(cover);
        return cover;
    }

    private final void setClassification(final String classifID) throws Exception {
        classif = getClassificationPool().getClassificationAsPojo(classifID);
        lines = new ArrayList();
        totalNumOfDocs = 0;
        putCategoriesintoLines(-1, classif.getCategories(), 1);
        LOGGER.debug("Arraylist of CategItems initialized - Size " + lines.size());
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
        final TreeSet<String> result = new TreeSet<String>();
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
        final Iterator it = result.iterator();
        while (it.hasNext()) {
            final String s = (String) it.next();
            categFields[j] = s;
            j++;
            path += "/" + s;
        }
        this.uri = path;
    }

    private void setActualPath(final String actEditorCategid) throws Exception {
        actItemID = lastItemID = "";
        for (String element : categFields) {
            update(element);
            lastItemID = actItemID;
            actItemID = element;
        }
        if (actEditorCategid != null) {
            actItemID = lastItemID = actEditorCategid;
        }
    }

    private Element setTreeline(final Element cat, final int level) {
        cat.setAttribute("level", String.valueOf(level));
        if (cat.getChildren("category").size() != 0) {
            cat.setAttribute("hasChildren", "T");
        } else {
            cat.setAttribute("hasChildren", " ");
        }
        return cat;
    }

    private Element getTreeline(final int i) {
        if (i >= lines.size()) {
            return null;
        }
        return (Element) lines.get(i);
    }

    @SuppressWarnings("unchecked")
    private void putCategoriesintoLines(final int startpos, final List<MCRCategoryItem> children, final int level) {
        LOGGER.debug("Start Explore Arraylist of CategItems  ");
        int i = startpos;
        for (int j = 0, k = children.size(); j < k; j++) {
            MCRCategoryItem categ = (MCRCategoryItem) children.get(j);
            Element child = MCRCategoryElementFactory.getCategoryElement(categ, classif.isCounterEnabled());
            lines.add(++i, setTreeline(child, level + 1));
            if (startpos == -1) {
                totalNumOfDocs += categ.getNumberOfObjects();
            }
        }
        LOGGER.debug("End Explore - Arraylist of CategItems ");
    }

    public org.jdom.Document createXmlTreeforAllClassifications() throws Exception {

        final Element xDocument = new Element("classificationbrowse");
        final Element CreateClassButton = new Element("userCanCreate");
        if (AI.checkPermission("create-classification")) {
            CreateClassButton.addContent("true");
        } else {
            CreateClassButton.addContent("false");
        }

        xDocument.addContent(CreateClassButton);

        final Element xNavtree = new Element("classificationlist");
        xDocument.addContent(xNavtree);
        String browserClass = "";
        String Counter = "";

        final Set<String> allIDs = getClassificationPool().getAllIDs();
        List<String> ids = new ArrayList<String>(allIDs.size());
        ids.addAll(allIDs);
        Collections.sort(ids);

        for (String id : ids) {
            MCRClassificationItem classif = getClassificationPool().getClassificationAsPojo(id);
            Element cli = getBrowseElement(classif);
            String sessionID = MCRSessionMgr.getCurrentSession().getID();
            // set browser type
            try {
                browserClass = config.getString("MCR.classeditor." + classif.getId());
            } catch (final Exception ignore) {
                browserClass = "default";
            }
            // set permissions
            if (getClassificationPool().isEdited(id) == false) {
                if (AI.checkPermission(id, "writedb")) {
                    cli.setAttribute("userCanEdit", "true");
                } else {
                    cli.setAttribute("userCanEdit", "false");
                }
                if (AI.checkPermission(id, "deletedb")) {
                    cli.setAttribute("userCanDelete", "true");
                } else {
                    cli.setAttribute("userCanDelete", "false");
                }
            } else {
                cli.setAttribute("userCanEdit", "true");
                cli.setAttribute("userCanDelete", "true");
            }
            // set done flag
            if (ClassUserTable.containsKey(id)) {
                if (ClassUserTable.get(id) != sessionID) {
                    MCRSession oldsession = MCRSessionMgr.getSession(ClassUserTable.get(id));
                    if (null != oldsession)
                        cli.setAttribute("userEdited", oldsession.getCurrentUserID());
                    else {
                        ClassUserTable.remove(id);
                        cli.setAttribute("userEdited", "false");
                    }
                } else {
                    cli.setAttribute("userEdited", "false");
                }
            } else {
                cli.setAttribute("userEdited", "false");
            }

            if (getClassificationPool().isEdited(id)) {
                cli.setAttribute("edited", "true");
            } else {
                cli.setAttribute("edited", "false");
            }
            cli.setAttribute("browserClass", browserClass);
            setObjectTypes(browserClass);

            try {
                int cnt = 0;
                for (MCRCategoryItem cat : classif.getCategories()) {
                    cnt += cat.getNumberOfObjects();
                }
                Counter = Integer.toString(cnt);
            } catch (Exception ignore) {
                Counter = "NaN";
            }
            cli.setAttribute("counter", Counter);
            xNavtree.addContent(cli);
        }
        return new Document(xDocument);
    }

    private static Element getBrowseElement(MCRClassificationItem classif) {
        Element ce = new Element("classification");
        ce.setAttribute("ID", classif.getId());
        for (MCRLabel label : classif.getLabels()) {
            Element labelElement = new Element("label");
            if (label.getLang() != null) {
                labelElement.setAttribute("lang", label.getLang(), Namespace.XML_NAMESPACE);
            }
            if (label.getText() != null) {
                labelElement.setAttribute("text", label.getText());
            }
            if (label.getDescription() != null) {
                labelElement.setAttribute("description", label.getDescription());
            }
            ce.addContent(labelElement);
        }
        return ce;
    }

    /**
     * Creates an XML representation of MCRClassificationBrowserData
     * 
     * @author Anja Schaar
     * 
     */

    public org.jdom.Document createXmlTree(final String lang) throws Exception {

        // final MCRClassificationItem cl =
        // getClassificationPool().getClassificationAsPojo(getClassification().getId());
        MCRClassificationPool cp = getClassificationPool();
        MCRClassificationItem cl = cp.getClassificationAsPojo(getClassification().getId());
        MCRLabel labels = getLabel(cl, lang);
        Element xDocument = new Element("classificationBrowse");

        final Element xID = new Element("classifID");
        xID.addContent(cl.getId());
        xDocument.addContent(xID);

        final Element xUserEdited = new Element("userEdited");
        if (ClassUserTable.containsKey(cl.getId())) {
            xUserEdited.addContent(MCRSessionMgr.getSession(ClassUserTable.get(cl.getId())).getCurrentUserID());
        } else {
            xUserEdited.addContent("false");
        }
        xDocument.addContent(xUserEdited);

        final Element xSessionID = new Element("session");
        if (ClassUserTable.containsKey(cl.getId())) {
            xSessionID.addContent(ClassUserTable.get(cl.getId()));
        } else {
            xSessionID.addContent("");
        }

        xDocument.addContent(xSessionID);

        final Element xCurrentSessionID = new Element("currentSession");
        xCurrentSessionID.addContent(MCRSessionMgr.getCurrentSession().getID());
        xDocument.addContent(xCurrentSessionID);

        final Element xLabel = new Element("label");
        xLabel.addContent(labels.getText());
        xDocument.addContent(xLabel);

        final Element xDesc = new Element("description");
        xDesc.addContent(labels.getDescription());
        xDocument.addContent(xDesc);

        final Element xDocuments = new Element("cntDocuments");
        xDocuments.addContent(String.valueOf(totalNumOfDocs));
        xDocument.addContent(xDocuments);

        final Element xShowComments = new Element("showComments");
        xShowComments.addContent(String.valueOf(showComments()));
        xDocument.addContent(xShowComments);

        final Element xUri = new Element("uri");
        xUri.addContent(uri);
        xDocument.addContent(xUri);

        final Element xStartPath = new Element("startPath");
        xStartPath.addContent(startPath);
        xDocument.addContent(xStartPath);

        final Element xSearchField = new Element("searchField");
        xSearchField.addContent(searchField);
        xDocument.addContent(xSearchField);

        // add edit button if user has permission
        final Element CreateButton = new Element("userCanCreate");
        final Element EditButton = new Element("userCanEdit");
        final Element DeleteButton = new Element("userCanDelete");

        // now we check this right for the current user
        if (cp.isEdited(getClassification().getId()) == false) {
            String permString = String.valueOf(AI.checkPermission("create-classification"));
            CreateButton.addContent(permString);
            xDocument.addContent(CreateButton);
            permString = String.valueOf(AI.checkPermission(cl.getId(), "writedb"));
            EditButton.addContent(permString);
            xDocument.addContent(EditButton);
            permString = String.valueOf(AI.checkPermission(cl.getId(), "deletedb"));
            DeleteButton.addContent(permString);
            xDocument.addContent(DeleteButton);
        } else {
            String permString = "true";
            CreateButton.addContent(permString);
            xDocument.addContent(CreateButton);
            EditButton.addContent(permString);
            xDocument.addContent(EditButton);
            DeleteButton.addContent(permString);
            xDocument.addContent(DeleteButton);
        }

        // data as XML from outputNavigationTree
        final Element xNavtree = new Element("navigationtree");
        xNavtree.setAttribute("classifID", cl.getId());
        xNavtree.setAttribute("categID", actItemID);
        xNavtree.setAttribute("predecessor", lastItemID);
        xNavtree.setAttribute("emptyLeafs", emptyLeafs);
        xNavtree.setAttribute("view", view);
        final StringBuffer sb = new StringBuffer();
        if (objectTypeArray.length > 1) {
            sb.append("(");
        }
        for (int i = 0; i < objectTypeArray.length; i++) {
            sb.append("(objectType+=+").append(objectTypeArray[i]).append(")");
            if ((objectTypeArray.length > 1) && (i < objectTypeArray.length - 1)) {
                sb.append("+or+");
            }
        }
        if (objectTypeArray.length > 1) {
            sb.append(")");
        }
        xNavtree.setAttribute("doctype", sb.toString());
        xNavtree.setAttribute("restriction", restriction != null ? restriction : "");
        xNavtree.setAttribute("searchField", searchField);

        int i = 0;
        Element line;
        while ((line = getTreeline(i++)) != null) {

            final String catid = line.getAttributeValue("ID");
            int numDocs = 0;
            if (line.getAttributeValue("counter") != null) {
                LOGGER.info("COUNTER ATTRIBUTE: " + line.getAttributeValue("counter"));
                numDocs = Integer.parseInt(line.getAttributeValue("counter"));
            }
            final String status = line.getAttributeValue("hasChildren");

            Element label = (Element) XPath.selectSingleNode(line, "label[lang('" + lang + "')]");
            if (label == null) {
                label = (Element) XPath.selectSingleNode(line, "label");
            }
            final String text = label.getAttributeValue("text");
            final String description = label.getAttributeValue("description");

            final int level = Integer.parseInt(line.getAttributeValue("level"));

            // f�r Sortierung schon mal die leveltiefe bestimmen
            LOGGER.debug(" NumDocs - " + numDocs);

            if (emptyLeafs.endsWith("no") && numDocs == 0) {
                LOGGER.debug(" empty Leaf continue - " + emptyLeafs);
                continue;
            }
            final Element xRow = new Element("row");
            final Element xCol1 = new Element("col");
            final Element xCol2 = new Element("col");
            final int numLength = String.valueOf(numDocs).length();

            xRow.addContent(xCol1);
            xRow.addContent(xCol2);
            xNavtree.addContent(xRow);

            xCol1.setAttribute("lineLevel", String.valueOf(level - 1));
            xCol1.setAttribute("childpos", "middle");

            if (level > maxlevel) {
                xCol1.setAttribute("childpos", "first");
                maxlevel = level;
                if (getTreeline(i) == null) {
                    // Spezialfall nur genau ein Element
                    xCol1.setAttribute("childpos", "firstlast");
                }
            } else if (getTreeline(i) == null) {
                xCol1.setAttribute("childpos", "last");
            }

            xCol1.setAttribute("folder1", "folder_plain");
            xCol1.setAttribute("folder2", numDocs > 0 ? "folder_closed_in_use" : "folder_closed_empty");

            if (status.equals("T")) {
                xCol1.setAttribute("plusminusbase", catid);
                xCol1.setAttribute("folder1", "folder_plus");
            } else if (status.equals("F")) {
                xCol1.setAttribute("plusminusbase", catid);
                xCol1.setAttribute("folder1", "folder_minus");
                xCol1.setAttribute("folder2", numDocs > 0 ? "folder_open_in_use" : "folder_open_empty");
            }

            xCol2.setAttribute("numDocs", String.valueOf(numDocs));
            final String fmtnumDocs = fillToConstantLength(String.valueOf(numDocs), " ", 6);
            xCol2.setAttribute("fmtnumDocs", fmtnumDocs);

            if (numLength > 0) {
                String search = uri;
                if (catid.equalsIgnoreCase(actItemID)) {
                    search += "/..";
                } else {
                    search += "/" + catid;
                }

                if (search.indexOf("//") > 0)
                    search = search.substring(0, search.indexOf("//")) + search.substring(search.indexOf("//") + 1);

                xCol2.setAttribute("searchbase", search);
                xCol2.setAttribute("lineID", catid);
            }

            xCol2.addContent(text);

            if (showComments() && (description != null)) {
                final Element comment = new Element("comment");
                xCol2.addContent(comment);
                comment.setText(description);
            }
        }

        xNavtree.setAttribute("rowcount", "" + i);
        xDocument.addContent(xNavtree);

        if ("true".equals(sort)) {
            xDocument = sortMyTree(xDocument);
        }
        Document doc = new org.jdom.Document(xDocument);
        return doc;
    }

    public void update(final String categID) throws Exception {
        int lastLevel = 0;
        boolean hideLevel = false;

        MCRCategoryItem cat = MCRClassificationQuery.findCategory(classif, categID);
        LOGGER.debug(this.getClass() + " update CategoryTree for: " + categID);
        Element line;
        for (int i = 0; i < lines.size(); i++) {
            line = getTreeline(i);
            final String catid = line.getAttributeValue("ID");
            final String status = line.getAttributeValue("hasChildren");
            final int level = Integer.parseInt(line.getAttributeValue("level"));

            hideLevel = hideLevel && (level > lastLevel);
            LOGGER.debug(" compare CategoryTree on " + i + "_" + catid + " to " + categID);

            if (view.endsWith("tree")) {
                if (hideLevel) {
                    lines.remove(i--);
                } else if (categID.equals(catid)) {
                    if (status.equals("F")) // hide expanded category - //
                    // children
                    {
                        line.setAttribute("hasChildren", "T");
                        hideLevel = true;
                        lastLevel = level;
                    } else if (status.equals("T")) // expand category - //
                    // children
                    {
                        line.setAttribute("hasChildren", "F");

                        putCategoriesintoLines(i, cat.getCategories(), level + 1);
                    }
                }
            } else {
                if (categID.equalsIgnoreCase(catid)) {
                    line.setAttribute("level", "0");
                    LOGGER.info(" expand " + catid);
                    line.setAttribute("hasChildren", "F");
                    putCategoriesintoLines(i, new MCRCategoryItem().getCategories(), level + 1);
                } else {
                    LOGGER.debug(" remove lines " + i + "_" + catid);
                    lines.remove(i--);
                }
            }

        }
    }

    // don't use it works not really good

    private final Element sortMyTree(final Element xDocument) {
        Element xDoc = (Element) xDocument.clone();
        final Element navitree = ((Element) xDoc.getChild("navigationtree"));
        // separate
        ArrayList<String> itemname = new ArrayList<String>();
        ArrayList<Integer> itemlevel = new ArrayList<Integer>();
        ArrayList<Element> itemelm = new ArrayList<Element>();
        List navitreelist = navitree.getChildren();
        for (int i = 0; i < navitreelist.size(); i++) {
            final Element child = (Element) (navitreelist.get(i));
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
        // debug
        // for (int i = 0; i < itemnum.length; i++) {
        // System.out.println("===> " + i + " " + itemnum[i] + " " +
        // itemname.get(itemnum[i]));
        // }
        // sort
        sortMyTreePerLevel(0, itemname.size(), 1, itemname, itemlevel, itemnum);
        // debug
        // for (int i = 0; i < itemnum.length; i++) {
        // System.out.println("---> " + i + " " + itemnum[i] + " " +
        // itemname.get(itemnum[i]));
        // }
        // write back
        for (int i = 0; i < itemnum.length; i++) {
            navitree.addContent(itemelm.get(itemnum[i]));
        }
        return xDoc;
    }

    private final void sortMyTreePerLevel(int von, int bis, int level, ArrayList<String> itemname, ArrayList<Integer> itemlevel, int[] itemnum) {
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

    private static String fillToConstantLength(final String value, final String fillsign, final int length) {
        final int valueLength = value.length();
        if (valueLength >= length)
            return value;
        final StringBuffer ret = new StringBuffer("");
        for (int i = 0; i < length - valueLength; i++) {
            ret.append(fillsign);
        }
        ret.append(value);
        return ret.toString();
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

    private static MCRLabel getLabel(MCRClassificationObject co, String lang) {
        for (MCRLabel label : co.getLabels()) {
            if (label.getLang().equals(lang)) {
                return label;
            }
        }
        return new MCRLabel();
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

    private static class MCRCategoryElementFactory {
        static Element getCategoryElement(MCRCategoryItem category, boolean withCounter) {
            Element ce = new Element("category");
            ce.setAttribute("ID", category.getId());
            if (withCounter) {
                ce.setAttribute("counter", Integer.toString(category.getNumberOfObjects()));
            }
            for (MCRLabel label : category.getLabels()) {
                ce.addContent(getElement(label));
            }
            for (MCRCategoryItem child : category.getCategories()) {
                ce.addContent(getCategoryElement(child, withCounter));
            }
            return ce;
        }

        private static Element getElement(MCRLabel label) {
            Element le = new Element("label");
            if (stringNotEmpty(label.getLang())) {
                le.setAttribute("lang", label.getLang(), Namespace.XML_NAMESPACE);
            }
            if (stringNotEmpty(label.getText())) {
                le.setAttribute("text", label.getText());
            }
            if (stringNotEmpty(label.getDescription())) {
                le.setAttribute("description", label.getDescription());
            }
            return le;
        }

        private static boolean stringNotEmpty(String test) {
            if (test != null && test.length() > 0) {
                return true;
            }
            return false;
        }
    }

}
