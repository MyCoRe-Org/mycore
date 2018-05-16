package org.mycore.pi.doi;

import java.util.Optional;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * Just for testing. Provides the doi in the doi property.
 */
public class MCRStaticDOIGenerator extends MCRPIGenerator<MCRDigitalObjectIdentifier> {

    public MCRStaticDOIGenerator(String generatorID) {
        super(generatorID);
        checkPropertyExists("doi");
    }

    @Override public MCRDigitalObjectIdentifier generate(MCRBase mcrBase, String additional)
        throws MCRPersistentIdentifierException {
        final String doi = getProperties().get("doi");
        final Optional<MCRDigitalObjectIdentifier> generatedDOI = new MCRDOIParser().parse(doi);
        return generatedDOI.orElseThrow(()-> new MCRConfigurationException("The property " + doi+" is not a valid doi!"));
    }

}
