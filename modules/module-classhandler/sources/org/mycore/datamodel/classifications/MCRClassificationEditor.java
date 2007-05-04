/**
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.datamodel.classifications;

import static org.jdom.Namespace.XML_NAMESPACE;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassificationItem;
import org.mycore.datamodel.classifications.MCRClassificationObject;
import org.mycore.datamodel.classifications.MCRClassificationTransformer;
import org.mycore.datamodel.classifications.MCRLabel;
import org.mycore.datamodel.classifications.MCRClassificationQuery;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class implements all methods for a edit, modify delete categories in
 * classification and the classification itself
 * 
 * @author Anja Schaar
 * @author Jens Kupferschmidt
 * @version
 */

public class MCRClassificationEditor {

    // logger
    private static Logger LOGGER = Logger.getLogger(MCRClassificationEditor.class);

    private MCRClassificationItem classif;

    private MCRConfiguration CONFIG;

    private File fout;

    public MCRClassificationEditor() {
        CONFIG = MCRConfiguration.instance();
    }

    /**
     * Create an new category in the category path.
     * 
     * @param indoc
     *            the output from the editor dialogue
     * @param clid
     *            the classification ID
     * @param categid
     *            the category ID to add after it
     * @return
     */
    public boolean createCategoryInClassification(org.jdom.Document indoc, String clid, String categid) {

        try {
            LOGGER.debug("Create a new category in classification " + clid + " after categid " + categid);
            Element clroot = indoc.getRootElement();
            if (clroot == null) {
                return false;
            }
            Element categories = (Element) clroot.getChild("categories");
            if (categories == null)
                return false;

            Element newCateg = (Element) categories.getChild("category").clone();
            String newID = newCateg.getAttributeValue("ID");
            classif = MCRClassificationBrowserData.getClassificationPool().getClassificationAsPojo(clid);

            // check the new category entry
            if (newID.equalsIgnoreCase(categid)) {
                LOGGER.error("The category ID's are not different.");
                return false;
            }

            newCateg = setNewJDOMCategElement(newCateg);
            newCateg.setAttribute("counter", "0");
            if (MCRClassificationQuery.findCategory(classif, newID) == null) {
                MCRCategoryItem newCategory = MCRClassificationTransformer.getCategory(newCateg);
                List<MCRCategoryItem> categs = classif.getCategories();
                categs.add(newCategory);
                classif.setCatgegories(categs);
                MCRClassificationBrowserData.getClassificationPool().updateClassification(classif);
                String sessionID = MCRSessionMgr.getCurrentSession().getID();
                MCRClassificationBrowserData.ClassUserTable.put(classif.getId(), sessionID);
                return true;
            }
            return false;
        } catch (Exception e1) {
            e1.printStackTrace();
            LOGGER.error("Classification creation fails. Reason is:" + e1.getMessage());
            return false;
        }

    }

    /**
     * Replace category data like label(s) and url.
     * 
     * @param indoc
     *            the output from the editor dialogue
     * @param clid
     *            the classification ID
     * @param categid
     *            the category ID
     * @return true if all it's okay, else return false
     */
    public final boolean modifyCategoryInClassification(org.jdom.Document indoc, String clid, String categid) {
        try {
            LOGGER.debug("Start modify category in classification " + clid + " with categid " + categid);
            Element clroot = indoc.getRootElement();
            Element newCateg = (Element) clroot.getChild("categories").getChild("category").clone();
            String newID = newCateg.getAttributeValue("ID");
            classif = MCRClassificationBrowserData.getClassificationPool().getClassificationAsPojo(clid);
            newCateg = setNewJDOMCategElement(newCateg);
            // check the category entry
            if (!newID.equalsIgnoreCase(categid)) {
                LOGGER.error("The category ID's are different.");
                return false;
            }
            MCRCategoryItem oldCategory = MCRClassificationQuery.findCategory(classif, newID);
            if (oldCategory == null) {
                LOGGER.error("The category ID does not exist in classification " + clid);
                return false;
            }
            MCRCategoryItem newCategory = MCRClassificationTransformer.getCategory(newCateg);
            oldCategory.getLabels().clear();
            oldCategory.setLabels(newCategory.getLabels());

            MCRClassificationBrowserData.getClassificationPool().updateClassification(classif);
            String sessionID = MCRSessionMgr.getCurrentSession().getID();
            MCRClassificationBrowserData.ClassUserTable.put(classif.getId(), sessionID);

            return true;
        } catch (Exception e1) {
            LOGGER.error("Category modify fails. Reason is:");
            e1.printStackTrace();
            return false;
        }
    }

