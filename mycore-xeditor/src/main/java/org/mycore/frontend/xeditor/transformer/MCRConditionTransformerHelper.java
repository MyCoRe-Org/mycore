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

public class MCRConditionTransformerHelper extends MCRTransformerHelperBase {

    private static final String ATTR_TEST = "test";

    @Override
    public Collection<String> getSupportedMethods() {
        return Arrays.asList("if", "when");
    }

    @Override
    public void handle(MCRTransformerHelperCall call) {
        String xPathExpression = call.getAttributeValue(ATTR_TEST);
        boolean testResult = state.getXPathEvaluator().test(xPathExpression);
        call.getReturnElement().setText(Boolean.toString(testResult));
    }
}
