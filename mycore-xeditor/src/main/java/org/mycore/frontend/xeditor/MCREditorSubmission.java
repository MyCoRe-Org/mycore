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
import java.util.List;
import java.util.Map;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.common.xml.MCRXMLHelper;

public class MCREditorSubmission {

    public static final String PREFIX_DEFAULT_VALUE = "_xed_default_";

    private Map<String, String> xPath2DefaultValue = new LinkedHashMap<>();

    private MCREditorSession session;

    public MCREditorSubmission(MCREditorSession session) {
        this.session = session;
    }

    public void clear() {
        xPath2DefaultValue.clear();
    }

    public void markDefaultValue(String xPath, String defaultValue) {
        xPath2DefaultValue.put(xPath, defaultValue);
    }

    public Map<String, String> getDefaultValues() {
        return xPath2DefaultValue;
    }

    public void setSubmittedValues(Map<String, String[]> values) throws JaxenException, JDOMException {
        xPath2DefaultValue.clear();

        Map<MCRBinding, String[]> valuesToSet = new HashMap<>();

        Document doc = session.getEditedXML();
        MCRFieldMapper fieldMapper = new MCRFieldMapper(doc);

        for (String paramName : values.keySet()) {
            if (fieldMapper.hasField(paramName)) {
                List<Object> nodes = fieldMapper.removeResubmittedValueNodes(paramName, values.get(paramName).length);
                for( Object node : nodes ) System.out.println(node);
                MCRBinding binding = new MCRBinding(nodes);
                valuesToSet.put(binding, values.get(paramName));
            } else if (paramName.startsWith("/")) {
                MCRBinding binding = new MCRBinding(paramName, true, session.getRootBinding());
                valuesToSet.put(binding, values.get(paramName));
            } else if (paramName.startsWith(PREFIX_DEFAULT_VALUE)) {
                String xPath = paramName.substring(PREFIX_DEFAULT_VALUE.length());
                MCRBinding binding = new MCRBinding(xPath, false, session.getRootBinding());
                boolean nodesExist = binding.getBoundNodes().isEmpty();
                binding.detach();
                if (nodesExist) {
                    String defaultValue = values.get(paramName)[0];
                    markDefaultValue(xPath, defaultValue);
                }
            }
        }

        for (MCRBinding binding : valuesToSet.keySet()) {
            setSubmittedValues(binding, valuesToSet.get(binding));
        }

        fieldMapper.emptyNotResubmittedNodes();
        setDefaultValues();

        session.setBreakpoint("After setting submitted values");
    }

    private void setSubmittedValues(MCRBinding binding, String[] values) {
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

        binding.detach();
    }

    public void setDefaultValues() throws JaxenException {
        MCRBinding rootBinding = session.getRootBinding();
        for (String xPath : xPath2DefaultValue.keySet()) {
            String defaultValue = xPath2DefaultValue.get(xPath);
            MCRBinding binding = new MCRBinding(xPath, false, rootBinding);
            binding.setDefault(defaultValue);
            binding.detach();
        }
    }
}
