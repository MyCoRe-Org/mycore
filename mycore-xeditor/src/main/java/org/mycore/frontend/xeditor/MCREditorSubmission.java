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

import java.util.Map;

import org.jaxen.JaxenException;
import org.jdom2.JDOMException;
import org.mycore.frontend.xeditor.mapper.MCRField;
import org.mycore.frontend.xeditor.mapper.MCRFieldMapping;
import org.mycore.frontend.xeditor.mapper.MCRMappingDecoder;

public class MCREditorSubmission {

    private MCREditorSession session;

    private MCRFieldMapping fieldMapping = new MCRFieldMapping();

    public MCREditorSubmission(MCREditorSession session) {
        this.session = session;
        MCRMappingDecoder decoder = new MCRMappingDecoder();
        fieldMapping = decoder.decode(session.getEditedXML());
    }

    public void setSubmittedValues(Map<String, String[]> params) throws JaxenException, JDOMException {
        handleLegacySubmissions(params);

        setSubmittedFields(params);
        fieldMapping.setFieldsWithoutValues();

        session.setBreakpoint("After setting submitted values");
    }

    /** Legacy submission use absolute XPath as field name **/
    private void handleLegacySubmissions(Map<String, String[]> params) throws JaxenException {
        for (String paramName : params.keySet()) {
            if (paramName.startsWith("/")) {
                MCRBinding binding = new MCRBinding(paramName, true, session.getRootBinding());
                binding.setValues(params.get(paramName));
                session.getChangeTracker().track(binding.getChanges());
                binding.detach();
            }
        }
    }

    private void setSubmittedFields(Map<String, String[]> params) {
        params.keySet().stream().filter(name -> fieldMapping.hasField(name)).forEach(name -> {
            MCRField field = fieldMapping.getField(name);
            MCRNodes nodes = new MCRNodes(field.getNodes());
            String[] values = params.get(name);
            nodes.setValues(values);
            session.getChangeTracker().track(nodes.getChanges());
            fieldMapping.removeField(field);
        });
    }
}
