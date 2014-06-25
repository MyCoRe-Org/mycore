package org.mycore.frontend.editor.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jdom2.Element;

public class MCRValidationResults {

    private Map<String, MCRCondition> failed = new HashMap<String, MCRCondition>();

    public void addFailedValidation(MCRCondition condition, String... sortNrs) {
        for (String sortNr : sortNrs)
            failed.put(sortNr, condition);
    }

    public boolean validationFailed() {
        return !failed.isEmpty();
    }

    public boolean validationFailed(String... sortNrs) {
        for (String sortNr : sortNrs)
            if (failed.containsKey(sortNr))
                return true;

        return false;
    }

    public Element buildXML() {
        Element failedConds = new Element("failed");

        for (Entry<String, MCRCondition> entry : failed.entrySet()) {
            String sortNr = entry.getKey();
            MCRCondition condition = entry.getValue();

            Element field = new Element("field");
            field.setAttribute("sortnr", sortNr);
            field.setAttribute("condition", condition.getID());
            failedConds.addContent(field);
        }
        return failedConds;
    }
}
