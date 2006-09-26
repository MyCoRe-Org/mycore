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

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRActiveLinkException;
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
    static Logger LOGGER = Logger.getLogger(MCRClassificationEditor.class);

    private MCRClassification cl = new MCRClassification();

    private Document cljdom;

    private Element categories;

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
            LOGGER.info("Create a new category in classification " + clid + " after categid " + categid);
            Element clroot = indoc.getRootElement();
            if (clroot == null)
                return false;
            Element categories = (Element) clroot.getChild("categories");
            if (categories == null)
                return false;
            Element newCateg = (Element) categories.getChild("category").clone();
            String newID = newCateg.getAttributeValue("ID");
            cljdom = MCRClassification.receiveClassificationAsJDOM(clid);
            categories = cljdom.getRootElement().getChild("categories");

            // check the new category entry
            if (newID.equalsIgnoreCase(categid)) {
                LOGGER.error("The category ID's are not different.");
                return false;
            }
            try {
                MCRCategoryItem item = new MCRCategoryItem(newID, new MCRClassificationItem(clid));
                item.existInStore();
                LOGGER.error("The new category ID is not unique for classification " + clid);
                return false;
            } catch (Exception e) {
            }

            newCateg = setNewJDOMCategElement(newCateg);
            newCateg.setAttribute("counter", "0");

            if (findAndOperateCateg(categories, newCateg, categid, "create", false)) {
                cl.updateFromJDOM(cljdom);
            } else
                return false;
            return true;
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
            cljdom = MCRClassification.receiveClassificationAsJDOM(clid);
            categories = cljdom.getRootElement().getChild("categories");

            // check the category entry
            if (!newID.equalsIgnoreCase(categid)) {
                LOGGER.error("The category ID's are different.");
                return false;
            }
            try {
                MCRCategoryItem item = new MCRCategoryItem(newID, new MCRClassificationItem(clid));
                item.existInStore();
            } catch (Exception e) {
                LOGGER.error("The category ID does not exist in classification " + clid);
                return false;
            }

            // set new category and replace the old
            newCateg = setNewJDOMCategElement(newCateg);
            if (findAndOperateCateg(categories, newCateg, categid, "modify", false)) {
                cl.updateFromJDOM(cljdom);
            }
            LOGGER.info("Update category " + categid + " in classification " + clid + " was successful.");
            return true;
        } catch (Exception e1) {
            LOGGER.error("Category modify fails. Reason is:" + e1.getMessage());
            return false;
        }
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
                if (bUpdate) {
                    clid = cl.updateFromURI(sFile);
                    LOGGER.info("Classification: " + clid + " successfully imported with update!");
                } else {
                    clid = cl.createFromURI(sFile);
                    LOGGER.info("Classification: " + clid + " successfully imported with create!");
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

            cljdom = new Document();
            Element mycoreclass = new Element("mycoreclass");
            Element categories = new Element("categories");
            Element category = new Element("category");
            category.setAttribute("ID", "empty");
            Element label = new Element("label");
            label.setAttribute("text", "empty");
            label.setAttribute("description", "empty");
            category.addContent(label);
            categories.addContent(category);

            MCRObjectID cli = new MCRObjectID();
            String base = CONFIG.getString("MCR.default_project_id", "DocPortal") + "_class";

            LOGGER.debug("Create a CLID with base " + base);
            cli.setNextFreeId(base);

            if (!cli.isValid()) {
                LOGGER.error("Create a unique CLID failed. " + cli.toString());
                return false;
            }

            mycoreclass.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
            mycoreclass.setAttribute("noNamespaceSchemaLocation", "MCRClassification.xsd", org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
            mycoreclass.setAttribute("ID", cli.toString());
            mycoreclass.setAttribute("counter", "0");
            List tagList = clroot.getChildren("label");
            Element element;
            for (int i = 0; i < tagList.size(); i++) {
                element = (Element) tagList.get(i);
                Element newE = new Element("label");
                newE.setAttribute("lang", element.getAttributeValue("lang"), Namespace.XML_NAMESPACE);
                newE.setAttribute("text", element.getAttributeValue("text"));
                if (element.getAttributeValue("description") != null) {
                    newE.setAttribute("description", element.getAttributeValue("description"));
                }
                mycoreclass.addContent(newE);
            }
            mycoreclass.addContent(categories);
            cljdom.addContent(mycoreclass);
            cl.createFromJDOM(cljdom);
            LOGGER.info("Classification " + cli.toString() + " successfully created!");
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
            cljdom = MCRClassification.receiveClassificationAsJDOM(clid);
            Element element;
            List tagList = clroot.getChildren("label");

            cljdom.getRootElement().removeChildren("label");

            for (int i = 0; i < tagList.size(); i++) {
                element = (Element) tagList.get(i);
                Element newE = new Element("label");
                newE.setAttribute("lang", element.getAttributeValue("lang"), Namespace.XML_NAMESPACE);
                newE.setAttribute("text", element.getAttributeValue("text"));
                newE.setAttribute("description", element.getAttributeValue("description"));
                cljdom.getRootElement().addContent(newE);
            }
            cl.updateFromJDOM(cljdom);
            LOGGER.info("Classification " + clid + " successfully modified!");
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
            Element newCateg = new Element("category");

            newCateg.setAttribute("ID", categid);
            cljdom = MCRClassification.receiveClassificationAsJDOM(clid);
            categories = cljdom.getRootElement().getChild("categories");

            bret = findAndOperateCateg(categories, newCateg, categid, way, false);
            if (bret) {
                cl.updateFromJDOM(cljdom);
                LOGGER.info("Category " + categid + " in classification " + clid + " successfull moved!");
                bret = true;
            } else {
                LOGGER.warn("Category " + categid + " in classification " + clid + " not found!");
            }
            return bret;
        } catch (Exception e1) {
            LOGGER.error("Classification modify failed - the Reason is:" + e1.getMessage());
            return false;
        }
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
            cljdom = MCRClassification.receiveClassificationAsJDOM(clid);
            categories = cljdom.getRootElement().getChild("categories");
            bret = findAndOperateCateg(categories, null, categid, "delete", false);
            if (bret) {
                MCRCategoryItem clc = new MCRCategoryItem(categid, clid, null);
                cnt = clc.countDocLinks(null, null);
                if (cnt == 0) {
                    cl.updateFromJDOM(cljdom);
                    LOGGER.info("Category " + categid + " in classification " + clid + " successfull deleted!");
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
            MCRClassification.delete(clid);
            LOGGER.info("Classification: " + clid + " successfull deleted!");
            return true;
        } catch (MCRActiveLinkException ae) {
            LOGGER.warn("Classification: " + clid + " can't be deleted, there are some refernces of documents to this");
            return false;
        } catch (Exception e) {
            LOGGER.error("Classification delete failed - the Reason is:" + e.getMessage());
            return false;
        }
    }

    /**
     * Here comes private methods
     */

    private final Element setNewJDOMCategElement(Element newCateg) {
        List tagList = newCateg.getChildren("label");
        Element element;
        for (int i = 0; i < tagList.size(); i++) {
            element = (Element) tagList.get(i);
            element.setAttribute("lang", element.getAttributeValue("lang"), Namespace.XML_NAMESPACE);
            element.removeAttribute("lang");
        }
        // process url, if given
        element = newCateg.getChild("url");
        if (element != null) {
            element.setAttribute("href", element.getAttributeValue("href"), Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
            element.removeAttribute("href");
        }
        return newCateg;
    }

    private final boolean findAndOperateCateg(Element categories, Element newCateg, String categid, String todo, boolean bfound) {
        List children = categories.getChildren("category");
        if (children.size() == 0 && "create".equalsIgnoreCase(todo)) {
            categories.addContent((Element) newCateg.clone());
            bfound = true;
        }
        for (int j = 0; j < children.size() && bfound == false; j++) {
            // LOGGER.info("J= " + j + " ID =" +
            // ((Element)children.get(j)).getAttributeValue("ID"));
            if (((Element) children.get(j)).getAttributeValue("ID").equalsIgnoreCase(categid)) {
                LOGGER.info(" Found Element!! ID =" + ((Element) children.get(j)).getAttributeValue("ID"));
                bfound = true;
                if ("create".equalsIgnoreCase(todo)) {
                    // ans ende angehangen ->children.size erh�ht sich dadurch
                    // um eins
                    ((Element) children.get(j)).getParentElement().addContent((Element) newCateg.clone());
                    // Element nur noch an die Richtige Stelle setzen
                    int k = j + 1;
                    if (k < children.size()) {
                        // ein element ist dazugekommen, neu initialisieren!
                        children = categories.getChildren("category");
                        // alle elemente runterschieben
                        if (children.get(k) == null) {
                            LOGGER.info("Irgendein merkw�rdiger sonderfall nach ID=" + ((Element) children.get(j)).getAttributeValue("ID"));
                            return false;
                        }
                        Element E2, E1 = (Element) ((Element) children.get(k)).clone();
                        copyElement((Element) children.get(k), newCateg, true);
                        for (j = k; j + 1 < children.size(); j++) {
                            E2 = (Element) ((Element) children.get(j + 1)).clone();
                            copyElement((Element) children.get(j + 1), E1, true);
                            E1 = (Element) E2.clone();
                        }
                    }
                } else if ("modify".equalsIgnoreCase(todo)) {
                    // neue werte auf die categorie r�berkopieren, Kinderknoten
                    // aber nicht anfassen
                    copyElement((Element) children.get(j), newCateg, false);
                } else if ("delete".equalsIgnoreCase(todo)) {
                    String Scounter = ((Element) children.get(j)).getAttributeValue("counter");
                    if (Scounter != null && Integer.parseInt(Scounter) > 0) {
                        return false;
                    }
                    children.remove(j);
                } else if ("up".equalsIgnoreCase(todo) || "down".equalsIgnoreCase(todo)) {
                    // im Baum nach oben/unten verschieben
                    int k = j - 1;
                    if ("up".equalsIgnoreCase(todo)) {
                        // Stelle gefunden, nun das aktuelle Element eins nach
                        // oben schieben ??
                        k = j - 1;
                    } else if ("down".equalsIgnoreCase(todo)) {
                        // Stelle gefunden, nun das aktuelle Element eins nach
                        // unten schieben ??
                        k = j + 1;
                    }
                    // bei up muss es einen vorg�nger geben
                    // bei down einen nachfolger
                    if (children.get(k) == null)
                        return false;

                    Element E2 = (Element) ((Element) children.get(j)).clone();
                    Element E1 = (Element) ((Element) children.get(k)).clone();

                    // platztausch
                    copyElement((Element) children.get(j), E1, true);
                    copyElement((Element) children.get(k), E2, true);
                } else if ("left".equalsIgnoreCase(todo)) {

                    // im Baum nach <- verschieben
                    // in der Hierarchie als Sibling dem dar�berliegenden Parent
                    // anlegen
                    Element E1 = (Element) ((Element) children.get(j)).clone();
                    if (categories.getParentElement() == null)
                        return false;
                    if (!categories.getParentElement().getName().equalsIgnoreCase("category") && !categories.getParentElement().getName().equalsIgnoreCase("categories"))
                        return false;

                    categories.getParentElement().addContent(E1);
                    children.remove(j);
                } else if ("right".equalsIgnoreCase(todo)) {
                    // im Baum nach -> verschieben
                    // in der Hierarchie als Kind im dar�berliegenden Knoten
                    // anlegen
                    Element E1 = (Element) ((Element) children.get(j)).clone();
                    if (j > 0) {
                        // neues erstes kind des dar�berliegenden Siblings
                        if (children.get(j - 1) == null) {
                            return false;
                        }
                        ((Element) children.get(j - 1)).addContent(E1);
                        children.remove(j);
                    } else {
                        // ans ende der kinderliste des parents setzen
                        if (categories.getParentElement() == null || (!categories.getParentElement().getName().equalsIgnoreCase("category"))) {
                            return false;
                        }
                        categories.addContent(E1);
                        children.remove(j);
                    }
                }
                return true;
            }
            if (!bfound && !((Element) children.get(j)).getChildren("category").isEmpty()) {
                bfound = findAndOperateCateg((Element) children.get(j), newCateg, categid, todo, bfound);
            }
        }
        return bfound;
    }

    private final Element copyElement(Element Edest, Element Esrc, boolean bdeep) {
        Edest.setAttribute("ID", Esrc.getAttributeValue("ID"));
        Edest.setAttribute("counter", Esrc.getAttributeValue("counter"));
        Edest.removeChildren("label");
        List labelList = Esrc.getChildren("label");
        Element el;
        for (int i = 0; i < labelList.size(); i++) {
            el = (Element) ((Element) labelList.get(i)).clone();
            Edest.addContent(el);
        }
        Edest.removeChildren("url");
        if (Esrc.getChild("url") != null) {
            Edest.addContent(el = (Element) Esrc.getChild("url").clone());
        }
        if (bdeep) {
            Edest.removeChildren("category");
            List categList = Esrc.getChildren("category");
            for (int i = 0; i < categList.size(); i++) {
                el = (Element) ((Element) categList.get(i)).clone();
                Edest.addContent(el);
            }
        }
        return Edest;
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
