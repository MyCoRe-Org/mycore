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

package org.mycore.datamodel.metadata.share;

import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectMetadata;

/**
 * @author Thomas Scheffler (yagee)
 */
class MCRDefaultMetadataShareAgent implements MCRMetadataShareAgent {

    private static final Logger LOGGER = LogManager.getLogger();

    /* (non-Javadoc)
     * @see org.mycore.datamodel.metadata.share.MCRMetadataShareAgent#inheritableMetadataChanged(org.mycore.datamodel.metadata.MCRObject, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    public boolean shareableMetadataChanged(MCRObject oldVersion, MCRObject newVersion) {
        final MCRObjectMetadata md = newVersion.getMetadata();
        final MCRObjectMetadata mdold = oldVersion.getMetadata();
        final Element newXML = md.createXML();
        Element oldXML = null;
        try {
            oldXML = mdold.createXML();
        } catch (MCRException exc) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("The old metadata of the object {} was invalid.", oldVersion.getId(), exc);
            }
        }
        //simple save without changes, this is also a short-path for mycore-mods
        //TODO: handle inheritance of mycore-mods in that component
        if (oldXML != null && MCRXMLHelper.deepEqual(newXML, oldXML)) {
            return false;
        }
        int numheritablemd = 0;
        int numheritablemdold;

        for (int i = 0; i < md.size(); i++) {
            final MCRMetaElement melm = md.getMetadataElement(i);
            if (melm.isHeritable()) {
                numheritablemd++;
                try {
                    final MCRMetaElement melmold = mdold.getMetadataElement(melm.getTag());
                    final Element jelm = melm.createXML(true);
                    Element jelmold = null;
                    try {
                        jelmold = melmold.createXML(true);
                    } catch (MCRException exc) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("One of the old metadata elements is invalid.", exc);
                        }
                    }
                    if (jelmold == null || !MCRXMLHelper.deepEqual(jelmold, jelm)) {
                        return true;
                    }
                } catch (final RuntimeException e) {
                    return true;
                }
            }
        }
        numheritablemdold = (int) StreamSupport.stream(mdold.spliterator(), false)
            .filter(MCRMetaElement::isHeritable)
            .count();
        return numheritablemd != numheritablemdold;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.metadata.share.MCRMetadataShareAgent#inheritMetadata(org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    public void distributeMetadata(MCRObject parent) throws MCRPersistenceException, MCRAccessException {
        for (MCRMetaLinkID childId : parent.getStructure().getChildren()) {
            LOGGER.debug("Update metadata from Child {}", childId);
            final MCRObject child = MCRMetadataManager.retrieveMCRObject(childId.getXLinkHrefID());
            MCRMetadataManager.update(child);
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.metadata.share.MCRMetadataShareAgent#inheritMetadata(org.mycore.datamodel.metadata.MCRObject, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    public void receiveMetadata(MCRObject child) {
        MCRObjectID parentID = child.getStructure().getParentID();
        if (parentID == null) {
            return;
        }
        LOGGER.debug("Parent ID = {}", parentID);
        MCRObject parent = MCRMetadataManager.retrieveMCRObject(parentID);
        // remove already embedded inherited tags
        child.getMetadata().removeInheritedMetadata();
        // insert heritable tags
        child.getMetadata().appendMetadata(parent.getMetadata().getHeritableMetadata());
    }

}
