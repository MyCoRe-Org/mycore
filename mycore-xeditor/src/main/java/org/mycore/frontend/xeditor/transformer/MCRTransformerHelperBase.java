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

import java.util.Collection;

import org.jdom2.Element;

public abstract class MCRTransformerHelperBase {

    private static final char COLON = ':';

    private static final String ATTR_NAME = "name";

    private static final String PREDICATE_IS_FIRST = "[1]";

    protected MCRTransformerHelper state;

    void init(MCRTransformerHelper state) {
        this.state = state;
    }

    abstract Collection<String> getSupportedMethods();

    abstract void handle(MCRTransformerHelperCall call) throws Exception;

    protected String replaceXPaths(String text) {
        return state.getXPathEvaluator().replaceXPaths(text, false);
    }

    protected void replaceXPaths(MCRTransformerHelperCall call) {
        call.getAttributeMap().keySet().removeIf(key -> key.contains(String.valueOf(COLON)));
        call.getAttributeMap()
            .forEach((name, value) -> call.getReturnElement().setAttribute(name, replaceXPaths(value)));
    }

    protected void setXPath(Element result, boolean fixPathForMultiple) {
        String xPath = state.currentBinding.getAbsoluteXPath();
        if (fixPathForMultiple && xPath.endsWith(PREDICATE_IS_FIRST)) {
            xPath = xPath.substring(0, xPath.length() - PREDICATE_IS_FIRST.length());
        }
        result.setAttribute(ATTR_NAME, xPath);
    }
}
