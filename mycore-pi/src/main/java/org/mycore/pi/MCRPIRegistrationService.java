package org.mycore.pi;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.backend.MCRPI_;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public abstract class MCRPIRegistrationService<T extends MCRPersistentIdentifier> {

    public static final String REGISTRATION_CONFIG_PREFIX = "MCR.PI.Registration.";

    public static final String GENERATOR_CONFIG_PREFIX = "MCR.PI.Generator.";

    public static final String INSCRIBER_CONFIG_PREFIX = "MCR.PI.Inscriber.";

    private final String registrationServiceID;

    private final String type;

    public MCRPIRegistrationService(String registrationServiceID, String type) {
        this.registrationServiceID = registrationServiceID;
        this.type = type;
    }

    public final String getRegistrationServiceID() {
        return registrationServiceID;
    }

    public MCRPersistentIdentifierInscriber<T> getSynchronizer() {
        String classProperty = getProperties().get("Inscriber");
        Object inscriber = MCRConfiguration.instance().getInstanceOf(INSCRIBER_CONFIG_PREFIX + classProperty);
        return (MCRPersistentIdentifierInscriber<T>) inscriber;
    }

    /**
     * Validates if a object can get a Identifier assigned from this service! <b>Better call super when overwrite!</b>
     *
     * @param obj
     * @throws MCRPersistentIdentifierException see {@link org.mycore.pi.exceptions}
     * @throws MCRAccessException               if the user does not have the rights to assign a pi to the specific object
     */
    public void validateRegistration(MCRBase obj, String additional)
        throws MCRPersistentIdentifierException, MCRAccessException {
        String type = getType();
        MCRObjectID id = obj.getId();
        if (isRegistered(id, additional)) {
            throw new MCRPersistentIdentifierException("There is already a registered " + type + " for Object "
                + id.toString() + " and additional " + additional);
        }

        if (getSynchronizer().hasIdentifier(obj, additional)) {
            throw new MCRPersistentIdentifierException(
                "There is already a " + type + " in the Object " + id.toString());
        }

        if (!MCRAccessManager.checkPermission(obj.getId(), PERMISSION_WRITE)) {
            throw MCRAccessException.missingPermission("Update object.", obj.getId().toString(),
                PERMISSION_WRITE);
        }
    }

    /**
     * Validates everything, registers a new Identifier, inserts the identifier to object metadata and writes a information to the Database.
     *
     * @param obj the object which has to be identified
     * @return the assigned Identifier
     * @throws MCRAccessException               the current User doesn't have the rights to insert the Identifier to Metadata
     * @throws MCRActiveLinkException           the {@link MCRPersistentIdentifierInscriber} lets {@link MCRMetadataManager#update(MCRObject)} throw this
     * @throws MCRPersistentIdentifierException see {@link org.mycore.pi.exceptions}
     */
    public final MCRPersistentIdentifier fullRegister(MCRBase obj, String additional)
        throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException {
        this.validateRegistration(obj, additional);
        T identifier = this.registerIdentifier(obj, additional);
        this.getSynchronizer().insertIdentifier(identifier, obj, additional);

        insertIdentifierToDatabase(obj, additional, identifier);

        if (obj instanceof MCRObject) {
            MCRMetadataManager.update((MCRObject) obj);
        } else if (obj instanceof MCRDerivate) {
            try {
                MCRMetadataManager.update((MCRDerivate) obj);
            } catch (IOException e) {
                throw new MCRPersistentIdentifierException("Error while saving derivate!", e);
            }
        }
        return identifier;
    }

    public void insertIdentifierToDatabase(MCRBase obj, String additional, T identifier) {
        MCRPI databaseEntry = new MCRPI(identifier.asString(), getType(), obj.getId().toString(), additional,
            this.getRegistrationServiceID(), new Date());
        MCRHIBConnection.instance().getSession().save(databaseEntry);
    }

    public final String getType() {
        return this.type;
    }

    protected abstract T registerIdentifier(MCRBase obj, String additional) throws MCRPersistentIdentifierException;

    protected final void onDelete(T identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        delete(identifier, obj, additional);
        MCRPersistentIdentifierManager.delete(obj.getId().toString(), getType(), this.getRegistrationServiceID());
    }

    protected final void onUpdate(T identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        update(identifier, obj, additional);
    }

    /**
     * Should handle deletion of a Object with the PI.
     * E.g. The {@link org.mycore.pi.doi.MCRDOIRegistrationService} sets the active flag in Datacite datacentre to false.
     * @param identifier the Identifier
     * @param obj the deleted object
     * @param additional
     * @throws MCRPersistentIdentifierException to abort deletion of the object  or if something went wrong. (E.G. {@link org.mycore.pi.doi.MCRDOIRegistrationService} throws if not superuser tries to delete the object)
     */
    protected abstract void delete(T identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException;

    /**
     * Should handle updates of a Object with the PI.
     * E.g. The {@link org.mycore.pi.doi.MCRDOIRegistrationService} sends the updated metadata to the Datacite datacentre.
     * @param identifier the Identifier
     * @param obj the deleted object
     * @param additional
     * @throws MCRPersistentIdentifierException to abort update of the object or if something went wrong.
     */
    protected abstract void update(T identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException;

    public boolean isRegistered(MCRObjectID id, String additional) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Number> query = cb.createQuery(Number.class);
        Root<MCRPI> pi = query.from(MCRPI.class);
        return em.createQuery(
            query
                .select(cb.count(pi))
                .where(
                    cb.equal(pi.get(MCRPI_.mycoreID), id.toString()),
                    cb.equal(pi.get(MCRPI_.type), type),
                    cb.equal(pi.get(MCRPI_.additional), additional),
                    cb.equal(pi.get(MCRPI_.service), registrationServiceID)))
            .getSingleResult().shortValue() > 0;
    }

    protected final Map<String, String> getProperties() {
        Map<String, String> propertiesMap = MCRConfiguration.instance()
            .getPropertiesMap(REGISTRATION_CONFIG_PREFIX + registrationServiceID + ".");

        Map<String, String> shortened = new HashMap<>();

        propertiesMap.keySet().stream().forEach(key -> {
            String newKey = key.substring(REGISTRATION_CONFIG_PREFIX.length() + registrationServiceID.length() + 1);
            shortened.put(newKey, propertiesMap.get(key));
        });

        return shortened;
    }

    protected final T getNewIdentifier(MCRObjectID id, String additional) throws MCRPersistentIdentifierException {
        MCRPersistentIdentifierGenerator<T> persitentIdentifierGenerator = MCRConfiguration.instance()
            .<MCRPersistentIdentifierGenerator<T>> getInstanceOf(
                GENERATOR_CONFIG_PREFIX + getProperties().get("Generator"));
        return persitentIdentifierGenerator.generate(id, additional);
    }

}
