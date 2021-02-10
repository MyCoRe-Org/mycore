/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.mycore.pi;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;
import org.mycore.common.MCRGsonUTCDateAdapter;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.doi.MCRDOIService;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.services.i18n.MCRTranslation;
import org.mycore.util.concurrent.MCRFixedUserCallable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class MCRPIService<T extends MCRPersistentIdentifier> {

    public static final String REGISTRATION_CONFIG_PREFIX = "MCR.PI.Service.";

    public static final String GENERATOR_CONFIG_PREFIX = "MCR.PI.Generator.";

    public static final String METADATA_SERVICE_CONFIG_PREFIX = "MCR.PI.MetadataService.";

    public static final String PI_FLAG = "MyCoRe-PI";

    public static final String GENERATOR_PROPERTY_KEY = "Generator";

    protected static final String METADATA_SERVICE_PROPERTY_KEY = "MetadataService";

    protected static final String TRANSLATE_PREFIX = "component.pi.register.error.";

    private final String registrationServiceID;

    private final String type;

    private static final ExecutorService REGISTER_POOL;

    static {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("MCRPIRegister-#%d")
            .build();
        REGISTER_POOL = Executors.newFixedThreadPool(1, threadFactory);
    }

    public MCRPIService(String registrationServiceID, String identifierType) {
        this.registrationServiceID = registrationServiceID;
        this.type = identifierType;
    }

    // generated identifier is already present in database
    private static final int ERR_CODE_0_1 = 0x0001;

    private static Logger LOGGER = LogManager.getLogger();

    /**
     * Removes a flag from a {@link MCRObject}
     *
     * @param obj           the object
     * @param databaseEntry the database entry
     * @return the remove entry parsed from json or null
     */
    public static MCRPI removeFlagFromObject(MCRBase obj, MCRPI databaseEntry) {
        MCRObjectService service = obj.getService();
        ArrayList<String> flags = service.getFlags(MCRPIService.PI_FLAG);
        int flagCount = flags.size();
        for (int flagIndex = 0; flagIndex < flagCount; flagIndex++) {
            String flag = flags.get(flagIndex);
            MCRPI pi = getGson().fromJson(flag, MCRPI.class);
            if (pi.getIdentifier().equals(databaseEntry.getIdentifier()) &&
                pi.getAdditional().equals(databaseEntry.getAdditional()) &&
                pi.getService().equals(databaseEntry.getService()) &&
                pi.getType().equals(databaseEntry.getType())) {
                service.removeFlag(flagIndex);
                return databaseEntry;
            }
        }
        return null;
    }

    protected static Gson getGson() {
        return new GsonBuilder().registerTypeAdapter(Date.class, new MCRGsonUTCDateAdapter())
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

    public final String getServiceID() {
        return registrationServiceID;
    }

    /**
     * Checks the service parameters.
     *
     * @throws MCRConfigurationException if parameter is missing or wrong!
     */
    protected void checkConfiguration() throws MCRConfigurationException {
        if (getProperties().containsKey("MetadataManager")) {
            throw new MCRConfigurationException("The MCRPIService " + getServiceID() +
                " uses old property key MetadataManager");
        }
        getGenerator();
        getMetadataService();
    }

    public MCRPIMetadataService<T> getMetadataService() {
        Map<String, String> properties = getProperties();

        final String metadataManager;
        if (properties.containsKey(METADATA_SERVICE_PROPERTY_KEY)) {
            metadataManager = properties.get(METADATA_SERVICE_PROPERTY_KEY);
        } else {
            throw new MCRConfigurationException(
                getServiceID() + " has no " + METADATA_SERVICE_PROPERTY_KEY + "!");
        }

        final String className = MCRConfiguration2.getStringOrThrow(METADATA_SERVICE_CONFIG_PREFIX
            + metadataManager);

        try {
            Class<? extends MCRPIMetadataService<T>> classObject = MCRClassTools.forName(className);
            Constructor<? extends MCRPIMetadataService<T>> constructor = classObject.getConstructor(String.class);
            return constructor.newInstance(metadataManager);
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + (METADATA_SERVICE_CONFIG_PREFIX + metadataManager) + ") not found: "
                    + className,
                e);
        } catch (NoSuchMethodException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + (METADATA_SERVICE_CONFIG_PREFIX + metadataManager)
                    + ") needs a string constructor: " + className);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new MCRException(e);
        }
    }

    protected MCRPIGenerator<T> getGenerator() {
        Supplier<? extends RuntimeException> generatorPropertiesNotSetError = () -> new MCRConfigurationException(
            "Configuration property " + REGISTRATION_CONFIG_PREFIX + registrationServiceID
                + "." + GENERATOR_PROPERTY_KEY + " is not set");

        String generatorName = Optional.ofNullable(getProperties().get(GENERATOR_PROPERTY_KEY))
            .orElseThrow(generatorPropertiesNotSetError);

        String generatorPropertyKey = GENERATOR_CONFIG_PREFIX + generatorName;
        String className = MCRConfiguration2.getStringOrThrow(generatorPropertyKey);

        try {
            Class<? extends MCRPIGenerator<T>> classObject = MCRClassTools.forName(className);
            Constructor<? extends MCRPIGenerator<T>> constructor = classObject.getConstructor(String.class);
            return constructor.newInstance(generatorName);
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + generatorPropertyKey + ") not found: " + className, e);
        } catch (NoSuchMethodException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + generatorPropertyKey + ") needs a string constructor: " + className);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new MCRException(e);
        }
    }

    public static void addFlagToObject(MCRBase obj, MCRPI databaseEntry) {
        String json = getGson().toJson(databaseEntry);
        obj.getService().addFlag(PI_FLAG, json);
    }

    public static boolean hasFlag(MCRObjectID id, String additional, MCRPIRegistrationInfo mcrpi) {
        MCRBase obj = MCRMetadataManager.retrieve(id);
        return hasFlag(obj, additional, mcrpi);
    }

    public static boolean hasFlag(MCRBase obj, String additional, MCRPIRegistrationInfo mcrpi) {
        MCRObjectService service = obj.getService();
        ArrayList<String> flags = service.getFlags(MCRPIService.PI_FLAG);
        Gson gson = getGson();
        return flags.stream().anyMatch(_stringFlag -> {
            MCRPI flag = gson.fromJson(_stringFlag, MCRPI.class);
            return flag.getAdditional().equals(additional) && flag.getIdentifier().equals(mcrpi.getIdentifier());
        });
    }

    protected void validatePermission(MCRBase obj, boolean writePermission) throws MCRAccessException {
        List<String> requiredPermissions = new ArrayList<>(writePermission ? 2 : 1);
        requiredPermissions.add("register-" + getServiceID());
        if (writePermission) {
            requiredPermissions.add(PERMISSION_WRITE);
        }
        Optional<String> missingPermission = requiredPermissions.stream()
            .filter(permission -> !MCRAccessManager.checkPermission(obj.getId(), permission))
            .findFirst();
        if (missingPermission.isPresent()) {
            throw MCRAccessException
                .missingPermission("Register a " + type + " & Update object.", obj.getId().toString(),
                    missingPermission.get());
        }
    }

    protected void validateAlreadyCreated(MCRObjectID id, String additional) throws MCRPersistentIdentifierException {
        if (isCreated(id, additional)) {
            throw new MCRPersistentIdentifierException("There is already a registered " + getType() + " for Object "
                + id + " and additional " + additional);
        }
    }

    /**
     * Validates if an object can get an Identifier assigned from this service! <b>Better call super when overwrite!</b>
     *
     * @param obj
     * @throws MCRPersistentIdentifierException
     * see {@link org.mycore.pi.exceptions}
     * @throws MCRAccessException
     * if the user does not have the rights to assign a pi to the specific object
     */
    public void validateRegistration(MCRBase obj, String additional)
        throws MCRPersistentIdentifierException, MCRAccessException {
        validateRegistration(obj, additional, true);
    }

    public void validateRegistration(MCRBase obj, String additional, boolean checkWritePermission)
        throws MCRPersistentIdentifierException, MCRAccessException {
        validateAlreadyCreated(obj.getId(), additional);
        validatePermission(obj, checkWritePermission);
    }

    /**
     * shorthand for {@link #register(MCRBase, String, boolean)} with update = true
     */
    public T register(MCRBase obj, String additional)
        throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException, ExecutionException,
        InterruptedException {
        return register(obj, additional, true);
    }

    /**
     * Adds a identifier to the object.
     * Validates everything, registers a new Identifier, inserts the identifier to object metadata and writes a
     * information to the Database.
     *
     * @param obj the object which has to be identified
     * @return the assigned Identifier
     * @throws MCRAccessException
     * the current User doesn't have the rights to insert the Identifier to Metadata
     * @throws MCRActiveLinkException
     * the {@link MCRPIMetadataService} lets
     * {@link org.mycore.datamodel.metadata.MCRMetadataManager#update(MCRObject)} throw this
     * @throws MCRPersistentIdentifierException see {@link org.mycore.pi.exceptions}
     */
    public T register(MCRBase obj)
        throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException, ExecutionException,
        InterruptedException {
        return this.register(obj, null);
    }

    /**
     * Validates everything, registers a new Identifier, inserts the identifier to object metadata and writes a
     * information to the Database.
     *
     * @param obj          the object which has to be identified
     * @param additional   additional information for the persistent identifier
     * @param updateObject if true this method calls {@link MCRMetadataManager#update(MCRBase)}
     * @return the assigned Identifier
     * @throws MCRAccessException
     * the current User doesn't have the rights to insert the Identifier to Metadata
     * @throws MCRActiveLinkException
     * the {@link MCRPIMetadataService} lets
     * {@link org.mycore.datamodel.metadata.MCRMetadataManager#update(MCRObject)} throw this
     * @throws MCRPersistentIdentifierException
     * see {@link org.mycore.pi.exceptions}
     */
    public synchronized T register(MCRBase obj, String additional, boolean updateObject)
        throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException, ExecutionException,
        InterruptedException {

        // There are many querys that require the current database state.
        // So we start a new transaction within the synchronized block
        final MCRFixedUserCallable<T> createPICallable = new MCRFixedUserCallable<>(() -> {
            this.validateRegistration(obj, additional, updateObject);
            final T identifier = getNewIdentifier(obj, additional);
            this.registerIdentifier(obj, additional, identifier);
            this.getMetadataService().insertIdentifier(identifier, obj, additional);

            MCRPI databaseEntry = insertIdentifierToDatabase(obj, additional, identifier);

            addFlagToObject(obj, databaseEntry);

            if (updateObject) {
                if (obj instanceof MCRObject) {
                    MCRMetadataManager.update((MCRObject) obj);
                } else if (obj instanceof MCRDerivate) {
                    MCRMetadataManager.update((MCRDerivate) obj);
                }
            }

            return identifier;
        }, MCRSessionMgr.getCurrentSession().getUserInformation());

        try {
            return REGISTER_POOL.submit(createPICallable).get();
        } catch (ExecutionException e) {
            if (e.getCause() != null && e.getCause() instanceof MCRPersistentIdentifierException) {
                throw (MCRPersistentIdentifierException) e.getCause();
            }
            throw e;
        }
    }

    protected Date provideRegisterDate(MCRBase obj, String additional) {
        return new Date();
    }

    public MCRPI insertIdentifierToDatabase(MCRBase obj, String additional, T identifier) {
        MCRPI databaseEntry = new MCRPI(identifier.asString(), getType(), obj.getId().toString(), additional,
            this.getServiceID(), provideRegisterDate(obj, additional), null);
        MCREntityManagerProvider.getCurrentEntityManager().persist(databaseEntry);
        return databaseEntry;
    }

    public final String getType() {
        return this.type;
    }

    protected abstract void registerIdentifier(MCRBase obj, String additional, T pi)
        throws MCRPersistentIdentifierException;

    protected final void onDelete(T identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        delete(identifier, obj, additional);
        MCRPIManager.getInstance().delete(obj.getId().toString(), additional, getType(),
            this.getServiceID());
    }

    protected final void onUpdate(T identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        update(identifier, obj, additional);
    }

    /**
     * Should handle deletion of a Object with the PI.
     * E.g. The {@link MCRDOIService} sets the active flag in Datacite datacentre to false.
     *
     * @param identifier the Identifier
     * @param obj        the deleted object
     * @param additional
     * @throws MCRPersistentIdentifierException
     * to abort deletion of the object or if something went wrong, (e.g. {@link MCRDOIService} throws if not a superuser
     * tries to delete the object)
     */
    protected abstract void delete(T identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException;

    /**
     * Should handle updates of a Object with the PI.
     * E.g. The {@link MCRDOIService} sends the updated metadata to the Datacite datacentre.
     *
     * @param identifier the Identifier
     * @param obj        the deleted object
     * @param additional
     * @throws MCRPersistentIdentifierException to abort update of the object or if something went wrong.
     */
    protected abstract void update(T identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException;

    public boolean isCreated(MCRObjectID id, String additional) {
        return MCRPIManager.getInstance().isCreated(id, additional, type, registrationServiceID);
    }

    public boolean isRegistered(MCRObjectID id, String additional) {
        return MCRPIManager.getInstance().isRegistered(id, additional, type, registrationServiceID);
    }

    public boolean hasRegistrationStarted(MCRObjectID id, String additional) {
        return MCRPIManager.getInstance()
            .hasRegistrationStarted(id, additional, type, registrationServiceID);
    }

    protected final Map<String, String> getProperties() {
        final String configPrefix = REGISTRATION_CONFIG_PREFIX + registrationServiceID + ".";
        return MCRConfiguration2.getSubPropertiesMap(configPrefix);
    }

    protected T getNewIdentifier(MCRBase id, String additional) throws MCRPersistentIdentifierException {
        MCRPIGenerator<T> persitentIdentifierGenerator = getGenerator();
        final T generated = persitentIdentifierGenerator.generate(id, additional);
        final String generatedIdentifier = generated.asString();
        final Optional<MCRPIRegistrationInfo> mayInfo = MCRPIManager.getInstance()
            .getInfo(generatedIdentifier, getType());
        if (mayInfo.isPresent()) {
            final String presentObject = mayInfo.get().getMycoreID();
            throw new MCRPersistentIdentifierException(
                "The Generated identifier " + generatedIdentifier + " is already present in database in object "
                    + presentObject,
                MCRTranslation.translate(TRANSLATE_PREFIX + ERR_CODE_0_1),
                ERR_CODE_0_1);
        }
        return generated;
    }

    protected MCRPI getTableEntry(MCRObjectID id, String additional) {
        return MCRPIManager.getInstance().get(getServiceID(), id.toString(), additional);
    }

    public void updateFlag(MCRObjectID id, String additional, MCRPI mcrpi) {
        MCRBase obj = MCRMetadataManager.retrieve(id);
        MCRObjectService service = obj.getService();
        ArrayList<String> flags = service.getFlags(MCRPIService.PI_FLAG);
        Gson gson = getGson();
        String stringFlag = flags.stream().filter(_stringFlag -> {
            MCRPI flag = gson.fromJson(_stringFlag, MCRPI.class);
            return Objects.equals(flag.getAdditional(), additional) && Objects
                .equals(flag.getIdentifier(), mcrpi.getIdentifier());
        }).findAny().orElseThrow(() -> new MCRException(new MCRPersistentIdentifierException(
            "Could find flag to update (" + id + "," + additional + "," + mcrpi.getIdentifier() + ")")));

        int flagIndex = service.getFlagIndex(stringFlag);
        service.removeFlag(flagIndex);

        addFlagToObject(obj, mcrpi);
        try {
            MCRMetadataManager.update(obj);
        } catch (Exception e) {
            throw new MCRException("Could not update flags of object " + id, e);
        }
    }

    /**
     * Validates a property of this service
     * @param propertyName the property to check
     * @return the property
     * @throws MCRConfigurationException if property is not set or empty
     */
    protected String requireNotEmptyProperty(String propertyName) throws MCRConfigurationException {
        final Map<String, String> properties = getProperties();
        if (!properties.containsKey(propertyName) && properties.get(propertyName).length() > 0) {
            throw new MCRConfigurationException(String
                .format(Locale.ROOT, "The property %s%s.%s is empty or not set!", REGISTRATION_CONFIG_PREFIX,
                    registrationServiceID,
                    propertyName));
        }
        return properties.get(propertyName);
    }
}
