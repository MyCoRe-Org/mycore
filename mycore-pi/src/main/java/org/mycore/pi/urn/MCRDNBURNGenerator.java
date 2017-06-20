package org.mycore.pi.urn;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPersistentIdentifierGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public abstract class MCRDNBURNGenerator extends MCRPersistentIdentifierGenerator<MCRDNBURN> {
    public MCRDNBURNGenerator(String generatorID) {
        super(generatorID);
    }

    protected abstract String buildNISS();

    public MCRDNBURN generate(@NotNull String namespace, String additional) throws MCRPersistentIdentifierException {
        Objects.requireNonNull(namespace, "Namespace for an URN must not be null!");
        return new MCRDNBURN(namespace, buildNISS());
    }

    public MCRDNBURN generate(MCRObjectID mcrID, String additional) throws MCRPersistentIdentifierException {
        return generate(getProperties().get("Namespace"), additional);
    }

}
