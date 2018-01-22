package org.mycore.pi;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRException;
import org.mycore.common.MCRGsonUTCDateAdapter;
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
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class MCRPIRegistrationService<T extends MCRPersistentIdentifier> {

    public static final String REGISTRATION_CONFIG_PREFIX = "MCR.PI.Registration.";

    public static final String GENERATOR_CONFIG_PREFIX = "MCR.PI.Generator.";

    public static final String METADATA_MANAGER_CONFIG_PREFIX = "MCR.PI.MetadataManager.";

    public static final String METADATA_MANAGER_DEPRECATED_CONFIG_PREFIX = "MCR.PI.Inscriber.";

    public static final String PI_FLAG = "MyCoRe-PI";

    public static final String FLAG_SEPERATOR = "\\";

    protected static final String METADATA_MANAGER_PROPERTY_KEY = "MetadataManager";

    protected static final String METADATA_MANAGER_DEPRECATED_PROPERTY_KEY = "Inscriber";

    private final String registrationServiceID;

    private final String type;

    private static Logger LOGGER = LogManager.getLogger();

    /**
     * This map can will be used to replace the old IdentifierInscriber class names with the new MetadataManager class
     * names
     */
    private static final HashMap<String, String> OLD_CLASS_NEW_CLASS_MAPPING = new HashMap<>();

    static {
        OLD_CLASS_NEW_CLASS_MAPPING
            .put("MCRMODSDOIPersistentIdentifierInscriber", "MCRMODSDOIPersistentIdentifierMetadataManager");
        OLD_CLASS_NEW_CLASS_MAPPING
            .put("MCRMODSURNPersistentIdentifierInscriber", "MCRMODSURNPersistentIdentifierMetadataManager");
        OLD_CLASS_NEW_CLASS_MAPPING.put("MCRURNObjectXPathInscriber", "MCRURNObjectXPathMetadataManager");
    }

    public MCRPIRegistrationService(String registrationServiceID, String identifierType) {
        this.registrationServiceID = registrationServiceID;
        this.type = identifierType;
    }

    public final String getRegistrationServiceID() {
        return registrationServiceID;
    }

    public static String repairDeprecatedClassNames(final String className, String metadataManagerPropertyKey) {
        String newClassName = className;
        for (String key : OLD_CLASS_NEW_CLASS_MAPPING.keySet()) {
            if (className.contains(key)) {
                String replacement = OLD_CLASS_NEW_CLASS_MAPPING.get(key);
                LOGGER.warn("You should replace {} with {} in {}", key, replacement, metadataManagerPropertyKey);
                newClassName = newClassName.replaceAll(key, replacement);
            }
        }
        return newClassName;
    }

    public MCRPersistentIdentifierMetadataManager<T> getMetadataManager() {
        Map<String, String> properties = getProperties();
        MCRConfiguration configuration = MCRConfiguration.instance();

        String metadataManager;
        if (properties.containsKey(METADATA_MANAGER_PROPERTY_KEY)) {
            metadataManager = properties.get(METADATA_MANAGER_PROPERTY_KEY);
        } else if (properties.containsKey(METADATA_MANAGER_DEPRECATED_PROPERTY_KEY)) {
            LOGGER.warn(MessageFormat
                .format("You should use {0}{1}.{2} instead of {3}{4}.{5}", REGISTRATION_CONFIG_PREFIX,
                    getRegistrationServiceID(), METADATA_MANAGER_PROPERTY_KEY, REGISTRATION_CONFIG_PREFIX,
                    getRegistrationServiceID(), METADATA_MANAGER_DEPRECATED_PROPERTY_KEY));
            metadataManager = properties.get(METADATA_MANAGER_DEPRECATED_PROPERTY_KEY);
        } else {
            throw new MCRConfigurationException(
                getRegistrationServiceID() + " has no MetadataManager(or legacy Inscriber)!");
        }

        String className;
        String metadataManagerPropertyKey;

        metadataManagerPropertyKey = METADATA_MANAGER_CONFIG_PREFIX + metadataManager;
        className = configuration.getString(metadataManagerPropertyKey, null);

        if (className == null) {
            metadataManagerPropertyKey = METADATA_MANAGER_DEPRECATED_CONFIG_PREFIX + metadataManager;
            className = configuration.getString(metadataManagerPropertyKey, null);
            if (className == null) {
                throw new MCRConfigurationException(
                    "Missing property: " + METADATA_MANAGER_CONFIG_PREFIX + metadataManager + " or " + metadataManagerPropertyKey);
            }

            LOGGER.warn("You should use {} instead of {}", METADATA_MANAGER_CONFIG_PREFIX + metadataManager,
                METADATA_MANAGER_DEPRECATED_PROPERTY_KEY + metadataManager);
        }

        className = repairDeprecatedClassNames(className, metadataManagerPropertyKey);

        try {
            Class<MCRPersistentIdentifierMetadataManager<T>> classObject = (Class<MCRPersistentIdentifierMetadataManager<T>>) Class
                .forName(className);
            Constructor<MCRPersistentIdentifierMetadataManager<T>> constructor = classObject
                .getConstructor(String.class);
            return constructor.newInstance(metadataManager);
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + metadataManagerPropertyKey + ") not found: " + className, e);
        } catch (NoSuchMethodException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + metadataManagerPropertyKey + ") needs a string constructor: " + className);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new MCRException(e);
        }
    }

    private MCRPersistentIdentifierGenerator<T> getGenerator() {
        Supplier<? extends RuntimeException> generatorPropertiesNotSetError = () -> new MCRConfigurationException(
            "Configuration property " + REGISTRATION_CONFIG_PREFIX + registrationServiceID
                + ".Generator is not set");

        String generatorName = Optional.ofNullable(getProperties().get("Generator"))
            .orElseThrow(generatorPropertiesNotSetError);

        String inscriberPropertyKey = GENERATOR_CONFIG_PREFIX + generatorName;
        String className = MCRConfiguration.instance().getString(inscriberPropertyKey);

        try {
            Class<MCRPersistentIdentifierGenerator<T>> classObject = (Class<MCRPersistentIdentifierGenerator<T>>) Class
                .forName(className);
            Constructor<MCRPersistentIdentifierGenerator<T>> constructor = classObject.getConstructor(String.class);
            return constructor.newInstance(generatorName);
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + inscriberPropertyKey + ") not found: " + className, e);
        } catch (NoSuchMethodException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + inscriberPropertyKey + ") needs a string constructor: " + className);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new MCRException(e);
        }
    }

    public static void addFlagToObject(MCRBase obj, MCRPI databaseEntry) {
        String json = getGson().toJson(databaseEntry);
        obj.getService().addFlag(PI_FLAG, json);
    }

    protected void validatePermission(MCRBase obj) throws MCRAccessException {
        String missingPermission;
        if (!MCRAccessManager.checkPermission(obj.getId(), missingPermission = PERMISSION_WRITE) ||
            !MCRAccessManager
                .checkPermission(obj.getId(), missingPermission = "register-" + getRegistrationServiceID())) {
            throw MCRAccessException
                .missingPermission("Register a " + type + " & Update object.", obj.getId().toString(),
                    missingPermission);
        }
    }

    protected void validateAlreadyCreated(MCRObjectID id, String additional) throws MCRPersistentIdentifierException {
        if (isCreated(id, additional)) {
            throw new MCRPersistentIdentifierException("There is already a registered " + getType() + " for Object "
                + id.toString() + " and additional " + additional);
        }
    }

    private static Gson getGson() {
        return new GsonBuilder()
            .registerTypeAdapter(Date.class, new MCRGsonUTCDateAdapter())
            .setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                String name = fieldAttributes.getName();

                return Stream.of("mcrRevision", "mycoreID", "id", "mcrVersion")
                    .anyMatch(field -> field.equals(name));
            }

            @Override
            public boolean shouldSkipClass(Class<?> aClass) {
                return false;
            }
            }).create();
    }

    /**
     * Validates if an object can get an Identifier assigned from this service! <b>Better call super when overwrite!</b>
     *
     * @param obj
     * @throws MCRPersistentIdentifierException see {@link org.mycore.pi.exceptions}
     * @throws MCRAccessException               if the user does not have the rights to assign a pi to the specific object
     */
    public void validateRegistration(MCRBase obj, String additional)
        throws MCRPersistentIdentifierException, MCRAccessException {
        validateAlreadyCreated(obj.getId(), additional);
        validatePermission(obj);
    }

    /**
     * shorthand for {@link #register(MCRBase, String, boolean)} with update = true
     */
    public T register(MCRBase obj, String additional)
        throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException {
        return register(obj,additional,true);
    }

    /**
     * Adds a identifier to the object.
     * Validates everything, registers a new Identifier, inserts the identifier to object metadata and writes a
     * information to the Database.
     * @param updateObject if true this method calls {@link MCRMetadataManager#update(MCRBase)}
     * @param obj the object which has to be identified
     * @return the assigned Identifier
     * @throws MCRAccessException               the current User doesn't have the rights to insert the Identifier to Metadata
     * @throws MCRActiveLinkException           the {@link MCRPersistentIdentifierMetadataManager} lets
     * {@link org.mycore.datamodel.metadata.MCRMetadataManager#update(MCRObject)} throw this
     * @throws MCRPersistentIdentifierException see {@link org.mycore.pi.exceptions}
     */
    public T register(MCRBase obj, String additional, boolean updateObject)
        throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException {
        this.validateRegistration(obj, additional);
        T identifier = this.registerIdentifier(obj, additional);
        this.getMetadataManager().insertIdentifier(identifier, obj, additional);

        MCRPI databaseEntry = insertIdentifierToDatabase(obj, additional, identifier);

        addFlagToObject(obj, databaseEntry);

        if(updateObject){
            if (obj instanceof MCRObject) {
                MCRMetadataManager.update((MCRObject) obj);
            } else if (obj instanceof MCRDerivate) {
                try {
                    MCRMetadataManager.update((MCRDerivate) obj);
                } catch (IOException e) {
                    throw new MCRPersistentIdentifierException("Error while saving derivate!", e);
                }
            }
        }

        return identifier;
    }

    protected Date provideRegisterDate(MCRBase obj, String additional) {
        return new Date();
    }

    public MCRPI insertIdentifierToDatabase(MCRBase obj, String additional, T identifier) {
        MCRPI databaseEntry = new MCRPI(identifier.asString(), getType(), obj.getId().toString(), additional,
            this.getRegistrationServiceID(), provideRegisterDate(obj, additional));
        MCRHIBConnection.instance().getSession().save(databaseEntry);
        return databaseEntry;
    }

    public final String getType() {
        return this.type;
    }

    protected abstract T registerIdentifier(MCRBase obj, String additional) throws MCRPersistentIdentifierException;

    protected final void onDelete(T identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        delete(identifier, obj, additional);
        MCRPersistentIdentifierManager.getInstance().delete(obj.getId().toString(), additional, getType(),
            this.getRegistrationServiceID());
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
     * @throws MCRPersistentIdentifierException to abort deletion of the object  or if something went wrong.
     * (E.G. {@link org.mycore.pi.doi.MCRDOIRegistrationService} throws if not superuser tries to delete the object)
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
        return MCRPersistentIdentifierManager.getInstance().isCreated(id, additional, type, registrationServiceID);
    }

    public boolean isRegistered(MCRObjectID id, String additional) {
        return MCRPersistentIdentifierManager.getInstance().isRegistered(id, additional, type, registrationServiceID);
    }

    protected final Map<String, String> getProperties() {
        Map<String, String> propertiesMap = MCRConfiguration.instance()
            .getPropertiesMap(
                REGISTRATION_CONFIG_PREFIX + registrationServiceID
                    + ".");

        Map<String, String> shortened = new HashMap<>();

        propertiesMap.keySet().forEach(key -> {
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
        Gson gson = getGson();
        String stringFlag = flags.stream().filter(_stringFlag -> {
            MCRPI flag = gson.fromJson(_stringFlag, MCRPI.class);
            return flag.getAdditional().equals(additional) && flag.getIdentifier().equals(mcrpi.getIdentifier());
        }).findAny().orElseThrow(() -> new MCRException(new MCRPersistentIdentifierException(
            "Could find flag to update (" + id + "," + additional + "," + mcrpi.getIdentifier() + ")")));

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
