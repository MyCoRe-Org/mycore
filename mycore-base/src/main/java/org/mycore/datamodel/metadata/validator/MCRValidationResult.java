package org.mycore.datamodel.metadata.validator;

/**
 * This class represents the result of a validation process for MCR objects.
 * It provides methods to check if the validation was successful and to retrieve
 * any associated message. The class also includes a static instance representing a valid result.
 * @see #VALID
 */
public abstract class MCRValidationResult {

    /**
     * This method checks if the validation was successful.
     * @return true if the validation was successful, false otherwise
     */
    public abstract boolean isValid();

    /**
     * This method retrieves the message associated with the validation result. Should only be called if isValid()
     * returns false.
     * @return the message associated with the validation result or an empty string if the validation was successful.
     */
    public abstract String getMessage();

    public static final MCRValidationResult VALID = new MCRValidationResult() {
        @Override
        public String getMessage() {
            return "";
        }

        @Override
        public boolean isValid() {
            return true;
        }
    };
}
