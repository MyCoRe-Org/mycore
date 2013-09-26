package org.mycore.frontend.xeditor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jaxen.JaxenException;
import org.jdom2.JDOMException;

public class MCREditorSubmission {

    private Set<String> xPaths2CheckResubmission = new HashSet<String>();

    private MCREditorSession session;

    public MCREditorSubmission(MCREditorSession session) {
        this.session = session;
    }

    public void mark2checkResubmission(MCRBinding binding) {
        for (Object node : binding.getBoundNodes())
            xPaths2CheckResubmission.add(MCRXPathBuilder.buildXPath(node));
    }

    public Set<String> getXPaths2CheckResubmission() {
        return xPaths2CheckResubmission;
    }

    public void setXPaths2CheckResubmission(String[] xPaths) {
        xPaths2CheckResubmission.clear();
        if (xPaths != null)
            for (String xPath : xPaths)
                xPaths2CheckResubmission.add(xPath);
    }

    private void removeXPaths2CheckResubmission(MCRBinding binding) {
        for (Object node : binding.getBoundNodes())
            xPaths2CheckResubmission.remove(MCRXPathBuilder.buildXPath(node));
    }

    public void emptyNotResubmittedNodes() throws JDOMException, JaxenException {
        for (String xPath : xPaths2CheckResubmission) {
            MCRBinding binding = new MCRBinding(xPath, session.getRootBinding());
            binding.setValue("");
            binding.detach();
        }
    }

    public void setSubmittedValues(Map<String, String[]> values) throws JaxenException, JDOMException {
        setXPaths2CheckResubmission(values.get("_xed_check"));

        for (String xPath : values.keySet())
            if (xPath.startsWith("/"))
                setSubmittedValues(xPath, values.get(xPath));

        emptyNotResubmittedNodes();
        session.setBreakpoint("After setting submitted values");

    }

    public void setSubmittedValues(String xPath, String[] values) throws JDOMException, JaxenException {
        MCRBinding binding = new MCRBinding(xPath, session.getRootBinding());
        List<Object> boundNodes = binding.getBoundNodes();

        while (boundNodes.size() < values.length)
            binding.cloneBoundElement(boundNodes.size() - 1);

        for (int i = 0; i < values.length; i++) {
            String value = values[i] == null ? "" : values[i].trim();
            binding.setValue(i, value);
        }

        removeXPaths2CheckResubmission(binding);
        binding.detach();
    }

}
