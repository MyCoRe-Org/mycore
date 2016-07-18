package org.mycore.pi;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.backend.MCRPI_;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

public abstract class MCRPIRegistrationService<T extends MCRPersistentIdentifier> {

    public static final String REGISTRATION_CONFIG_PREFIX = "MCR.PI.Registration.";

    public static final String GENERATOR_CONFIG_PREFIX = "MCR.PI.Generator.";

    public static final String INSCRIBER_CONFIG_PREFIX = "MCR.PI.Inscriber.";
    public static final String PI_FLAG = "MyCoRe-PI";
    public static final String FLAG_SEPERATOR = "\\";

    private final String registrationServiceID;

    private final String type;

    public MCRPIRegistrationService(String registrationServiceID, String type) {
        this.registrationServiceID = registrationServiceID;
        this.type = type;
    }

    public final String getRegistrationServiceID() {
        return registrationServiceID;
    }

    protected MCRPersistentIdentifierInscriber<T> getInscriber() {
        String inscriberName = getProperties().get("Inscriber");
        String inscriberPropertyKey = INSCRIBER_CONFIG_PREFIX + inscriberName;
        String className = MCRConfiguration.instance().getString(inscriberPropertyKey);

        try {
            Class<MCRPersistentIdentifierInscriber<T>> classObject = (Class<MCRPersistentIdentifierInscriber<T>>) Class.forName(className);
            Constructor<MCRPersistentIdentifierInscriber<T>> constructor = classObject.getConstructor(String.class);
            MCRPersistentIdentifierInscriber<T> identifierInscriber = constructor.newInstance(inscriberName);
            return identifierInscriber;
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException("Configurated class (" + inscriberPropertyKey + ") not found: " + className, e);
        } catch (NoSuchMethodException e) {
            throw new MCRConfigurationException("Configurated class (" + inscriberPropertyKey + ") needs a string constructor: " + className);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new MCRException(e);
        }
    }

    private MCRPersistentIdentifierGenerator<T> getGenerator() {
        String generatorName = getProperties().get("Generator");
        String inscriberPropertyKey = GENERATOR_CONFIG_PREFIX + generatorName;
        String className = MCRConfiguration.instance().getString(inscriberPropertyKey);

        try {
            Class<MCRPersistentIdentifierGenerator<T>> classObject = (Class<MCRPersistentIdentifierGenerator<T>>) Class.forName(className);
            Constructor<MCRPersistentIdentifierGenerator<T>> constructor = classObject.getConstructor(String.class);
            MCRPersistentIdentifierGenerator<T> identifierGenerator = constructor.newInstance(generatorName);
            return identifierGenerator;
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException("Configurated class (" + inscriberPropertyKey + ") not found: " + className, e);
        } catch (NoSuchMethodException e) {
            throw new MCRConfigurationException("Configurated class (" + inscriberPropertyKey + ") needs a string constructor: " + className);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new MCRException(e);
        }
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
        validateAlreadyCreated(id, additional);
        validateAlreadyInscribed(obj, additional, type, id);
        validatePermission(obj);
    }

    protected void validatePermission(MCRBase obj) throws MCRAccessException {
        if (!MCRAccessManager.checkPermission(obj.getId(), PERMISSION_WRITE)) {
            throw MCRAccessException.missingPermission("Update object.", obj.getId().toString(),
                    PERMISSION_WRITE);
        }
    }

    protected void validateAlreadyInscribed(MCRBase obj, String additional, String type, MCRObjectID id) throws MCRPersistentIdentifierException {
        if (getInscriber().hasIdentifier(obj, additional)) {
            throw new MCRPersistentIdentifierException(
                "There is already a " + type + " in the Object " + id.toString());
        }
    }

    protected void validateAlreadyCreated(MCRObjectID id, String additional) throws MCRPersistentIdentifierException {
        if (isCreated(id, additional)) {
            throw new MCRPersistentIdentifierException("There is already a registered " + getType() + " for Object "
                    + id.toString() + " and additional " + additional);
        }
    }

    public static void addFlagToObject(MCRBase obj, MCRPI databaseEntry) {
        String json = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                String name = fieldAttributes.getName();
                return name.equals("mcrRevision") || name.equals("mycoreID") || name.equals("id") || name.equals("mcrVersion");
            }

            @Override
            public boolean shouldSkipClass(Class<?> aClass) {
                return false;
            }
        }).create().toJson(databaseEntry);
        obj.getService().addFlag(PI_FLAG, json);
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
    public MCRPersistentIdentifier fullRegister(MCRBase obj, String additional)
        throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException {
        this.validateRegistration(obj, additional);
        T identifier = this.registerIdentifier(obj, additional);
        this.getInscriber().insertIdentifier(identifier, obj, additional);

        MCRPI databaseEntry = new MCRPI(identifier.asString(), getType(), obj.getId().toString(), additional,
                this.getRegistrationServiceID(), provideRegisterDate(obj, additional));
        MCRHIBConnection.instance().getSession().save(databaseEntry);

        addFlagToObject(obj, databaseEntry);

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

    protected Date provideRegisterDate(MCRBase obj, String additional) {
        return new Date();
    }

    public final String getType() {
        return this.type;
    }

    protected abstract T registerIdentifier(MCRBase obj, String additional) throws MCRPersistentIdentifierException;

    protected final void onDelete(T identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        delete(identifier, obj, additional);
        MCRPersistentIdentifierManager.getInstance().delete(obj.getId().toString(), additional, getType(), this.getRegistrationServiceID());
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

    public boolean isCreated(MCRObjectID id, String additional) {
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
                                cb.equal(pi.get(MCRPI_.service), registrationServiceID),
                                cb.isNotNull(pi.get(MCRPI_.registered))))
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
        MCRPersistentIdentifierGenerator<T> persitentIdentifierGenerator = getGenerator();
        return persitentIdentifierGenerator.generate(id, additional);
    }

    protected MCRPI getTableEntry(MCRObjectID id, String additional) {
        return MCRPersistentIdentifierManager.getInstance().get(getRegistrationServiceID(), id.toString(), additional);
    }

    public void updateFlag(MCRObjectID id, String additional, MCRPI mcrpi) {
        MCRBase obj = MCRMetadataManager.retrieve(id);
        MCRObjectService service = obj.getService();
        ArrayList<String> flags = service.getFlags(MCRPIRegistrationService.PI_FLAG);
        Gson gson = new Gson();
        String stringFlag = flags.stream().filter(_stringFlag -> {
            MCRPI flag = gson.fromJson(_stringFlag, MCRPI.class);
            return flag.getAdditional().equals(additional) && flag.getIdentifier().equals(mcrpi.getIdentifier());
        }).findAny().orElseThrow(() -> new MCRException(new MCRPersistentIdentifierException("Could find flag to update (" + id + "," + additional + "," + mcrpi.getIdentifier() + ")")));

        int flagIndex = service.getFlagIndex(stringFlag);
        service.removeFlag(flagIndex);

        addFlagToObject(obj, mcrpi);
        try {
            MCRMetadataManager.update(obj);
        } catch (IOException | MCRAccessException | MCRActiveLinkException e) {
            throw new MCRException("Could not update flags", e);
        }
    }
}
