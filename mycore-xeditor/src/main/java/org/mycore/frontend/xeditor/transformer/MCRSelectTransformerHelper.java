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

import java.util.Objects;

import org.apache.logging.log4j.util.Strings;

public class MCRSelectTransformerHelper {

    private static final String ATTR_TEXT = "text";
    private static final String ATTR_MULTIPLE = "multiple";
    private static final String ATTR_VALUE = "value";

    private static final String VALUE_SELECTED = "selected";

    private MCRTransformerHelper state;

    private boolean withinSelectElement;

    MCRSelectTransformerHelper(MCRTransformerHelper state) {
        this.state = state;
    }

    void handleSelect(MCRTransformerHelperCall call) {
        withinSelectElement = !withinSelectElement;

        String attrMultiple = call.getAttributeValueOrDefault(ATTR_MULTIPLE, null);
        boolean withinSelectMultiple = Objects.equals(attrMultiple, "multiple");

        if (withinSelectElement) {
            state.setXPath(call.getReturnElement(), withinSelectMultiple);
        }
    }

    void handleOption(MCRTransformerHelperCall call) {
        if (withinSelectElement) {
            String value = call.getAttributeValueOrDefault(ATTR_VALUE, call.getAttributeValue(ATTR_TEXT));

            if ((!Strings.isEmpty(value)) && state.hasValue(value)) {
                call.getReturnElement().setAttribute(VALUE_SELECTED, VALUE_SELECTED);
            }
        }
    }

}
