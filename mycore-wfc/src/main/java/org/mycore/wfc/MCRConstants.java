/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
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
        return MCRCategoryID.rootID(classID);
    }

    private static MCRCategoryID getCollectionClassID() {
        final String classID = MCRConfiguration.instance().getString(CONFIG_PREFIX + "CollectionClassID",
            "objectCollection");
        return MCRCategoryID.rootID(classID);
    }
}
