/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.frontend.cli;

import java.io.File;

import org.jdom.Document;

import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;

/**
 * Commands for the classification system.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRClassification2Commands extends MCRAbstractCommands {

    private static final MCRCategoryDAO DAO = new MCRCategoryDAOImpl();

    public MCRClassification2Commands() {
        command.add(new MCRCommand("load classification from file {0}", "org.mycore.frontend.cli.MCRClassification2Commands.loadFromFile String",
                "The command add a new classification form file {0} to the system."));
        command.add(new MCRCommand("update classification from file {0}", "org.mycore.frontend.cli.MCRClassification2Commands.updateFromFile String",
                "The command add a new classification form file {0} to the system."));
        command.add(new MCRCommand("delete classification {0}", "org.mycore.frontend.cli.MCRClassification2Commands.delete String",
                "The command remove the classification with MCRObjectID {0} from the system."));
        command.add(new MCRCommand("count classification children of {0}", "org.mycore.frontend.cli.MCRClassification2Commands.countChildren String",
                "The command remove the classification with MCRObjectID {0} from the system."));
    }

    /**
     * Deletes a classification
     * 
     * @param classID
     * @see MCRCategoryDAO#deleteCategory(MCRCategoryID)
     */
    public static void delete(String classID) {
        DAO.deleteCategory(MCRCategoryID.rootID(classID));
    }

    /**
     * Deletes a classification
     * 
     * @param classID
     * @see MCRCategoryDAO#deleteCategory(MCRCategoryID)
     */
    public static void countChildren(String classID) {
        MCRCategory category = DAO.getCategory(MCRCategoryID.rootID(classID), -1);
        System.out.printf("%s has %d children", category.getId(), category.getChildren().size());
    }

    /**
     * Adds a classification.
     * 
     * Classification is built from a file.
     * 
     * @param filname
     *            file in mcrclass xml format
     * @see MCRCategoryDAO#addCategory(MCRCategoryID, MCRCategory)
     */
    public static void loadFromFile(String filename) {
        File file = new File(filename);
        Document xml = MCRXMLHelper.parseURI(file.getPath());
        MCRCategory category = MCRXMLTransformer.getCategory(xml);
        DAO.addCategory(null, category);
    }

    /**
     * Replaces a classification with a new version
     * 
     * @param filename
     *            file in mcrclass xml format
     * @see MCRCategoryDAO#replaceCategory(MCRCategory)
     */
    public static void updateFromFile(String filename) {
        File file = new File(filename);
        Document xml = MCRXMLHelper.parseURI(file.getPath());
        MCRCategory category = MCRXMLTransformer.getCategory(xml);
        DAO.replaceCategory(category);
    }

}
