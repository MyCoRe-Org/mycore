/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.mods.dedup;

import java.util.Optional;

import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.dedup.MCRDeDupTitleResolver;
import org.mycore.mods.MCRMODSWrapper;

/**
 * {@link MCRDeDupTitleResolver} for MODS objects. It uses the first {@code mods:titleInfo} of the object
 * as the display title, combining {@code mods:title} and an optional {@code mods:subTitle}. Returns an
 * empty {@link Optional} for objects that are not MODS documents or that have no title.
 */
public class MCRMODSDeDupTitleResolver implements MCRDeDupTitleResolver {

    @Override
    public Optional<String> resolveTitle(MCRObject object) {
        if (!MCRMODSWrapper.isSupported(object)) {
            return Optional.empty();
        }
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        if (wrapper.getMODS() == null) {
            return Optional.empty();
        }
        Element titleInfo = wrapper.getElement("mods:titleInfo");
        if (titleInfo == null) {
            return Optional.empty();
        }
        String title = combine(
            titleInfo.getChildTextTrim("title", MCRConstants.MODS_NAMESPACE),
            titleInfo.getChildTextTrim("subTitle", MCRConstants.MODS_NAMESPACE));
        return title.isBlank() ? Optional.empty() : Optional.of(title);
    }

    private static String combine(String mainTitle, String subTitle) {
        String main = mainTitle == null ? "" : mainTitle;
        if (subTitle == null || subTitle.isBlank()) {
            return main;
        }
        return main.isBlank() ? subTitle : main + " : " + subTitle;
    }
}
