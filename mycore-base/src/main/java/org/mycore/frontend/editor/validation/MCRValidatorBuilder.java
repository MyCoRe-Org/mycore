package org.mycore.frontend.editor.validation;

public class MCRValidatorBuilder {

    public static MCRCombinedValidator buildPredefinedCombinedValidator() {
        MCRCombinedValidator validator = new MCRCombinedValidator();
        validator.addValidator(new MCRMaxLengthValidator());
        validator.addValidator(new MCRMinLengthValidator());
        validator.addValidator(new MCRRegExpValidator());
        validator.addValidator(new MCRXSLConditionValidator());
        validator.addValidator(new MCRExternalValidator());
        validator.addValidator(new MCRDateTimeValidator());
        validator.addValidator(new MCRMaxDateTimeValidator());
        validator.addValidator(new MCRMinDateTimeValidator());
        validator.addValidator(new MCRIntegerValidator());
        validator.addValidator(new MCRMaxIntegerValidator());
        validator.addValidator(new MCRMinIntegerValidator());
        validator.addValidator(new MCRDecimalValidator());
        validator.addValidator(new MCRMaxDecimalValidator());
        validator.addValidator(new MCRMinDecimalValidator());
        validator.addValidator(new MCRMaxStringValidator());
        validator.addValidator(new MCRMinStringValidator());
        return validator;
    }

    public static MCRCombinedPairValidator buildPredefinedCombinedPairValidator() {
        MCRCombinedPairValidator validator = new MCRCombinedPairValidator();
        validator.addValidator(new MCRStringPairValidator());
        validator.addValidator(new MCRIntegerPairValidator());
        return validator;
    }
}
