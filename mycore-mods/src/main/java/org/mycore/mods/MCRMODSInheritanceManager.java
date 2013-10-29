/*
 * $Id$
 * $Revision: 5697 $ $Date: Oct 28, 2013 $
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

package org.mycore.mods;

import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectMetadata;
import org.mycore.datamodel.metadata.inheritance.MCRInheritanceManager;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRMODSInheritanceManager implements MCRInheritanceManager {
    private static Logger LOGGER = Logger.getLogger(MCRMODSInheritanceManager.class);

    private static final String HOST_SECTION_XPATH = "mods:relatedItem[@type='host']";

    /* (non-Javadoc)
     * @see org.mycore.datamodel.metadata.inheritance.MCRInheritanceManager#inheritableMetadataChanged(org.mycore.datamodel.metadata.MCRObject, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    public boolean inheritableMetadataChanged(MCRObject oldVersion, MCRObject newVersion) {
        final MCRObjectMetadata md = newVersion.getMetadata();
        final MCRObjectMetadata mdold = oldVersion.getMetadata();
        //if any metadata changed we need to update children
        boolean metadataChanged = !MCRXMLHelper.deepEqual(md.createXML(), mdold.createXML());
        if (!metadataChanged) {
            LOGGER.info("Metadata did not change on update of " + newVersion.getId());
        }
        return metadataChanged;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.metadata.inheritance.MCRInheritanceManager#inheritMetadata(org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    public void inheritMetadata(MCRObject parent) {
        MCRMODSWrapper parentWrapper = new MCRMODSWrapper(parent);
        List<MCRMetaLinkID> children = parentWrapper.getMCRObject().getStructure().getChildren();
        if (!children.isEmpty()) {
            LOGGER.info("Update inherited metadata");
            for (MCRMetaLinkID childIdRef : children) {
                LOGGER.info("Update: " + childIdRef);
                MCRObject child = MCRMetadataManager.retrieveMCRObject(childIdRef.getXLinkHrefID());
                MCRMODSWrapper childWrapper = new MCRMODSWrapper(child);
                inheritToChild(parentWrapper, childWrapper);
                LOGGER.info("Saving: " + childIdRef);
                try {
                    MCRMetadataManager.update(child);
                } catch (MCRActiveLinkException e) {
                    throw new MCRPersistenceException("Error while updating inherited metadata", e);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.metadata.inheritance.MCRInheritanceManager#inheritMetadata(org.mycore.datamodel.metadata.MCRObject, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    public void inheritMetadata(MCRObject parent, MCRObject child) {
        MCRMODSWrapper parentWrapper = new MCRMODSWrapper(parent);
        MCRMODSWrapper childWrapper = new MCRMODSWrapper(child);
        inheritToChild(parentWrapper, childWrapper);
    }

    private void inheritToChild(MCRMODSWrapper parentWrapper, MCRMODSWrapper childWrapper) {
        LOGGER.debug("Removing old inherited Metadata.");
        childWrapper.removeInheritedMetadata();
        LOGGER.info("Inserting inherited Metadata.");
        Element hostContainer = childWrapper.getElement(HOST_SECTION_XPATH);
        if (hostContainer == null) {
            LOGGER.info("Adding new relatedItem[@type='host'])");
            hostContainer = new Element("relatedItem", MCRConstants.MODS_NAMESPACE).setAttribute("type", "host");
            childWrapper.getMODS().addContent(hostContainer);
        }
        hostContainer.addContent(parentWrapper.getMODS().cloneContent());
    }

}
