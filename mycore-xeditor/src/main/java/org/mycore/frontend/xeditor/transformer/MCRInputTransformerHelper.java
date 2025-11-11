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

/**
 * Helps transforming html input elements. 
 * 
 * @author Frank Lützenkirchen
 */
public class MCRInputTransformerHelper extends MCRTransformerHelperBase {

    private static final String ATTR_NAME = "name";
    private static final String ATTR_VALUE = "value";
    private static final String ATTR_TYPE = "type";

    private static final String TYPE_CHECKBOX = "checkbox";
    private static final String TYPE_RADIO = "radio";

    private static final String VALUE_CHECKED = "checked";

    @Override
    List<String> getSupportedMethods() {
        return Arrays.asList("input");
    }

    @Override
    void handle(MCRTransformerHelperCall call) {
        String type = call.getAttributeValue(ATTR_TYPE);

        if (call.getAttributeValue(ATTR_NAME) == null) {
            setXPath(call.getReturnElement(), Objects.equals(type, TYPE_CHECKBOX));

            if (Objects.equals(type, TYPE_RADIO) || Objects.equals(type, TYPE_CHECKBOX)) {
                String value = call.getAttributeValue(ATTR_VALUE);
                if (transformationState.hasValue(value)) {
                    call.getReturnElement().setAttribute(VALUE_CHECKED, VALUE_CHECKED);
                }
            } else {
                String currentValue = transformationState.getCurrentBinding().getValue();
                call.getReturnElement().setAttribute(ATTR_VALUE, currentValue);
            }
        }
    }
}
