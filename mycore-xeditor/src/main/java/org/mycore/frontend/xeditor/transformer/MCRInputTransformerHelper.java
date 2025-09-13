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

public class MCRInputTransformerHelper extends MCRTransformerHelperBase {

    private static final String ATTR_VALUE = "value";
    private static final String ATTR_TYPE = "type";

    private static final String TYPE_CHECKBOX = "checkbox";
    private static final String TYPE_RADIO = "radio";

    private static final String VALUE_CHECKED = "checked";

    @Override
    Collection<String> getSupportedMethods() {
        return Arrays.asList("input");
    }

    @Override
    void handle(MCRTransformerHelperCall call) throws Exception {
        String type = call.getAttributeValue(ATTR_TYPE);

        setXPath(call.getReturnElement(), TYPE_CHECKBOX.equals(type));

        if (TYPE_RADIO.equals(type) || TYPE_CHECKBOX.equals(type)) {
            String value = call.getAttributeValue(ATTR_VALUE);
            if (state.hasValue(value)) {
                call.getReturnElement().setAttribute(VALUE_CHECKED, VALUE_CHECKED);
            }
        } else {
            call.getReturnElement().setAttribute(ATTR_VALUE, state.currentBinding.getValue());
        }
    }
}
