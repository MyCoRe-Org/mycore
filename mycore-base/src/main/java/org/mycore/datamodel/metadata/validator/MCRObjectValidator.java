package org.mycore.datamodel.metadata.validator;

import org.mycore.datamodel.metadata.MCRObject;

/**
 * This class is an abstract base class for validators that check the validity of MCRObject instances.
 * It provides a method to validate an MCRObject and returns a result indicating the validation status.
 */
public abstract class MCRObjectValidator {

    public abstract MCRValidationResult validate(MCRObject object);

}

