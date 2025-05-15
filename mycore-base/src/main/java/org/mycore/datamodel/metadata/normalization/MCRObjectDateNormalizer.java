package org.mycore.datamodel.metadata.normalization;

import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectService;

import static org.mycore.datamodel.metadata.MCRObjectService.DATE_TYPE_CREATEDATE;

/**
 * This normalizer sets the creation and modification date of an MCRObject if they are not set.
 */
public class MCRObjectDateNormalizer extends MCRObjectNormalizer {

    @Override
    public void normalize(MCRObject mcrObject) {
        // create this object in datastore
        if (mcrObject.getService().getDate(DATE_TYPE_CREATEDATE) == null) {
            mcrObject.getService().setDate(DATE_TYPE_CREATEDATE);
        }
        if (mcrObject.getService().getDate(MCRObjectService.DATE_TYPE_MODIFYDATE) == null) {
            mcrObject.getService().setDate(MCRObjectService.DATE_TYPE_MODIFYDATE);
        }
    }
}
