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

package org.mycore.pi.urn.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityTransaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIService;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.MCRDNBURNParser;

/**
 * Service for assigning granular URNs to Derivate. You can call it with a Derivate-ID and it will assign a Base-URN for
 * the Derivate and granular URNs for every file in the Derivate (except IgnoreFileNames). If you then add a file to
 * Derivate you can call with Derivate-ID and additional path of the file. E.g. mir_derivate_00000060 and /image1.jpg
 * <p>
 * <b>Inscriber is ignored with this {@link MCRPIService}</b>
 * </p>
 * Configuration Parameter(s): <dl>
 * <dt>IgnoreFileNames</dt>
 * <dd>Comma seperated list of regex file which should not have a urn assigned. Default: mets\\.xml</dd> </dl>
 */
public class MCRURNGranularRESTService extends MCRPIService<MCRDNBURN> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Function<MCRDerivate, Stream<MCRPath>> derivateFileStream;

    public MCRURNGranularRESTService(String registrationServiceID) {
        this(registrationServiceID,
            MCRURNGranularRESTService::defaultDerivateFileStream);
    }

    public MCRURNGranularRESTService(String registrationServiceID,
        Function<MCRDerivate, Stream<MCRPath>> derivateFileStreamFunc) {
        super(registrationServiceID, MCRDNBURN.TYPE);
        this.derivateFileStream = derivateFileStreamFunc;
    }

    private static Stream<MCRPath> defaultDerivateFileStream(MCRDerivate derivate) {
        MCRObjectID derivateId = derivate.getId();
        Path derivRoot = MCRPath.getPath(derivateId.toString(), "/");

        try {
            return Files.walk(derivRoot)
                .map(MCRPath::toMCRPath)
                .filter(p -> !Files.isDirectory(p))
                .filter(p -> !p.equals(derivRoot));
        } catch (IOException e) {
            LOGGER.error("I/O error while access the starting file of derivate {}!", derivateId, e);
        } catch (SecurityException s) {
            LOGGER.error("No access to starting file of derivate {}!", derivateId, s);
        }

        return Stream.empty();
    }

    @Override
    public MCRDNBURN register(MCRBase obj, String filePath, boolean updateObject)
        throws MCRAccessException, MCRActiveLinkException, MCRPersistentIdentifierException {
        this.validateRegistration(obj, filePath);

        if (obj instanceof MCRDerivate) {
            MCRDerivate derivate = (MCRDerivate) obj;
            return registerURN(derivate, filePath);
        } else {
            throw new MCRPersistentIdentifierException("Object " + obj.getId() + " is not a MCRDerivate!");
        }
    }

    private MCRDNBURN registerURN(MCRDerivate deriv, String filePath) throws MCRPersistentIdentifierException {
        MCRObjectID derivID = deriv.getId();

        Function<String, Integer> countCreatedPI = s -> MCRPIManager
            .getInstance()
            .getCreatedIdentifiers(derivID, getType(), getServiceID())
            .size();

        int seed = Optional.of(filePath)
            .filter(p -> !"".equals(p))
            .map(countCreatedPI)
            .map(count -> count + 1)
            .orElse(1);

        MCRDNBURN derivURN = Optional
            .ofNullable(deriv.getDerivate())
            .map(MCRObjectDerivate::getURN)
            .flatMap(new MCRDNBURNParser()::parse)
            .orElseGet(() -> createNewURN(deriv));

        String setID = derivID.getNumberAsString();
        GranularURNGenerator granularURNGen = new GranularURNGenerator(seed, derivURN, setID);
        Function<MCRPath, Supplier<String>> generateURN = p -> granularURNGen.getURNSupplier();

        LinkedHashMap<Supplier<String>, MCRPath> urnPathMap = derivateFileStream.apply(deriv)
            .filter(notInIgnoreList().and(matchFile(filePath)))
            .sorted()
            .collect(Collectors.toMap(generateURN, p -> p, (m1, m2) -> m1,
                LinkedHashMap::new));

        if (!"".equals(filePath) && urnPathMap.isEmpty()) {
            String errMsg = MessageFormat.format("File {0} does not exist in {1}.\n", filePath, derivID.toString())
                + "Use absolute path of file without owner ID like /abs/path/to/file.\n";

            throw new MCRPersistentIdentifierException(errMsg);
        }

        urnPathMap.forEach(createFileMetadata(deriv).andThen(persistURN(deriv)));

        try {
            MCRMetadataManager.update(deriv);
        } catch (MCRPersistenceException | MCRAccessException e) {
            LOGGER.error("Error while updating derivate {}", derivID, e);
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
            MCRDNBURN derivURN = getNewIdentifier(deriv, "");
            deriv.getDerivate().setURN(derivURN.asString());

            persistURNStr(deriv, new Date()).accept(derivURN::asString, "");

            if (Boolean.valueOf(getProperties().getOrDefault("supportDfgViewerURN", "false"))) {
                String suffix = "dfg";
                persistURNStr(deriv, null, getServiceID() + "-" + suffix)
                    .accept(() -> derivURN.withNamespaceSuffix(suffix + "-").asString(), "");
            }

            return derivURN;
        } catch (MCRPersistentIdentifierException e) {
            throw new MCRPICreationException("Could not create new URN for " + derivID, e);
        }
    }

    private BiConsumer<Supplier<String>, MCRPath> createFileMetadata(MCRDerivate deriv) {
        return (urnSup, path) -> deriv.getDerivate().getOrCreateFileMetadata(path, urnSup.get());
    }

    private BiConsumer<Supplier<String>, MCRPath> persistURN(MCRDerivate deriv) {
        return (urnSup, path) -> persistURNStr(deriv, null).accept(urnSup, path.getOwnerRelativePath());
    }

    private BiConsumer<Supplier<String>, String> persistURNStr(MCRDerivate deriv, Date registerDate) {
        return (urnSup, path) -> persistURNStr(deriv, registerDate, getServiceID()).accept(urnSup, path);
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
        Supplier<? extends RuntimeException> errorInIgnorList = () -> new RuntimeException(
            "Error in ignore filename list!");

        return path -> getIgnoreFileList()
            .stream()
            .map(Pattern::compile)
            .map(Pattern::asPredicate)
            .map(Predicate::negate)
            .reduce(Predicate::and)
            .orElseThrow(errorInIgnorList)
            .test(path.getOwnerRelativePath());
    }

    private List<String> getIgnoreFileList() {
        List<String> ignoreFileNamesList = new ArrayList<>();
        String ignoreFileNames = getProperties().get("IgnoreFileNames");
        if (ignoreFileNames != null) {
            ignoreFileNamesList.addAll(Arrays.asList(ignoreFileNames.split(",")));
        } else {
            ignoreFileNamesList.add("mets\\.xml"); // default value
        }
        return ignoreFileNamesList;
    }

    @Override
    protected void registerIdentifier(MCRBase obj, String additional, MCRDNBURN urn)
        throws MCRPersistentIdentifierException {
        // not used in this impl
    }

    @Override
    protected void delete(MCRDNBURN identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        throw new MCRPersistentIdentifierException("Delete is not supported for " + getType());
    }

    @Override
    protected void update(MCRDNBURN identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        //TODO: improve API, don't override method to do nothing
        LOGGER.info("No update in this implementation");
    }

    private static class GranularURNGenerator {
        private final MCRDNBURN urn;

        private final String setID;

        private int counter;

        private String leadingZeros;

        public GranularURNGenerator(int seed, MCRDNBURN derivURN, String setID) {
            this.counter = seed;
            this.urn = derivURN;
            this.setID = setID;
        }

        public Supplier<String> getURNSupplier() {
            int currentCount = counter++;
            return () -> urn.toGranular(setID, getIndex(currentCount)).asString();
        }

        public String getIndex(int currentCount) {
            return String.format(Locale.getDefault(), leadingZeros(counter), currentCount);
        }

        private String leadingZeros(int i) {
            if (leadingZeros == null) {
                leadingZeros = "%0" + numDigits(i) + "d";
            }

            return leadingZeros;
        }

        private long numDigits(long n) {
            if (n < 10) {
                return 1;
            }
            return 1 + numDigits(n / 10);
        }
    }

    public class MCRPICreationException extends RuntimeException {
        public MCRPICreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
