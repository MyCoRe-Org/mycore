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

package org.mycore.frontend.fileupload;

import org.apache.log4j.Logger;

import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.workflow.MCRSimpleWorkflowManager;

/**
 * handles uploads via the UploadApplet and store files directly into the IFS.
 * 
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 * 
 * @version $Revision$ $Date$
 * 
 * @see MCRUploadHandler
 */
public class MCRSWFUploadHandlerIFS extends MCRUploadHandlerIFS {
    private static final Logger LOGGER = Logger.getLogger(MCRSWFUploadHandlerIFS.class);
    /**
     * The constructor for this class. It set all data to handle with IFS upload
     * store.
     * 
     * @param docId
     *            the document ID
     * @param derId
     *            the derivate ID
     * @param url
     *            an URL string. Not used in this implementation.
     */
    public MCRSWFUploadHandlerIFS(String docId, String derId, String url) {
        super(docId, derId, url);
        init(docId, derId);
    }

    @Override
    protected void init(String docId, String derId) {
        LOGGER.debug("MCRUploadHandlerMyCoRe DocID: " + docId + " DerId: " + derId);

        try {
            new MCRObjectID(docId);
        } catch (Exception e) {
            LOGGER.debug("Error while creating MCRObjectID : " + docId, e);
        }

        if (derId == null) {
            derId = MCRSimpleWorkflowManager.instance().getNextDrivateID(new MCRObjectID(docId)).getId();
        } else {
            try {
                new MCRObjectID(derId);
            } catch (Exception e) {
                LOGGER.debug("Error while creating MCRObjectID : " + derId, e);
            }
        }

        newDerivate = true;
        if (MCRDerivate.existInDatastore(derId)) {
            LOGGER.debug("Derivate allready exists: " + derId);
            newDerivate = false;
            derivate = new MCRDerivate();
            derivate.receiveFromDatastore(derId);
        } else {
            // create new derivate with given ID
            LOGGER.debug("Create derivate with that ID" + derId);
            derivate = MCRSimpleWorkflowManager.instance().createDerivate(new MCRObjectID(docId), new MCRObjectID(derId));
        }
    }

    @Override
    public void finishUpload() throws Exception {
        // existing files
        if (!rootDir.hasChildren()) {
            derivate.deleteFromDatastore(derivate.getId().getId());
            LOGGER.warn("No file were uploaded, delete entry in database for " + derivate.getId().getId() + " and return.");
            return;
        }
        super.finishUpload();
    }

}
