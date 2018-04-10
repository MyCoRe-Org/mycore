package org.mycore.pi.doi;

import java.util.Optional;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPersistentIdentifierGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

import static org.mycore.pi.MCRPIRegistrationService.GENERATOR_CONFIG_PREFIX;

/**
 * Just for testing. Provides the doi in the doi property.
 */
public class MCRStaticDOIGenerator extends MCRPersistentIdentifierGenerator<MCRDigitalObjectIdentifier> {

    public MCRStaticDOIGenerator(String generatorID) {
        super(generatorID);
        checkPropertyExists("doi");
    }

    @Override
    public MCRDigitalObjectIdentifier generate(MCRObjectID mcrID, String additional) throws MCRPersistentIdentifierException {
        final String doi = getProperties().get("doi");
        final Optional<MCRDigitalObjectIdentifier> generatedDOI = new MCRDOIParser().parse(doi);
        return generatedDOI.orElseThrow(()-> new MCRConfigurationException("The property " + doi+" is not a valid doi!"));
    }

}
