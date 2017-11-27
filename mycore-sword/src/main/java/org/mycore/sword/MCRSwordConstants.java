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
