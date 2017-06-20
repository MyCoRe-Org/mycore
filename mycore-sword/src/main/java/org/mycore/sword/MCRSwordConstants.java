package org.mycore.sword;

import org.mycore.common.config.MCRConfiguration;

/**
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSwordConstants {
    public static final String SWORD2_COL_IRI = "sword2/col/";

    public static final String SWORD2_EDIT_MEDIA_IRI = "sword2/edit-media/";

    public static final String SWORD2_EDIT_IRI = "sword2/edit/";

    public static final String SWORD2_EDIT_MEDIA_REL = "edit-media";

    public static final String SWORD2_EDIT_REL = "edit";

    public static final Integer MAX_ENTRYS_PER_PAGE = MCRConfiguration.instance().getInt("MCR.SWORD.Page.Object.Count");

    public static final String MCR_SWORD_COLLECTION_PREFIX = "MCR.Sword.Collection.";

    public static final String MIME_TYPE_APPLICATION_ZIP = "application/zip";

    public static final String MIME_TYPE_TEXT_XML = "text/xml";
}
