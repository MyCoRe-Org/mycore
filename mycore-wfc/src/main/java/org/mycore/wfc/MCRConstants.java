/*
 * $Id$
 * $Revision: 5697 $ $Date: 15.03.2012 $
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

package org.mycore.wfc;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.wfc.actionmapping.MCRAction;
import org.mycore.wfc.actionmapping.MCRActionMappings;
import org.mycore.wfc.actionmapping.MCRCollection;
import org.mycore.wfc.actionmapping.MCRDecision;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRConstants {
    private MCRConstants() {
    }

    public static final String CONFIG_PREFIX = "MCR.Module-wfc.";

    public static final JAXBContext JAXB_CONTEXT = initContext();

    public static final MCRCategoryID STATUS_CLASS_ID = getStatusClassID();

    public static final MCRCategoryID COLLECTION_CLASS_ID = getCollectionClassID();

    private static JAXBContext initContext() {
        try {
            return JAXBContext.newInstance(MCRActionMappings.class, MCRCollection.class, MCRAction.class,
                MCRDecision.class);
        } catch (JAXBException e) {
            throw new MCRException("Could not initialize JAXBContext.", e);
        }
    }

    private static MCRCategoryID getStatusClassID() {
        final String classID = MCRConfiguration.instance().getString(CONFIG_PREFIX + "StatusClassID", "objectStatus");
        final MCRCategoryID categoryID = MCRCategoryID.rootID(classID);
        return categoryID;
    }

    private static MCRCategoryID getCollectionClassID() {
        final String classID = MCRConfiguration.instance().getString(CONFIG_PREFIX + "CollectionClassID",
            "objectCollection");
        final MCRCategoryID categoryID = MCRCategoryID.rootID(classID);
        return categoryID;
    }
}
