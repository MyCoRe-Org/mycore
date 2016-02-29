package org.mycore.pi;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public interface MCRPersistentIdentifierGenerator<T extends MCRPersistentIdentifier> {
    T generate(MCRObjectID mcrID, String additional) throws MCRPersistentIdentifierException;
}
