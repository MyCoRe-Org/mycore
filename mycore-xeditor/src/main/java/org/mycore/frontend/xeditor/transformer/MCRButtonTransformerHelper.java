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

package org.mycore.frontend.xeditor.transformer;

import java.util.Arrays;
import java.util.Collection;

import org.apache.logging.log4j.util.Strings;
import org.mycore.frontend.xeditor.target.MCRSubselectTarget;

public class MCRButtonTransformerHelper extends MCRTransformerHelperBase {

    private static final char COLON = ':';

    private static final String ATTR_NAME = "name";
    private static final String ATTR_HREF = "xed:href";
    private static final String ATTR_TARGET = "xed:target";

    @Override
    Collection<String> getSupportedMethods() {
        return Arrays.asList("button");
    }

    @Override
    void handle(MCRTransformerHelperCall call) throws Exception {
        String target = call.getAttributeValue(ATTR_TARGET);
        String href = call.getAttributeValue(ATTR_HREF);

        StringBuilder name = new StringBuilder();
        name.append("_xed_submit_").append(target);

        if ("subselect".equals(target)) {
            name.append(COLON).append(transformationState.currentBinding.getAbsoluteXPath()).append(COLON)
                .append(MCRSubselectTarget.encode(href));
        } else if (Strings.isNotBlank(href)) {
            name.append(COLON).append(href);
        }

        call.getReturnElement().setAttribute(ATTR_NAME, name.toString());
    }
}
