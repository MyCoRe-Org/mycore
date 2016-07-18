package org.mycore.pi.urn;


import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPersistentIdentifierGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public abstract class MCRDNBURNGenerator extends MCRPersistentIdentifierGenerator<MCRDNBURN> {
    public MCRDNBURNGenerator(String generatorID) {
        super(generatorID);
    }

    protected abstract String buildNISS();



    @Override
    public MCRDNBURN generate(MCRObjectID mcrID, String additional) throws MCRPersistentIdentifierException {
        return new MCRDNBURN(getProperties().get("Namespace"), buildNISS());
    }


}
