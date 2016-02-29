package org.mycore.pi;


import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * Should be able to insert/remove DOI, URN or other identifiers to metadata and check if they already have a Identifier of type T
 * @param <T>
 */
public interface MCRPersistentIdentifierInscriber<T extends MCRPersistentIdentifier> {
    void insertIdentifier(T identifier, MCRBase obj, String additional) throws MCRPersistentIdentifierException;
    void removeIdentifier(T identifier, MCRBase obj, String additional);
    boolean hasIdentifier(MCRBase obj, String additional) throws MCRPersistentIdentifierException;
}
