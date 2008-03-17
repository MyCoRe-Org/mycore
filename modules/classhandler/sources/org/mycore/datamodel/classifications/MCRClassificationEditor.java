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

import static org.jdom.Namespace.XML_NAMESPACE;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
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

    private MCRCategory classif;

    private static MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

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
    public boolean createCategoryInClassification(org.jdom.Document indoc, MCRCategoryID id) {

        try {
            Element clroot = indoc.getRootElement();
            if (clroot == null) {
                return false;
            }
            Element categories = (Element) clroot.getChild("categories");
            if (categories == null)
                return false;

            Element newCateg = (Element) categories.getChild("category").clone();
            MCRCategoryID newID = MCRCategoryID.rootID(newCateg.getAttributeValue("ID"));
            classif = MCRClassificationBrowserData.getClassificationPool().getClassificationAsPojo(MCRCategoryID.rootID(id.getRootID()), true);

            // check the new category entry
            if (newID.getID().equalsIgnoreCase(id.getID())) {
                LOGGER.error("The category ID's are not different.");
                return false;
            }

            newCateg = setNewJDOMCategElement(newCateg);
            newCateg.setAttribute("counter", "0");

            if (!DAO.exist(newID)) {
                if (!id.getID().equals("empty")) {
                    
                    MCRCategory prevCateg = findCategory(classif, id);
                    LOGGER.debug("Previous Category: " + prevCateg.getId() + " found.");
                    
                    MCRCategory newCategory = MCRXMLTransformer.getCategory(id.getRootID(), newCateg, 1);
                    prevCateg.getChildren().add(newCategory);
                    MCRClassificationBrowserData.getClassificationPool().updateClassification(classif);
                    String sessionID = MCRSessionMgr.getCurrentSession().getID();
                    MCRClassificationBrowserData.ClassUserTable.put(classif.getId().getRootID(), sessionID);
                    return true;
                } else {
                    MCRCategory newCategory = MCRXMLTransformer.getCategory(id.getRootID(), newCateg, 1);
                    LOGGER.debug("Adding category:" + newCategory.getId()+" to classification: "+classif.getId());
                    classif.getChildren().add(newCategory);
                    MCRClassificationBrowserData.getClassificationPool().updateClassification(classif);
                    String sessionID = MCRSessionMgr.getCurrentSession().getID();
                    MCRClassificationBrowserData.ClassUserTable.put(classif.getId().getRootID(), sessionID);
                    return true;
                }
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
    public final boolean modifyCategoryInClassification(org.jdom.Document indoc, MCRCategoryID id) {
        try {
            LOGGER.debug("Start modify category in classification " + id.getRootID() + " with categid " + id.getID());
            Element clroot = indoc.getRootElement();
            Element newCateg = (Element) clroot.getChild("categories").getChild("category").clone();
            String newID = newCateg.getAttributeValue("ID");
            classif = MCRClassificationBrowserData.getClassificationPool().getClassificationAsPojo(MCRCategoryID.rootID(id.getRootID()), true);
            newCateg = setNewJDOMCategElement(newCateg);
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

            MCRCategory newCategory = MCRXMLTransformer.getCategory(id.getRootID(), newCateg, 1);
            oldCategory.getLabels().clear();
            Map<String, MCRLabel> labels = newCategory.getLabels();
            oldCategory.getLabels().putAll(labels);

            MCRClassificationBrowserData.getClassificationPool().updateClassification(classif);
            String sessionID = MCRSessionMgr.getCurrentSession().getID();
            MCRClassificationBrowserData.ClassUserTable.put(classif.getId().getRootID(), sessionID);

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
        try {
            try {
                LOGGER.info("Reading file " + sFile + " ...\n");
                Document jdom = MCRXMLHelper.parseURI(sFile);
                MCRCategory classification = MCRXMLTransformer.getCategory(jdom);

                MCRClassificationBrowserData.getClassificationPool().updateClassification(classification);

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
            XMLOutputter out = new XMLOutputter();
            out.output(indoc, System.out);

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

            if (!MCRClassificationBrowserData.getClassificationPool().getAllIDs().contains(MCRCategoryID.rootID(submittedID))) {
                cli = MCRCategoryID.rootID(submittedID);
            } else {
                LOGGER.error("Create an unique ID failed. " + cli.getRootID());
                return false;
            }

            mycoreclass.addNamespaceDeclaration(XSI_NAMESPACE);
            mycoreclass.setAttribute("noNamespaceSchemaLocation", "MCRClassification.xsd", XSI_NAMESPACE);
            mycoreclass.setAttribute("ID", cli.getRootID());
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
            out.output(cljdom, System.out);
            MCRCategory classif = MCRXMLTransformer.getCategory(cljdom);
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
            classif = MCRClassificationBrowserData.getClassificationPool().getClassificationAsPojo(MCRCategoryID.rootID(clid), true);
            Element element;
            List tagList = clroot.getChildren("label");

            classif.getLabels().clear();

            for (int i = 0; i < tagList.size(); i++) {
                element = (Element) tagList.get(i);
                MCRLabel label = new MCRLabel();
                label.setLang(element.getAttributeValue("lang"));
                label.setText(element.getAttributeValue("text"));
                label.setDescription(element.getAttributeValue("description"));
                classif.getLabels().put(label.getLang(), label);
            }

            MCRClassificationBrowserData.getClassificationPool().updateClassification(classif);
            String sessionID = MCRSessionMgr.getCurrentSession().getID();
            MCRClassificationBrowserData.ClassUserTable.put(classif.getId().getRootID(), sessionID);
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

            MCRCategoryID categoryId = new MCRCategoryID(clid, categid);
            classif = MCRClassificationBrowserData.getClassificationPool().getClassificationAsPojo(MCRCategoryID.rootID(clid), true);

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
                MCRClassificationBrowserData.getClassificationPool().updateClassification(classif);
                String sessionID = MCRSessionMgr.getCurrentSession().getID();
                MCRClassificationBrowserData.ClassUserTable.put(classif.getId().getRootID(), sessionID);
                bret = true;
            }
            return bret;
        } catch (Exception e1) {
            LOGGER.error("Classification modify failed - the Reason is:" + e1.getMessage(), e1);
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
        return true;
    }

    private boolean moveLeft(MCRCategory cat) {
        MCRCategory parent = cat.getParent();

        if ((!parent.isCategory()) || parent == null) {
            return false;
        }

        MCRCategory grandParent = parent.getParent();
        if (grandParent == null) {
            grandParent = classif;
        }

        int oldIndex = parent.getChildren().indexOf(cat);
        int newIndex = grandParent.getChildren().indexOf(parent);
        parent.getChildren().remove(oldIndex);

        grandParent.getChildren().add(newIndex + 1, cat);
        return true;

    }

    public boolean saveAll() {
        final boolean saveAll = MCRClassificationBrowserData.getClassificationPool().saveAll();
        MCRClassificationBrowserData.clearUserClassTable(MCRSessionMgr.getCurrentSession());
        return saveAll;
    }

    public boolean purgeAll() {
        MCRClassificationBrowserData.clearUserClassTable(MCRSessionMgr.getCurrentSession());
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
            int cnt = 0;

            classif = MCRClassificationBrowserData.getClassificationPool().getClassificationAsPojo(MCRCategoryID.rootID(clid), true);
            MCRCategoryID id = new MCRCategoryID(clid, categid);
            MCRCategory categToDelete = findCategory(classif, id);
            MCRCategory parent = categToDelete.getParent();

            if (parent != null) {
                Collection<String> links = MCRCategLinkServiceFactory.getInstance().getLinksFromCategory(id);
                if (links.isEmpty()) {
                    parent.getChildren().remove(categToDelete);
                    MCRClassificationBrowserData.getClassificationPool().updateClassification(classif);
                    String sessionID = MCRSessionMgr.getCurrentSession().getID();
                    MCRClassificationBrowserData.ClassUserTable.put(classif.getId().getRootID(), sessionID);
                    cnt = links.size();
                }
            } else {
                LOGGER.warn("Category " + categid + " in classification: " + clid + " not found! - nothing todo");
            }

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
            MCRCategoryID mcrclid = MCRCategoryID.rootID(clid);
            MCRCategoryDAOFactory.getInstance().deleteCategory(mcrclid);
            LOGGER.debug("Classification: " + clid + " deleted.");
            MCRClassificationBrowserData.getClassificationPool().deleteClassification(mcrclid);
            return true;
        } catch (Exception e) {
            LOGGER.error("Classification delete failed - the Reason is:" + e.getMessage() + " .");
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
