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

import org.jdom2.Element;
import org.mycore.common.xml.MCRURIResolver;

/**
 * Helps transforming xed:load-resource elements. 
 * 
 * @author Frank Lützenkirchen
 */
public class MCRLoadResourceTransformerHelper extends MCRTransformerHelperBase {

    private static final String ATTR_URI = "uri";
    private static final String ATTR_NAME = "name";

    @Override
    List<String> getSupportedMethods() {
        return Arrays.asList("load-resource");
    }

    @Override
    void handle(MCRTransformerHelperCall call) {
        String uri = replaceXPaths(call.getAttributeValue(ATTR_URI));
        String name = call.getAttributeValue(ATTR_NAME);

        Element resource = MCRURIResolver.obtainInstance().resolve(uri);
        getSession().getVariables().put(name, resource);
    }
}
