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

import org.mycore.common.MCRUsageException;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.jdom.Element;

/**
 * This class represents a classification item of the MyCoRe classification
 * model and implements the abstract MCRClassificationObject class.
 * 
 * @author Frank Luetzenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRClassificationItem extends MCRClassificationObject {
    /**
     * The constructor create a new MCRClassificationItem for the given classID.
     * This must be a valid MCRObjectID.
     * 
     * @param classID
     *            the ID of this classification
     */
    public MCRClassificationItem(String classID) {
        super(classID);
        MCRObjectID.isValidOrDie(classID);
    }

    /**
     * The constructor create a new MCRClassificationItem for the given classID.
     * This must be a valid MCRObjectID.
     * 
     * @param classID
     *            the ID of this classification as MCRObjectID
     */
    public MCRClassificationItem(MCRObjectID classID) {
        super(classID.getId());
    }

    /**
     * The method call the MCRClassificationManager to create this instance.
     */
    public final void create() {
        manager().createClassificationItem(this);
    }

    /**
     * The method call the MCRClassificationManager to delete this instance.
     * 
     * @param ID
     *            the MCRClassificationItem ID
     */
    public void delete(String ID) {
        super.delete();
        manager().deleteClassificationItem(ID);
    }

    /**
     * The methode return the classification ID.
     * 
     * @return the classification ID
     */
    public final String getClassificationID() {
        return ID;
    }

    /**
     * The method return a MCRCategoryItem for the given category ID.
     * 
     * @param categID
     *            the category ID
     * @return the MCRCategoryItem
     */
    public MCRCategoryItem getCategoryItem(String categID) {
        ensureNotDeleted();
        if ((categID == null) || ((categID = categID.trim()).length() == 0)) {
            throw new MCRUsageException("categID is an empty String");
        }
        return MCRCategoryItem.getCategoryItem(this.ID, categID);
    }

    /**
     * The method return a MCRCategoryItem for the given category labeltext.
     * 
     * @param labeltext
     *            the category label text
     * @return the MCRCategoryItem
     */
    public MCRCategoryItem getCategoryItemForLabelText(String labeltext) {
        ensureNotDeleted();
        if ((labeltext == null) || ((labeltext = labeltext.trim()).length() == 0)) {
            throw new MCRUsageException("labeltext is an empty String");
        }
        return MCRCategoryItem.getCategoryItemForLabelText(this.ID, labeltext);
    }

    /**
     * The method return a MCRClassificationItem for the given classification
     * ID.
     * 
     * @param ID
     *            the classification ID
     * @return the MCRClassificationItem
     */
    public static MCRClassificationItem getClassificationItem(String ID) {
        if ((ID == null) || ((ID = ID.trim()).length() == 0)) {
            throw new MCRUsageException("ID is an empty String");
        }
        return manager().retrieveClassificationItem(ID);
    }

    /**
     * return a MCRClassificationItem as JDOM Element
     * 
     * @return the MCRClassificationItem
     */
    public Element getClassificationItemAsJDom() {
        Element xClassI = new Element("classification");
        xClassI.setAttribute("ID", getClassificationID());
        for (int i = 0; i < getSize(); i++) {
            xClassI.addContent(getJDOMElement(i));
        }
        return xClassI;
    }
}
