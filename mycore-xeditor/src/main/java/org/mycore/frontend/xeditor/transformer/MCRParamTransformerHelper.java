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
import java.util.Objects;

import org.jaxen.JaxenException;

public class MCRParamTransformerHelper extends MCRTransformerHelperBase {

    private static final String ATTR_NAME = "name";
    private static final String ATTR_DEFAULT = "default";

    @Override
    Collection<String> getSupportedMethods() {
        return Arrays.asList("param");
    }

    @Override
    void handle(MCRTransformerHelperCall call) throws JaxenException {
        String name = call.getAttributeValue(ATTR_NAME);
        String defaultValue = call.getAttributeValueOrDefault(ATTR_DEFAULT, null);

        Object currentValue = state.editorSession.getVariables().get(name);

        if ((currentValue == null) || Objects.equals(currentValue, "")) {
            state.editorSession.getVariables().put(name, defaultValue == null ? "" : defaultValue);
        }
    }
}
