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
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * Helps transforming html select and option elements. 
 * 
 * @author Frank Lützenkirchen
 */
public class MCRSelectTransformerHelper extends MCRTransformerHelperBase {

    private static final String ATTR_TEXT = "xed:text";
    private static final String ATTR_MULTIPLE = "multiple";
    private static final String ATTR_VALUE = "value";

    private static final String VALUE_SELECTED = "selected";

    /** If true, we are currently somewhere within a HTML select element */
    private boolean withinSelectElement;

    @Override
    public List<String> getSupportedMethods() {
        return Arrays.asList("select", "option");
    }

    @Override
    public void handle(MCRTransformerHelperCall call) {
        if ("select".equals(call.getMethod())) {
            handleSelect(call);
        } else if ("option".equals(call.getMethod())) {
            handleOption(call);
        }
    }

    /**
     * Will be called at both the starting and closing html select tag.
     */
    private void handleSelect(MCRTransformerHelperCall call) {
        withinSelectElement = !withinSelectElement;

        if (withinSelectElement) { // only do at starting html select tag 
            String attrMultiple = call.getAttributeValueOrDefault(ATTR_MULTIPLE, null);
            boolean withinSelectMultiple = Objects.equals(attrMultiple, "multiple");

            replaceXPaths(call);
            setXPath(call.getReturnElement(), withinSelectMultiple);
        }
    }

    /**
     * Will only to something if within a html select elements.
     * There may be option elements outside of select elements that do not bother us.
     */
    private void handleOption(MCRTransformerHelperCall call) {
        if (withinSelectElement) {
            String value = call.getAttributeValueOrDefault(ATTR_VALUE, call.getAttributeValue(ATTR_TEXT));

            if ((!StringUtils.isEmpty(value)) && transformationState.hasValue(value)) {
                call.getReturnElement().setAttribute(VALUE_SELECTED, VALUE_SELECTED);
            }
        }
    }
}
