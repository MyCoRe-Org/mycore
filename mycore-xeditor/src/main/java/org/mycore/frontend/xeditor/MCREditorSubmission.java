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
import java.util.List;
import java.util.Map;

import org.jaxen.JaxenException;
import org.jdom2.JDOMException;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.common.xml.MCRXMLHelper;

public class MCREditorSubmission {

    public static final String PREFIX_DEFAULT_VALUE = "_xed_default_";

    private MCREditorSession session;

    private MCRFieldMapper fieldMapper;

    private Map<String, SubmittedField> fields = new HashMap<String, SubmittedField>();

    public MCREditorSubmission(MCREditorSession session) {
        this.session = session;
        this.fieldMapper = MCRFieldMapper.decodeFromXML(session.getEditedXML());
    }

    public void setSubmittedValues(Map<String, String[]> params) throws JaxenException, JDOMException {
        handleLegacySubmissions(params);
        collectSubmittedFields(params);
        collectDefaultValues(params);
        setSubmittedValues();

        fieldMapper.emptyNotResubmittedNodes();
        session.setBreakpoint("After setting submitted values");
    }

    /** Legacy submission use absolute XPath as field name **/
    private void handleLegacySubmissions(Map<String, String[]> params) throws JaxenException {
        for (String paramName : params.keySet()) {
            if (paramName.startsWith("/")) {
                MCRBinding binding = new MCRBinding(paramName, true, session.getRootBinding());
                setSubmittedValues(binding, params.get(paramName));
            }
        }
    }

    private void collectSubmittedFields(Map<String, String[]> params) {
        params.keySet().stream().filter(name -> fieldMapper.hasField(name)).forEach(name -> {
            SubmittedField field = new SubmittedField(name);
            field.values = params.get(name);
        });
    }

    private void collectDefaultValues(Map<String, String[]> params) {
        params.keySet().stream().filter(name -> name.startsWith(PREFIX_DEFAULT_VALUE)).forEach(name -> {
            String defaultValue = params.get(name)[0];

            SubmittedField field = fields.containsKey(name) ? fields.get(name) : new SubmittedField(name);
            if (field.values.length == 0) {
                field.values = new String[1];
                field.values[0] = defaultValue;
            } else {
                // Replace all empty string submissions with default value
                for (int i = 0; i < field.values.length; i++) {
                    if (field.values[i].isEmpty()) {
                        field.values[i] = defaultValue;
                    }
                }
            }
        });
    }

    private void setSubmittedValues() {
        for (SubmittedField field : fields.values()) {
            if (fieldMapper.hasField(field.name)) {
                List<Object> nodes = fieldMapper.removeResubmittedValueNodes(field.name, field.values.length);
                MCRBinding binding = new MCRBinding(nodes);
                setSubmittedValues(binding, field.values);
            }
        }
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

    class SubmittedField {
        String name;
        String[] values = {};

        SubmittedField(String name) {
            this.name = name;
            fields.put(name, this);
        }
    }
}
