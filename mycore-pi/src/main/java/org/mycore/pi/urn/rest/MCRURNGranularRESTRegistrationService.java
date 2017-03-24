package org.mycore.pi.urn.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.*;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.pi.MCRPIRegistrationService;
import org.mycore.pi.MCRPersistentIdentifierManager;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.MCRDNBURNParser;

import javax.persistence.EntityTransaction;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for assigning granular URNs to Derivate.
 * You can call it with a Derivate-ID and it will assign a Base-URN for the Derivate and granular URNs for every file in the Derivate (except IgnoreFileNames).
 * If you then add a file to Derivate you can call with Derivate-ID and additional path of the file. E.g. mir_derivate_00000060 and /image1.jpg
 * <p>
 * <b>Inscriber is ignored with this {@link MCRPIRegistrationService}</b>
 * </p>
 * Configuration Parameter(s):
 * <dl>
 * <dt>IgnoreFileNames</dt>
 * <dd>Comma seperated list of regex file which should not have a urn assigned. Default: mets\\.xml</dd>
 * </dl>
 */
public class MCRURNGranularRESTRegistrationService extends MCRPIRegistrationService<MCRDNBURN> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Function<MCRDerivate, Stream<MCRPath>> derivateFileStream;

    public MCRURNGranularRESTRegistrationService(String registrationServiceID) {
        this(registrationServiceID,
             MCRURNGranularRESTRegistrationService::defaultDerivateFileStream);
    }

    public MCRURNGranularRESTRegistrationService(String registrationServiceID,
                                                 Function<MCRDerivate, Stream<MCRPath>> derivateFileStream) {
        super(registrationServiceID, MCRDNBURN.TYPE);
        this.derivateFileStream = derivateFileStream;
    }

    private static Stream<MCRPath> defaultDerivateFileStream(MCRDerivate derivate) {
        MCRObjectID derivateId = derivate.getId();
        Path path = MCRPath.getPath(derivateId.toString(), "/");

        try {
            return Files.walk(path)
                        .map(MCRPath::toMCRPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not walk file tree of derivate " + derivateId.toString() + "!", e);
        }
    }

    @Override
    public MCRDNBURN fullRegister(MCRBase obj, String filePath)
            throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException {
        this.validateRegistration(obj, filePath);

        Supplier<? extends RuntimeException> objIsNotMCRDerivate = () ->
                new RuntimeException("Object " + obj.getId().toString() + " is not a MCRDerivate!");

        return Optional.of(obj)
                       .filter(MCRDerivate.class::isInstance)
                       .map(MCRDerivate.class::cast)
                       .map(deriv -> registerURN(deriv, filePath))
                       .orElseThrow(objIsNotMCRDerivate);
    }

    private MCRDNBURN registerURN(MCRDerivate deriv, String filePath) {
        MCRObjectID derivID = deriv.getId();

        MCRDNBURN derivURN = Optional
                .ofNullable(deriv.getDerivate())
                .map(MCRObjectDerivate::getURN)
                .flatMap(new MCRDNBURNParser()::parse)
                .orElseGet(() -> createNewURN(deriv));

        Function<String, Integer> countCreatedPI = s -> MCRPersistentIdentifierManager
                .getInstance()
                .getCreatedIdentifiers(derivID, getType(), getRegistrationServiceID())
                .size();

        int seed = Optional.of(filePath)
                           .filter(p -> !"".equals(p))
                           .map(countCreatedPI)
                           .map(count -> count + 1)
                           .orElse(1);

        String setID = derivID.getNumberAsString();
        GranularURNGenerator granularURNGen = new GranularURNGenerator(seed, derivURN, setID);
        Function<MCRPath, Supplier<String>> generateURN = p -> granularURNGen.getURNSupplier();

        derivateFileStream.apply(deriv)
                          .filter(notInIgnoreList().and(matchFile(filePath)))
                          .sorted()
                          .collect(Collectors.toMap(generateURN, p -> p, (m1, m2) -> m1,
                                                    LinkedHashMap::new))
                          .forEach(createFileMetadata(deriv).andThen(persistURN(deriv)));

        try {
            MCRMetadataManager.update(deriv);
        } catch (IOException | MCRAccessException e) {
            LOGGER.error("Error while updating derivate!", e);
        }

        EntityTransaction transaction = MCREntityManagerProvider
                .getCurrentEntityManager()
                .getTransaction();

        if (!transaction.isActive()) {
            transaction.begin();
        }

        transaction.commit();

        return derivURN;
    }

    public MCRDNBURN createNewURN(MCRDerivate deriv) {
        MCRObjectID derivID = deriv.getId();

        try {
            MCRDNBURN derivURN = getNewIdentifier(derivID, "");
            deriv.getDerivate().setURN(derivURN.asString());

            persistURNStr(deriv, new Date()).accept(() -> derivURN.asString(), "");

            if (Boolean.valueOf(getProperties().getOrDefault("supportDfgViewerURN", "false"))) {
                String suffix = "-dfg";
                persistURNStr(deriv, null, getRegistrationServiceID() + suffix)
                        .accept(() -> derivURN.withSuffix(suffix).asString(), "");
            }

            return derivURN;
        } catch (MCRPersistentIdentifierException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not create new URN for " + derivID.toString());
        }
    }

    private BiConsumer<Supplier<String>, MCRPath> createFileMetadata(MCRDerivate deriv) {
        return (urnSup, path) -> deriv.getDerivate().getOrCreateFileMetadata(path, urnSup.get());
    }

    private BiConsumer<Supplier<String>, MCRPath> persistURN(MCRDerivate deriv) {
        return (urnSup, path) -> persistURNStr(deriv, null).accept(urnSup, path.getOwnerRelativePath().toString());
    }

    private BiConsumer<Supplier<String>, String> persistURNStr(MCRDerivate deriv, Date registerDate) {
        return (urnSup, path) -> persistURNStr(deriv, registerDate, getRegistrationServiceID()).accept(urnSup, path);
    }

    private BiConsumer<Supplier<String>, String> persistURNStr(MCRDerivate deriv, Date registerDate, String serviceID) {
        return (urnSup, path) -> {
            MCRPI mcrpi = new MCRPI(urnSup.get(), getType(), deriv.getId().toString(), path, serviceID,
                                    registerDate);
            MCREntityManagerProvider.getCurrentEntityManager().persist(mcrpi);
        };
    }

    private Predicate<MCRPath> matchFile(String ownerRelativPath) {
        return path -> Optional.of(ownerRelativPath)
                               .filter(""::equals)
                               .map(p -> Boolean.TRUE)
                               .orElseGet(() -> path.getOwnerRelativePath().equals(ownerRelativPath));

    }

    private Predicate<MCRPath> notInIgnoreList() {
        Supplier<? extends RuntimeException> errorInIgnorList = () ->
                new RuntimeException("Error in ignore filename list!");

        return path -> getIgnoreFileList()
                .stream()
                .map(Pattern::compile)
                .map(Pattern::asPredicate)
                .map(Predicate::negate)
                .reduce(Predicate::and)
                .orElseThrow(errorInIgnorList)
                .test(path.getOwnerRelativePath());
    }

    private static class GranularURNGenerator {
        private final MCRDNBURN derivURN;

        private final String setID;

        int counter;

        String leadingZeros;

        public GranularURNGenerator(int seed, MCRDNBURN derivURN, String setID) {
            this.counter = seed;
            this.derivURN = derivURN;
            this.setID = setID;
        }

        public BiFunction<MCRDNBURN, String, String> getURNFunction() {
            int currentCount = counter++;
            return (urn, setID) -> urn.toGranular(setID, getIndex(currentCount)).asString();
        }

        public Supplier<String> getURNSupplier() {
            int currentCount = counter++;
            return () -> derivURN.toGranular(setID, getIndex(currentCount)).asString();
        }

        public String getIndex(int currentCount) {
            return String.format(Locale.getDefault(), leadingZeros(counter), currentCount);}

        private String leadingZeros(int i) {
            if (leadingZeros == null) {
                leadingZeros = "%0" + numDigits(i) + "d";
            }

            return leadingZeros;
        }

        private long numDigits(long n) {
            if (n < 10)
                return 1;
            return 1 + numDigits(n / 10);
        }
    }

    @Override
    protected void validateAlreadyInscribed(MCRBase obj, String additional, String type, MCRObjectID id)
            throws MCRPersistentIdentifierException {
    }

    private List<String> getIgnoreFileList() {
        List<String> ignoreFileNamesList = new ArrayList<>();
        String ignoreFileNames = getProperties().get("IgnoreFileNames");
        if (ignoreFileNames != null) {
            Stream.of(ignoreFileNames.split(",")).forEach(ignoreFileNamesList::add);
        } else {
            ignoreFileNamesList.add("mets\\.xml"); // default value
        }
        return ignoreFileNamesList;
    }

    @Override
    protected MCRDNBURN registerIdentifier(MCRBase obj, String additional) throws MCRPersistentIdentifierException {
        // not used in this impl
        return null;
    }

    @Override
    protected void delete(MCRDNBURN identifier, MCRBase obj, String additional)
            throws MCRPersistentIdentifierException {
        throw new MCRPersistentIdentifierException("Delete is not supported for " + getType());
    }

    @Override
    protected void update(MCRDNBURN identifier, MCRBase obj, String additional)
            throws MCRPersistentIdentifierException {
    }
}
