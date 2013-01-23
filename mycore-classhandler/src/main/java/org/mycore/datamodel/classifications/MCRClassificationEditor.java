/**
 * $RCSfile: MCRClassificationEditor.java,v $
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

import static org.jdom2.Namespace.XML_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications.ClassificationUserTableFactory.ClassificationUserTable;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;

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
    public boolean createCategoryInClassification(org.jdom2.Document indoc, MCRCategoryID id) {

        try {
            Element clroot = indoc.getRootElement();
            if (clroot == null) {
                return false;
            }
            Element categories = (Element) clroot.getChild("categories");
            if (categories == null)
                return false;

            Element newCateg = (Element) categories.getChild("category").clone();
            MCRCategoryID newID = new MCRCategoryID(id.getRootID(), newCateg.getAttributeValue("ID"));
            MCRCategory classif = getClassificationPool().getClassificationAsPojo(MCRCategoryID.rootID(id.getRootID()),
                    true);

            // check the new category entry
            if (newID.getID().equalsIgnoreCase(id.getID())) {
                LOGGER.error("The category ID's are not different.");
                return false;
            }

            final MCRCategory findCategory = findCategory(classif, newID);
            if (findCategory == null) {
                MCRCategoryID classificationID = classif.getId();
                if (!id.getID().equals("empty")) {

                    MCRCategory prevCateg = findCategory(classif, id);
                    LOGGER.debug("Previous Category: " + prevCateg.getId() + " found.");

                    MCRXMLTransformer.buildCategory(id.getRootID(), newCateg, prevCateg);
                    getClassificationPool().updateClassification(classif);
                    String sessionID = MCRSessionMgr.getCurrentSession().getID();
                    addClassUser(classificationID, sessionID);
                    return true;
                } else {
                    MCRCategory newCategory = MCRXMLTransformer.buildCategory(id.getRootID(), newCateg, classif);
                    LOGGER.debug("Adding category:" + newCategory.getId() + " to classification: " + classificationID);
                    getClassificationPool().updateClassification(classif);
                    String sessionID = MCRSessionMgr.getCurrentSession().getID();
                    addClassUser(classificationID, sessionID);
                    return true;
                }
            } else {
                LOGGER.error("The category " + newID + " does already exist.");
                return false;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            LOGGER.error("Classification creation fails.", e1);
            return false;
        }

    }

    private String addClassUser(MCRCategoryID classificationID, String sessionID) {
        return ClassificationUserTableFactory.getInstance().addClassUser(classificationID.getRootID(), sessionID);
    }

    private MCRClassificationPool getClassificationPool() {
        return MCRClassificationPoolFactory.getInstance();
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
    public final boolean modifyCategoryInClassification(org.jdom2.Document indoc, MCRCategoryID id) {
        try {
            LOGGER.debug("Start modify category in classification " + id.getRootID() + " with categid " + id.getID());
            Element clroot = indoc.getRootElement();
            Element newCateg = (Element) clroot.getChild("categories").getChild("category").clone();
            String newID = newCateg.getAttributeValue("ID");
            MCRCategory classif = getClassificationPool().getClassificationAsPojo(MCRCategoryID.rootID(id.getRootID()),
                    true);
            // check the category entry
            if (!newID.equalsIgnoreCase(id.getID())) {
                LOGGER.error("The category ID's are different.");
                return false;
            }

            MCRCategory oldCategory = findCategory(classif, id);
            if (oldCategory == null) {
                LOGGER.error("The category ID " + id.getID() + " does not exist in classification " + id.getRootID());
                return false;
            }

            MCRCategory parent = oldCategory.getParent();
            MCRCategory newCategory = MCRXMLTransformer.buildCategory(id.getRootID(), newCateg, parent);
            //copy new values to old copy of category
            oldCategory.setURI(newCategory.getURI());
            oldCategory.getLabels().clear();
            Set<MCRLabel> labels = newCategory.getLabels();
            oldCategory.getLabels().addAll(labels);
            //added by MCRXMLTransformer
            parent.getChildren().remove(newCategory);

            getClassificationPool().updateClassification(classif);
            String sessionID = MCRSessionMgr.getCurrentSession().getID();
            addClassUser(classif.getId(), sessionID);

            return true;
        } catch (Exception e1) {
            LOGGER.error("Category modify fails.", e1);
            return false;
        }
    }

    public boolean isLocked(String classid) {
        ClassificationUserTable classificationUserTable = ClassificationUserTableFactory.getInstance();
        String lockedSessionID = classificationUserTable.getSession(classid);
        if (lockedSessionID != null) {
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
     * @param fileName
     *            the name of classification file
     * @return true if all it's okay, else return false
     */
    public boolean importClassification(boolean bUpdate, String fileName) {
        LOGGER.debug("Start importNewClassification.");
        try {
            try {
                LOGGER.info("Reading file " + fileName + " ...\n");
                Document jdom = MCRXMLParserFactory.getParser().parseXML(new MCRFileContent(fileName));
                MCRCategory classification = MCRXMLTransformer.getCategory(jdom);

                getClassificationPool().updateClassification(classification);

                return true;
            } catch (MCRException ex) {
                LOGGER.error("Exception while loading from file " + fileName, ex);
                return false;
            }
        } catch (Exception e1) {
            LOGGER.error("Classification import fails.", e1);
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
    public final boolean createNewClassification(org.jdom2.Document indoc) {
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
            label.setAttribute("lang", CONFIG.getString("MCR.Metadata.DefaultLang"), XML_NAMESPACE);
            category.addContent(label);
            categories.addContent(category);

            String submittedID = indoc.getRootElement().getAttributeValue("ID");
            MCRCategoryID cli = null;

            if (!getClassificationPool().getAllIDs().contains(MCRCategoryID.rootID(submittedID))) {
                cli = MCRCategoryID.rootID(submittedID);
            } else {
                LOGGER.error("Create an unique ID failed. " + submittedID);
                return false;
            }

            mycoreclass.addNamespaceDeclaration(XSI_NAMESPACE);
            mycoreclass.setAttribute("noNamespaceSchemaLocation", "MCRClassification.xsd", XSI_NAMESPACE);
            mycoreclass.setAttribute("ID", cli.getRootID());
            mycoreclass.setAttribute("counter", "0");
            @SuppressWarnings("unchecked")
            List<Element> tagList = clroot.getChildren("label");
            for (Element element : tagList) {
                Element newE = new Element("label");
                newE.setAttribute("lang", element.getAttributeValue("lang", XML_NAMESPACE), XML_NAMESPACE);
                newE.setAttribute("text", element.getAttributeValue("text"));
                if (element.getAttributeValue("description") != null) {
                    newE.setAttribute("description", element.getAttributeValue("description"));
                }
                mycoreclass.addContent(newE);
            }
            mycoreclass.addContent(categories);
            Document cljdom = new Document();
            cljdom.addContent(mycoreclass);
            MCRCategory classif = MCRXMLTransformer.getCategory(cljdom);
            getClassificationPool().updateClassification(classif);
            LOGGER.debug("Classification " + cli.toString() + " successfully created!");
            return true;

        } catch (Exception e1) {
            LOGGER.error("Classification creation fails.", e1);
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
    public final boolean modifyClassificationDescription(org.jdom2.Document indoc, String clid) {
        try {
            LOGGER.debug("Start modify classification description for " + clid);
            Element clroot = indoc.getRootElement();
            MCRCategory classif = getClassificationPool().getClassificationAsPojo(MCRCategoryID.rootID(clid), true);
            classif.getLabels().clear();

            @SuppressWarnings("unchecked")
            List<Element> tagList = clroot.getChildren("label");
            for (Element element : tagList) {
                MCRLabel label = new MCRLabel();
                label.setLang(element.getAttributeValue("lang", XML_NAMESPACE));
                label.setText(element.getAttributeValue("text"));
                label.setDescription(element.getAttributeValue("description"));
                classif.getLabels().add(label);
            }

            getClassificationPool().updateClassification(classif);
            String sessionID = MCRSessionMgr.getCurrentSession().getID();
            addClassUser(classif.getId(), sessionID);
            return true;
        } catch (Exception e1) {
            LOGGER.error("Classification modify fails.", e1);
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

            MCRCategoryID categoryId = new MCRCategoryID(clid, categid);
            MCRCategory classif = getClassificationPool().getClassificationAsPojo(MCRCategoryID.rootID(clid), true);

            if (way.equalsIgnoreCase("up")) {
                MCRCategory categ = findCategory(classif, categoryId);
                bret = moveUp(categ);
            } else if (way.equalsIgnoreCase("down")) {
                MCRCategory categ = findCategory(classif, categoryId);
                bret = moveDown(categ);
            } else if (way.equalsIgnoreCase("right")) {
                MCRCategory categ = findCategory(classif, categoryId);
                bret = moveRight(categ);
            } else if (way.equalsIgnoreCase("left")) {
                MCRCategory categ = findCategory(classif, categoryId);
                bret = moveLeft(categ);
            }

            if (bret) {
                getClassificationPool().updateClassification(classif);
                String sessionID = MCRSessionMgr.getCurrentSession().getID();
                addClassUser(classif.getId(), sessionID);
                bret = true;
            }
            return bret;
        } catch (Exception e1) {
            LOGGER.error("Classification modify failed.", e1);
            return false;
        }
    }

    private boolean moveUp(MCRCategory cat) {
        MCRCategory parent = cat.getParent();
        int index = parent.getChildren().indexOf(cat);
        if (index > 0) {
            parent.getChildren().remove(index);
            parent.getChildren().add(index - 1, cat);
            return true;
        } else {
            return false;
        }
    }

    private boolean moveDown(MCRCategory cat) {
        MCRCategory parent = cat.getParent();
        int index = parent.getChildren().indexOf(cat);
        if (index < (parent.getChildren().size() - 1)) {
            parent.getChildren().remove(index);
            parent.getChildren().add(index + 1, cat);
            return true;
        } else {
            return false;
        }
    }

    private boolean moveRight(MCRCategory cat) {
        MCRCategory parent = cat.getParent();
        if (parent.getChildren().size() == 1) {
            return false;
        }
        int index = parent.getChildren().indexOf(cat);
        parent.getChildren().remove(index);
        if (index > 0) {
            index--;
        }
        parent.getChildren().get(index).getChildren().add(cat);
        getClassificationPool().getMovedCategories().add(cat.getId());
        return true;
    }

    private boolean moveLeft(MCRCategory cat) {
        MCRCategory parent = cat.getParent();

        if ((!parent.isCategory()) || parent == null) {
            return false;
        }

        MCRCategory grandParent = parent.getParent();
        if (grandParent == null) {
            grandParent = cat;
        }

        int oldIndex = parent.getChildren().indexOf(cat);
        int newIndex = grandParent.getChildren().indexOf(parent);
        parent.getChildren().remove(oldIndex);

        grandParent.getChildren().add(newIndex + 1, cat);
        getClassificationPool().getMovedCategories().add(cat.getId());
        return true;
    }

    public boolean saveAll() {
        final boolean saveAll = getClassificationPool().saveAll();
        ClassificationUserTableFactory.getInstance().clearUserClassTable(MCRSessionMgr.getCurrentSession());
        return saveAll;
    }

    public boolean purgeAll() {
        ClassificationUserTableFactory.getInstance().clearUserClassTable(MCRSessionMgr.getCurrentSession());
        return getClassificationPool().purgeAll();
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
            int cnt = 1;

            MCRCategory classif = getClassificationPool().getClassificationAsPojo(MCRCategoryID.rootID(clid), true);
            MCRCategoryID id = new MCRCategoryID(clid, categid);
            MCRCategory categToDelete = findCategory(classif, id);
            MCRCategory parent = categToDelete.getParent();

            if (parent != null) {
                Map<MCRCategoryID, Boolean> linkMap = getClassificationPool().hasLinks(categToDelete);
                if (!linkMap.get(categToDelete.getId())) {
                    parent.getChildren().remove(categToDelete);
                    getClassificationPool().updateClassification(classif);
                    String sessionID = MCRSessionMgr.getCurrentSession().getID();
                    addClassUser(classif.getId(), sessionID);
                    LOGGER.info("Classif: " + classif.getId().getRootID());
                    cnt = 0;
                }
            } else {
                LOGGER.warn("Category " + categid + " in classification: " + clid + " not found! - nothing todo");
            }

            return cnt;
        } catch (Exception e1) {
            LOGGER.error("Categorie delete failed.", e1);
            e1.printStackTrace();
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
            MCRCategoryID mcrclid = MCRCategoryID.rootID(clid);
            getClassificationPool().deleteClassification(mcrclid);
            LOGGER.debug("Classification: " + clid + " deleted.");
            return true;
        } catch (Exception e) {
            LOGGER.error("Classification delete failed.", e);
            return false;
        }
    }

    /**
     * Here come private methods
     */

    public MCRCategory findCategory(MCRCategory classification, MCRCategoryID ID) {
        MCRCategory found = null;
        for (MCRCategory cat : classification.getChildren()) {
            if (cat.getId().equals(ID)) {
                found = cat;
                break;
            }
            MCRCategory rFound = findCategory(cat, ID);
            if (rFound != null) {
                found = rFound;
                break;
            }
        }

        return found;
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
            LOGGER.error("Error storing under " + fname, allE);
            fname = null;
        }
        return fname;
    }

    public boolean isEdited(MCRCategoryID classID) {
        return getClassificationPool().isEdited(classID);
    }

    public MCRCategory getClassification(MCRCategoryID rootID, boolean writeAccess) {
        return getClassificationPool().getClassificationAsPojo(rootID, writeAccess);
    }

}
