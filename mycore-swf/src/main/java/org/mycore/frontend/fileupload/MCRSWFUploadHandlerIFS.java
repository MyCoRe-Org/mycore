/*
 * 
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

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.workflow.MCRSimpleWorkflowManager;

/**
 * handles uploads via the UploadApplet and store files directly into the IFS.
 * 
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 * @author Frank L\u00FCtzenkirchen
 * 
 * @version $Revision$ $Date$
 * 
 * @see MCRUploadHandler
 */
public class MCRSWFUploadHandlerIFS extends MCRUploadHandlerIFS {
    
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
    }

    protected MCRObjectID getOrCreateDerivateID() {
        if (derivateID == null) {
            MCRObjectID documentOID = MCRObjectID.getInstance( documentID );
            MCRObjectID derivateOID = MCRSimpleWorkflowManager.instance().getNextDrivateID( documentOID );
            this.derivateID = derivateOID.toString();
            return derivateOID;
        } else
            return MCRObjectID.getInstance(derivateID);
    }
}
