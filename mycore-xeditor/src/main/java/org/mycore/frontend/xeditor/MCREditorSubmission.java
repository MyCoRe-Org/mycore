/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.frontend.xeditor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jaxen.JaxenException;
import org.jdom2.JDOMException;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.common.xml.MCRXPathBuilder;

public class MCREditorSubmission {

    public static final String PREFIX_DEFAULT_VALUE = "_xed_default_";

    public static final String PREFIX_CHECK_RESUBMISSION = "_xed_check";

    private Set<String> xPaths2CheckResubmission = new LinkedHashSet<>();

    private Map<String, String> xPath2DefaultValue = new LinkedHashMap<>();

    private MCREditorSession session;

    public MCREditorSubmission(MCREditorSession session) {
        this.session = session;
    }

    public void clear() {
        xPaths2CheckResubmission.clear();
        xPath2DefaultValue.clear();
    }

    public void mark2checkResubmission(MCRBinding binding) {
        for (Object node : binding.getBoundNodes()) {
            xPaths2CheckResubmission.add(MCRXPathBuilder.buildXPath(node));
        }
    }

    public String getXPaths2CheckResubmission() {
        StringBuilder sb = new StringBuilder();
        for (String xPath : xPaths2CheckResubmission) {
            xPath = xPath.substring(xPath.indexOf("/", 1) + 1);
            sb.append(xPath).append(" ");
        }
        return sb.toString().trim();
    }

    public void setXPaths2CheckResubmission(String xPaths) throws JDOMException {
        xPaths2CheckResubmission.clear();
        String rootXPath = MCRXPathBuilder.buildXPath(session.getEditedXML().getRootElement()) + "/";
        if (xPaths != null) {
            for (String xPath : xPaths.split(" ")) {
                xPaths2CheckResubmission.add(rootXPath + xPath);
            }
        }
    }

    private void removeXPaths2CheckResubmission(MCRBinding binding) {
        for (Object node : binding.getBoundNodes()) {
            xPaths2CheckResubmission.remove(MCRXPathBuilder.buildXPath(node));
        }
    }

    public void emptyNotResubmittedNodes() throws JDOMException, JaxenException {
        for (String xPath : xPaths2CheckResubmission) {
            MCRBinding binding = new MCRBinding(xPath, false, session.getRootBinding());
            binding.setValue("");
            binding.detach();
        }
    }

    public void markDefaultValue(String xPath, String defaultValue) {
        xPath2DefaultValue.put(xPath, defaultValue);
    }

    public Map<String, String> getDefaultValues() {
        return xPath2DefaultValue;
    }

    public void setSubmittedValues(Map<String, String[]> values) throws JaxenException, JDOMException {
        String[] xPaths2Check = values.get(PREFIX_CHECK_RESUBMISSION);
        if ((xPaths2Check != null) && (xPaths2Check.length > 0)) {
            setXPaths2CheckResubmission(xPaths2Check[0]);
        }

        xPath2DefaultValue.clear();

        Map<MCRBinding, String[]> valuesToSet = new HashMap<>();

        for (String paramName : values.keySet()) {
            if (paramName.startsWith("/")) {
                MCRBinding binding = new MCRBinding(paramName, true, session.getRootBinding());
                valuesToSet.put(binding, values.get(paramName));
            } else if (paramName.startsWith(PREFIX_DEFAULT_VALUE)) {
                String xPath = paramName.substring(PREFIX_DEFAULT_VALUE.length());

                MCRBinding binding = new MCRBinding(xPath, false, session.getRootBinding());
                boolean noSuchNode = binding.getBoundNodes().isEmpty();
                binding.detach();
                if (noSuchNode) {
                    continue;
                }

                String defaultValue = values.get(paramName)[0];
                markDefaultValue(xPath, defaultValue);
            }
        }

        for (MCRBinding binding : valuesToSet.keySet()) {
            setSubmittedValues(binding, valuesToSet.get(binding));
        }

        emptyNotResubmittedNodes();
        setDefaultValues();

        session.setBreakpoint("After setting submitted values");
    }

    private void setSubmittedValues(MCRBinding binding, String[] values) throws JDOMException, JaxenException {
        List<Object> boundNodes = binding.getBoundNodes();

        while (boundNodes.size() < values.length) {
            binding.cloneBoundElement(boundNodes.size() - 1);
        }

        for (int i = 0; i < values.length; i++) {
            String value = values[i] == null ? "" : values[i].trim();
            value = MCRXMLFunctions.normalizeUnicode(value);
            value = MCRXMLHelper.removeIllegalChars(value);
            binding.setValue(i, value);
        }

        removeXPaths2CheckResubmission(binding);
        binding.detach();
    }

    public void setDefaultValues() throws JDOMException, JaxenException {
        MCRBinding rootBinding = session.getRootBinding();
        for (String xPath : xPath2DefaultValue.keySet()) {
            String defaultValue = xPath2DefaultValue.get(xPath);
            MCRBinding binding = new MCRBinding(xPath, false, rootBinding);
            binding.setDefault(defaultValue);
            binding.detach();
        }
    }
}
