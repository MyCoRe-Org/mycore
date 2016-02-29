package org.mycore.pi;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

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

    protected MCRPersistentIdentifierInscriber<T> getSynchronizer() {
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
    public void validateRegistration(MCRBase obj, String additional) throws MCRPersistentIdentifierException, MCRAccessException {
        String type = getType();
        MCRObjectID id = obj.getId();
        if (isRegistered(id, additional)) {
            throw new MCRPersistentIdentifierException("There is already a registered " + type + " for Object " + id.toString() + " and additional " + additional);
        }

        if (getSynchronizer().hasIdentifier(obj, additional)) {
            throw new MCRPersistentIdentifierException("There is already a " + type + " in the Object " + id.toString());
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
    public final MCRPersistentIdentifier fullRegister(MCRBase obj, String additional) throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException {
        this.validateRegistration(obj, additional);
        T identifier = this.registerIdentifier(obj, additional);
        this.getSynchronizer().insertIdentifier(identifier, obj, additional);

        MCRPI databaseEntry = new MCRPI(identifier.asString(), getType(), obj.getId().toString(), additional, this.getRegistrationServiceID(), new Date());
        MCRHIBConnection.instance().getSession().save(databaseEntry);

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

    public final String getType() {
        return this.type;
    }

    protected abstract T registerIdentifier(MCRBase obj, String additional) throws MCRPersistentIdentifierException;

    /**
     * Maybe deletes information in the registration service document deletes
     *
     * @param identifier which is assigned to the object
     * @param obj        which will be deleted
     * @throws MCRPersistentIdentifierException
     */
    public abstract void onDelete(T identifier, MCRBase obj) throws MCRPersistentIdentifierException;

    public abstract void onUpdate(T identifier, MCRBase obj) throws MCRPersistentIdentifierException;

    public boolean isRegistered(MCRObjectID id, String additional) {
        return ((Number)MCRHIBConnection.instance()
                .getSession()
                .createCriteria(MCRPI.class)
                .add(Restrictions.eq("mycoreID", id.toString()))
                .add(Restrictions.eq("type", type))
                .add(Restrictions.eq("additional", additional))
                .add(Restrictions.eq("service", registrationServiceID))
                .setProjection(Projections.rowCount())
                .uniqueResult()).shortValue() != 0;
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
        MCRPersistentIdentifierGenerator<T> persitentIdentifierGenerator =
                MCRConfiguration.instance().<MCRPersistentIdentifierGenerator<T>>getInstanceOf(GENERATOR_CONFIG_PREFIX + getProperties().get("Generator"));
        return persitentIdentifierGenerator.generate(id, additional);
    }

}
