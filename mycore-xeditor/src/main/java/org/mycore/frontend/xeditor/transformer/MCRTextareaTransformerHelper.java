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

public class MCRTextareaTransformerHelper extends MCRTransformerHelperBase {

    private static final String ATTR_NAME = "name";

    @Override
    Collection<String> getSupportedMethods() {
        return Arrays.asList("textarea");
    }

    @Override
    void handle(MCRTransformerHelperCall call) throws Exception {
        replaceXPaths(call);

        call.getReturnElement().setAttribute(ATTR_NAME, state.currentBinding.getAbsoluteXPath());

        String value = state.currentBinding.getValue();
        if (value != null) {
            call.getReturnElement().setText(value);
        }
    }
}
