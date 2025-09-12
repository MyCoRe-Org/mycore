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

import org.jaxen.JaxenException;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;
import org.mycore.frontend.xeditor.MCRXEditorPostProcessor;

public class MCRPostProcessorTransformerHelper extends MCRTransformerHelperBase {

    private static final String ATTR_CLASS = "class";

    @Override
    Collection<String> getSupportedMethods() {
        return Arrays.asList("post-processor");
    }

    @Override
    void handle(MCRTransformerHelperCall call) throws JaxenException {
        String clazz = call.getAttributeValueOrDefault(ATTR_CLASS, null);
        if (clazz != null) {
            try {
                MCRXEditorPostProcessor instance = ((MCRXEditorPostProcessor) MCRClassTools.forName(clazz)
                    .getDeclaredConstructor()
                    .newInstance());
                state.editorSession.setPostProcessor(instance);
            } catch (ReflectiveOperationException e) {
                throw new MCRException("Could not initialize Post-Processor with class" + clazz, e);
            }
        }
        state.editorSession.getPostProcessor().setAttributes(call.getAttributeMap());
    }

}