    public boolean isLocked(String classid) {
        if (MCRClassificationBrowserData.ClassUserTable.containsKey(classid)) {
            String lockedSessionID = MCRClassificationBrowserData.ClassUserTable.get(classid);
            String currentSessionID = MCRSessionMgr.getCurrentSession().getID();
            if (!lockedSessionID.equals(currentSessionID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create or update a classification form import.
     * 
     * @param bUpdate
     *            true if this operation should be a update, else false
     * @param sFile
     *            the name of classification file
     * @return true if all it's okay, else return false
     */
    public boolean importClassification(boolean bUpdate, String sFile) {
        LOGGER.debug("Start importNewClassification.");
        String clid = "";
        try {
            try {
                LOGGER.info("Reading file " + sFile + " ...\n");
                MCRClassification cl = new MCRClassification();
                cl.setFromURI(sFile);
                if (bUpdate) {
                    cl.updateInDatastore();
                    LOGGER.info("Classification: " + cl.getId() + " successfully imported with update!");
                } else {
                    cl.createInDatastore();
                    LOGGER.info("Classification: " + cl.getId() + " successfully imported with create!");
                }
                return true;
            } catch (MCRException ex) {
                LOGGER.error("Exception while loading from file " + sFile, ex);
                return false;
            }
        } catch (Exception e1) {
            LOGGER.error("Classification import fails. Reason is:" + e1.getMessage());
            return false;
        }

    }

    /**
     * Create a new classification with the data from the editor dialogue.
     * 
     * @param indoc
     *            the output from the editor dialogue
     * @return true if all it's okay, else return false
     */
    public final boolean createNewClassification(org.jdom.Document indoc) {
        try {
            LOGGER.debug("Start create a  new classification.");
            Element clroot = indoc.getRootElement();

            Element mycoreclass = new Element("mycoreclass");
            Element categories = new Element("categories");
            Element category = new Element("category");
            category.setAttribute("ID", "empty");
            Element label = new Element("label");
            label.setAttribute("text", "empty");
            label.setAttribute("description", "empty");
            category.addContent(label);
            categories.addContent(category);

            String submittedID = indoc.getRootElement().getAttributeValue("ID");
            MCRObjectID cli = new MCRObjectID();
            if (submittedID != null) {
                cli.setID(submittedID);
            }
            if (submittedID == null || !cli.isValid()) {

                String base = CONFIG.getString("MCR.default_project_id", "DocPortal") + "_class";

                LOGGER.debug("Create a CLID with base " + base);
                cli.setNextFreeId(base);

                if (!cli.isValid()) {
                    LOGGER.error("Create an unique CLID failed. " + cli.toString());
                    return false;
                }
            }

            mycoreclass.addNamespaceDeclaration(XSI_NAMESPACE);
            mycoreclass.setAttribute("noNamespaceSchemaLocation", "MCRClassification.xsd", XSI_NAMESPACE);
            mycoreclass.setAttribute("ID", cli.toString());
            mycoreclass.setAttribute("counter", "0");
            List tagList = clroot.getChildren("label");
            Element element;
            for (int i = 0; i < tagList.size(); i++) {
                element = (Element) tagList.get(i);
                Element newE = new Element("label");
                newE.setAttribute("lang", element.getAttributeValue("lang"), XML_NAMESPACE);
                newE.setAttribute("text", element.getAttributeValue("text"));
                if (element.getAttributeValue("description") != null) {
                    newE.setAttribute("description", element.getAttributeValue("description"));
                }
                mycoreclass.addContent(newE);
            }
            mycoreclass.addContent(categories);
            Document cljdom = new Document();
            cljdom.addContent(mycoreclass);
            MCRClassificationItem classif = MCRClassificationTransformer.getClassification(cljdom);
            MCRClassificationBrowserData.getClassificationPool().updateClassification(classif);
            LOGGER.debug("Classification " + cli.toString() + " successfully created!");
            return true;

        } catch (Exception e1) {
            LOGGER.error("Classification creation fails. Reason is:" + e1.getMessage());
            return false;
        }
    }

    /**
     * Change the description data of a classification.
     * 
     * @param indoc
     *            the output from the editor dialogue
     * @param clid
     *            the classification ID
     * @return true if all it's okay, else return false
     */
    public final boolean modifyClassificationDescription(org.jdom.Document indoc, String clid) {
        try {
            LOGGER.debug("Start modify classification description for " + clid);
            Element clroot = indoc.getRootElement();
            classif = MCRClassificationBrowserData.getClassificationPool().getClassificationAsPojo(clid);
            Element element;
            List tagList = clroot.getChildren("label");

            classif.getLabels().clear();

            for (int i = 0; i < tagList.size(); i++) {
                element = (Element) tagList.get(i);
                MCRLabel label = new MCRLabel();
                label.setLang(element.getAttributeValue("lang"));
                label.setText(element.getAttributeValue("text"));
                label.setDescription(element.getAttributeValue("description"));
                classif.getLabels().add(label);
            }

            MCRClassificationBrowserData.getClassificationPool().updateClassification(classif);
            String sessionID = MCRSessionMgr.getCurrentSession().getID();
            MCRClassificationBrowserData.ClassUserTable.put(classif.getId(), sessionID);
            return true;
        } catch (Exception e1) {
            LOGGER.error("Classification modify fails. Reason is:" + e1.getMessage());
            return false;
        }
    }

    /**
     * Move a category in a classification.
     * 
     * @param categid
     *            the category ID
     * @param clid
     *            the classification ID
     * @param way
     *            the way to move
     * @return true if all it's okay, else return false
     */
    public boolean moveCategoryInClassification(String categid, String clid, String way) {
        try {
            LOGGER.debug("Start move in classification " + clid + " the category " + categid + " in direction: " + way);
            boolean bret = false;

            classif = MCRClassificationBrowserData.getClassificationPool().getClassificationAsPojo(clid);
            if (way.equalsIgnoreCase("up")) {
                MCRCategoryItem categ = MCRClassificationQuery.findCategory(classif, categid);
                moveUp(categ);
                bret = true;
            } else if (way.equalsIgnoreCase("down")) {
                MCRCategoryItem categ = MCRClassificationQuery.findCategory(classif, categid);
                moveDown(categ);
                bret = true;
            } else if (way.equalsIgnoreCase("right")) {
                MCRCategoryItem categ = MCRClassificationQuery.findCategory(classif, categid);
                moveRight(categ);
                bret = true;
            } else if (way.equalsIgnoreCase("left")) {
                MCRCategoryItem categ = MCRClassificationQuery.findCategory(classif, categid);
                moveLeft(categ);
                bret = true;
            }

            if (bret) {
                MCRClassificationBrowserData.getClassificationPool().updateClassification(classif);
                String sessionID = MCRSessionMgr.getCurrentSession().getID();
                MCRClassificationBrowserData.ClassUserTable.put(classif.getId(), sessionID);
                bret = true;
            }
            return bret;
        } catch (Exception e1) {
            LOGGER.error("Classification modify failed - the Reason is:" + e1.getMessage(), e1);
            return false;
        }
    }

    private boolean moveUp(MCRCategoryItem cat) {
        MCRClassificationObject parent = MCRClassificationQuery.findParent(classif, cat);
        int index = parent.getCategories().indexOf(cat);
        if (index > 0) {
            parent.getCategories().remove(index);
            parent.getCategories().add(index - 1, cat);
            return true;
        } else {
            return false;
        }
    }

    private boolean moveDown(MCRCategoryItem cat) {
        MCRClassificationObject parent = MCRClassificationQuery.findParent(classif, cat);
        int index = parent.getCategories().indexOf(cat);
        if (index < parent.getCategories().size()) {
            parent.getCategories().remove(index);
            parent.getCategories().add(index + 1, cat);
            return true;
        } else {
            return false;
        }
    }

    private boolean moveRight(MCRCategoryItem cat) {
        MCRClassificationObject parent = MCRClassificationQuery.findParent(classif, cat);
        if (parent.getCategories().size() == 1) {
            return false;
        }
        int index = parent.getCategories().indexOf(cat);
        parent.getCategories().remove(index);
        if (index > 0) {
            index--;
        }
        parent.getCategories().get(index).getCategories().add(cat);
        return true;
    }

    private boolean moveLeft(MCRCategoryItem cat) {
        MCRClassificationObject parent = MCRClassificationQuery.findParent(classif, cat);
        if (!(parent instanceof MCRCategoryItem) || parent == null) {
            return false;
        }
        MCRClassificationObject grandParent = MCRClassificationQuery.findParent(classif, (MCRCategoryItem) parent);
        if (grandParent == null) {
            grandParent = classif;
        }
        int oldIndex = parent.getCategories().indexOf(cat);
        parent.getCategories().remove(oldIndex);
        int newIndex = grandParent.getCategories().indexOf(parent) + 1;
        grandParent.getCategories().add(newIndex, cat);
        return true;

    }

    public boolean saveAll() {
        final boolean saveAll = MCRClassificationBrowserData.getClassificationPool().saveAll();
        MCRClassificationBrowserData.clearCurrentUserClassTable();
        return saveAll;
    }

    public boolean purgeAll() {
        MCRClassificationBrowserData.clearCurrentUserClassTable();
        return MCRClassificationBrowserData.getClassificationPool().purgeAll();
    }

    /**
     * Delete a category from a classification.
     * 
     * @param clid
     *            the classification ID
     * @param categid
     *            the category ID
     * @return true if all it's okay, else return false
     */
    public final int deleteCategoryInClassification(String clid, String categid) {
        try {
            LOGGER.debug("Start delete in classification " + clid + " the category: " + categid);
            boolean bret = false;
            int cnt = 0;

            classif = MCRClassificationBrowserData.getClassificationPool().getClassificationAsPojo(clid);

            if (bret) {

                MCRCategoryItem clc = new MCRCategoryItem();
                clc.setId(categid);
                clc.setClassID(clid);
                clc.setParentID(null);

                MCRLinkTableManager mcl = MCRLinkTableManager.instance();
                if (clc.getClassID().equals(clc.getId())) {
                    cnt = mcl.countReferenceCategory(clc.getClassID(), "", null, null);
                } else {
                    cnt = mcl.countReferenceCategory(clc.getClassID(), clc.getId(), null, null);
                }
                if (cnt == 0) {
                    MCRClassificationBrowserData.getClassificationPool().updateClassification(classif);
                    String sessionID = MCRSessionMgr.getCurrentSession().getID();
                    MCRClassificationBrowserData.ClassUserTable.put(classif.getId(), sessionID);
                } else {
                    LOGGER.error("Category " + categid + " in classification " + clid + " can't be deleted, there are " + cnt + "refernces of documents to this");
                }
            } else {
                LOGGER.warn("Category " + categid + " in classification: " + clid + " not found! - nothing todo");
            }
            // 0 ist ok
            return cnt;
        } catch (Exception e1) {
            LOGGER.error("Categorie delete failed - the Reason is:" + e1.getMessage());
            return 1;
        }
    }

    /**
     * Delete a classification from the system.
     * 
     * @param clid
     *            the classification ID
     * @return
     */
    public final boolean deleteClassification(String clid) {
        LOGGER.debug("Start delete classification " + clid);
        try {
            MCRObjectID mcrclid = new MCRObjectID(clid);
            MCRClassification cl = new MCRClassification();
            cl.deleteFromDatastore(mcrclid);
            LOGGER.info("Classification: " + clid + " deleted.");
            return true;
        } catch (MCRActiveLinkException ae) {
            LOGGER.warn("Classification: " + clid + " can't be deleted, there are some refernces of documents to this.");
            return false;
        } catch (Exception e) {
            LOGGER.error("Classification delete failed - the Reason is:" + e.getMessage() + " .");
            return false;
        }
    }

    /**
     * Here come private methods
     */

    private final Element setNewJDOMCategElement(Element newCateg) {
        List tagList = newCateg.getChildren("label");
        Element element;
        for (int i = 0; i < tagList.size(); i++) {
            element = (Element) tagList.get(i);
            element.getAttribute("lang").setNamespace(XML_NAMESPACE);
        }
        // process url, if given
        element = newCateg.getChild("url");
        if (element != null) {
            element.getAttribute("href").setNamespace(XLINK_NAMESPACE);
        }
        return newCateg;
    }

    public final void deleteTempFile() {
        if (fout != null && fout.isFile())
            fout.delete();
        fout = null;
    }

    public final String setTempFile(String name, FileItem fi) {
        String fname = name;
        fname.replace(' ', '_');
        try {
            fout = new File(CONFIG.getString("MCR.Editor.FileUpload.TempStoragePath"), fname);
            FileOutputStream fouts = new FileOutputStream(fout);
            MCRUtils.copyStream(fi.getInputStream(), fouts);
            fouts.close();
            fname = fout.getPath();
            LOGGER.info("Classification temporary stored under " + name);
        } catch (Exception allE) {
            LOGGER.info("Error storing under " + fname + "  Error: " + allE.getMessage());
            fname = null;
        }
        return fname;
    }

}
