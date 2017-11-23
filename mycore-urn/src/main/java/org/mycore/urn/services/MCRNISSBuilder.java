package org.mycore.urn.services;

/**
 * Implementations of this interface provide different strategies to generate a
 * NISS (namespace specific string) for a new URN. Each subnamespace
 * configuration can have its own instance. A NISS must be a unique ID within
 * the subnamespace.
 *
 * MCR.URN.SubNamespace.[ConfigID].NISSBuilder=[Class], for example
 * MCR.URN.SubNamespace.Essen.NISSBuilder=org.mycore.urn.services.MCRNISSBuilderDateCounter
 *
 * @author Frank Lützenkirchen
 */
public interface MCRNISSBuilder {
    /**
     * Initializes this instance of a MCRNISSBuilder. This method is only called
     * once for each instance before this builder is used.
     *
     * @param configID
     *            the ID of a subnamespace configuration in mycore.properties
     */
    void init(String configID);

    /**
     * Builds a new NISS. No MCRNISSBuilder object must generate the same NISS
     * twice, they must ensure the NISS is unique.
     */
    String buildNISS();
}
