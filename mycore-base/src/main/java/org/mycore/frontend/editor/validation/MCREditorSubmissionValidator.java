package org.mycore.frontend.editor.validation;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.frontend.editor.MCREditorDefReader;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;
import org.mycore.frontend.editor.validation.value.MCRRequiredValidator;

public class MCREditorSubmissionValidator {

    private final static Logger LOGGER = Logger.getLogger(MCREditorSubmissionValidator.class);

    private MCREditorSubmission sub;

    private MCRRequestParameters parms;

    private Element editor;

    private MCRValidationResults results;

    public MCREditorSubmissionValidator(MCRRequestParameters parms, MCREditorSubmission sub, Element editor) {
        this.parms = parms;
        this.sub = sub;
        this.editor = editor;
    }

    public MCRValidationResults validate() {
        LOGGER.info("Validating editor input... ");
        results = new MCRValidationResults();
        validateConditionsOnSingleValue();
        validateConditionsOnPanel();
        return results;
    }

    private void validateConditionsOnPanel() {
        for (Enumeration<String> e = parms.getParameterNames(); e.hasMoreElements();) {
            String name = e.nextElement();

            if (!name.startsWith("_cond-")) {
                continue;
            }

            String path = name.substring(6);
            String[] ids = parms.getParameterValues(name);
            if (ids == null)
                return;

            for (String id : ids) {
                Element condition = MCREditorDefReader.findElementByID(id, editor);

                if (condition == null) {
                    continue;
                }

                String field1 = condition.getAttributeValue("field1", "");
                String field2 = condition.getAttributeValue("field2", "");

                if (!field1.isEmpty()) {
                    validateConditionOnPair(parms, path, condition, field1, field2);
                } else {
                    XPathExpression<Element> xp = XPathFactory.instance().compile(path, Filters.element());
                    Element current = xp.evaluateFirst(sub.getXML());
                    if (current == null) {
                        LOGGER.debug("Skipping further validation, because no element found at xpath " + path);
                        continue;
                    }

                    validateConditionOnElement(parms, path, condition, current);
                }
            }
        }
    }

    private void validateConditionOnElement(MCRRequestParameters parms, String path, Element condition, Element current) {
        MCRValidator validator = MCRValidatorBuilder.buildPredefinedCombinedElementValidator();
        MCRCondition c = new MCRCondition(condition, validator);
        if (!validator.isValid(current)) {
            String sortNr = getSortNr(path);
            results.addFailedValidation(c, sortNr);
        }
    }

    private String getSortNr(String path) {
        return parms.getParameter(sortNrPrefix + path);
    }

    private void validateConditionOnPair(MCRRequestParameters parms, String path, Element condition, String field1, String field2) {
        String pathA = path + "/" + field1;
        String pathB = path + "/" + field2;

        String valueA = parms.getParameter(pathA);
        String valueB = parms.getParameter(pathB);

        String sortNrA = getSortNr(pathA);
        String sortNrB = getSortNr(pathB);

        boolean pairValuesAlreadyInvalid = results.validationFailed(sortNrA, sortNrB);

        if (!pairValuesAlreadyInvalid) {
            MCRValidator validator = MCRValidatorBuilder.buildPredefinedCombinedPairValidator();
            MCRCondition c = new MCRCondition(condition, validator);
            if (!validator.isValid(valueA, valueB)) {
                results.addFailedValidation(c, sortNrA, sortNrB);
            }
        }
    }

    private void validateConditionsOnSingleValue() {
        List<String> parameterNames = getParameterNamesThatHaveSortNr();

        for (String name : parameterNames) {

            String ID = getSubmittedIDForParameter(name);
            if (ID == null)
                continue;

            Element component = MCREditorDefReader.findElementByID(ID, editor);
            if (component == null)
                continue;

            validateField(name, component);
        }
    }

    private void validateField(String name, Element component) {
        String[] values = getSubmittedValues(name);
        String autoFill = getAutoFillValue(component);
        removeAutoFillValues(values, autoFill);

        List<Element> conditions = (List<Element>) (component.getChildren("condition"));
        for (Element condition : conditions) {

            for (int j = 0; j < values.length; j++) {
                String nname = getRepeatedVariableName(name, j);
                String value = values[j];
                value = (value == null ? "" : value.trim());

                MCRValidator validator;
                if (value.isEmpty()) {
                    validator = new MCRRequiredValidator();
                } else {
                    validator = MCRValidatorBuilder.buildPredefinedCombinedValidator();
                }

                MCRCondition c = new MCRCondition(condition, validator);
                setRequiredProperty(validator, c, nname);

                if (!validator.isValid(value)) {
                    String sortNr = getSortNr(name);
                    results.addFailedValidation(c, sortNr);
                    break;
                }
            }
        }
    }

    private String getRepeatedVariableName(String name, int j) {
        return (j == 0 ? name : name + "[" + (j + 1) + "]");
    }

    private void removeAutoFillValues(String[] values, String autoFill) {
        for (int i = 0; i < values.length; i++)
            if (values[i].trim().equals(autoFill)) {
                values[i] = "";
            }
    }

    private String getSubmittedIDForParameter(String name) {
        String id = parms.getParameter("_id@" + name);
        if ((id == null) || (id.isEmpty()))
            return null;
        else
            return id;
    }
    
    private final static String sortNrPrefix = "_sortnr-";

    private List<String> getParameterNamesThatHaveSortNr() {
        List<String> parameterNames = new ArrayList<String>();

        for (Enumeration<String> e = parms.getParameterNames(); e.hasMoreElements();) {
            String name = e.nextElement();

            if (name.startsWith(sortNrPrefix)) {
                name = name.substring(sortNrPrefix.length());
                parameterNames.add(name);
            }
        }
        return parameterNames;
    }

    private String[] getSubmittedValues(String name) {
        String[] values = parms.getParameterValues(name);
        return (values == null ? new String[] { "" } : values);
    }

    private String getAutoFillValue(Element component) {
        String AUTOFILL = "autofill";

        String attributeValue = component.getAttributeValue(AUTOFILL, "").trim();
        if (!attributeValue.isEmpty())
            return attributeValue;

        String elementValue = component.getChildTextTrim(AUTOFILL);
        if ((elementValue == null) || elementValue.isEmpty())
            return null;
        else
            return elementValue;
    }

    private void setRequiredProperty(MCRValidator validator, MCRCondition condition, String name) {
        boolean repeated = name.endsWith("]");
        boolean required = condition.isRequired() && !repeated;
        validator.setProperty("required", Boolean.toString(required));
    }
}
