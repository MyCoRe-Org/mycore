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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Parent;
import org.mycore.common.xml.MCRXPathEvaluator;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCRIncludeHandler;

/**
 * While transforming xed to html, holds state information 
 * necessary for implementing the transformation. 
 * 
 * @author Frank Lützenkirchen
 */
public class MCRTransformationState {

    private final MCREditorSession session;

    private MCRBinding currentBinding;
    
    private MCRIncludeHandler includeHandler = new MCRIncludeHandler();

    private Map<String, MCRTransformerHelperBase> method2helper = new HashMap<>();

    public MCRTransformationState(MCREditorSession session) {
        this.session = session;

        List<MCRTransformerHelperBase> helpers = Arrays.asList(
            new MCRSelectTransformerHelper(),
            new MCRValidationTransformerHelper(),
            new MCRRepeatTransformerHelper(),
            new MCRConditionTransformerHelper(),
            new MCROutputTransformerHelper(),
            new MCRPostProcessorTransformerHelper(),
            new MCRSourceTransformerHelper(),
            new MCRCancelTransformerHelper(),
            new MCRParamTransformerHelper(),
            new MCRBindTransformerHelper(),
            new MCRUnbindTransformerHelper(),
            new MCRGetAdditionalParamsTransformerHelper(),
            new MCRPreloadTransformerHelper(),
            new MCRIncludeTransformerHelper(),
            new MCRCleanupRuleTransformerHelper(),
            new MCRLoadResourceTransformerHelper(),
            new MCRInputTransformerHelper(),
            new MCRTextareaTransformerHelper(),
            new MCRFormTransformerHelper(),
            new MCRButtonTransformerHelper(),
            new MCRReplaceXPathsTransformerHelper());

        helpers.forEach(helper -> {
            helper.init(this);
            helper.getSupportedMethods().forEach(method -> method2helper.put(method, helper));
        });
    }

    Map<String, MCRTransformerHelperBase> getMethodHelperMap() {
        return method2helper;
    }
    
    MCREditorSession getSession() {
        return session;
    }

    MCRIncludeHandler getIncludeHandler() {
        return includeHandler;
    }

    MCRBinding getCurrentBinding() {
        return currentBinding;
    }

    void setCurrentBinding(MCRBinding binding) {
        this.currentBinding = binding;
        session.getValidator().setValidationMarker(currentBinding);
    }

    boolean hasValue(String value) {
        session.getSubmission().mark2checkResubmission(currentBinding);
        return currentBinding.hasValue(value);
    }

    MCRXPathEvaluator getXPathEvaluator() {
        if (currentBinding != null) {
            return currentBinding.getXPathEvaluator();
        } else {
            return new MCRXPathEvaluator(session.getVariables(), (Parent) null);
        }
    }
}
