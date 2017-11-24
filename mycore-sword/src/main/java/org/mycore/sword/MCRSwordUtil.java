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

package org.mycore.sword;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.Deflater;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.sword.application.MCRSwordCollectionProvider;
import org.mycore.sword.application.MCRSwordObjectIDSupplier;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.MediaResource;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

public class MCRSwordUtil {

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    private static final int COPY_BUFFER_SIZE = 32 * 1024;

    private static Logger LOGGER = LogManager.getLogger(MCRSwordUtil.class);

    public static MCRDerivate createDerivate(String documentID)
        throws MCRPersistenceException, IOException, MCRAccessException {
        final String projectId = MCRObjectID.getInstance(documentID).getProjectId();
        MCRObjectID oid = MCRObjectID.getNextFreeId(projectId, "derivate");
        final String derivateID = oid.toString();

        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(oid);
        derivate.setLabel("data object from " + documentID);

        String schema = CONFIG.getString("MCR.Metadata.Config.derivate", "datamodel-derivate.xml").replaceAll(".xml",
            ".xsd");
        derivate.setSchema(schema);

        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag("linkmeta");
        linkId.setReference(documentID, null, null);
        derivate.getDerivate().setLinkMeta(linkId);

        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(null);

        derivate.getDerivate().setInternals(ifs);

        LOGGER.debug("Creating new derivate with ID {}", derivateID);
        MCRMetadataManager.create(derivate);

        if (CONFIG.getBoolean("MCR.Access.AddDerivateDefaultRule", true)) {
            MCRAccessInterface AI = MCRAccessManager.getAccessImpl();
            Collection<String> configuredPermissions = AI.getAccessPermissionsFromConfiguration();
            for (String permission : configuredPermissions) {
                MCRAccessManager.addRule(derivateID, permission, MCRAccessManager.getTrueRule(),
                    "default derivate rule");
            }
        }

        final MCRPath rootDir = MCRPath.getPath(derivateID, "/");
        if (Files.notExists(rootDir)) {
            rootDir.getFileSystem().createRoot(derivateID);
        }

        return derivate;
    }

    public static MediaResource getZippedDerivateMediaResource(String object) {
        final Path tempFile;

        try {
            tempFile = Files.createTempFile("swordv2_", ".temp.zip");
        } catch (IOException e) {
            throw new MCRException("Could not create temp file!", e);
        }

        try (final OutputStream tempFileStream = Files.newOutputStream(tempFile)) {
            final ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(tempFileStream);
            zipOutputStream.setLevel(Deflater.BEST_COMPRESSION);

            final MCRPath root = MCRPath.getPath(object, "/");
            addDirectoryToZip(zipOutputStream, root);
            zipOutputStream.close();
        } catch (IOException e) {
            throw new MCRException(e);
        }

        MediaResource resultRessource;
        InputStream is;
        try {
            is = new MCRDeleteFileOnCloseFilterInputStream(Files.newInputStream(tempFile), tempFile);
            resultRessource = new MediaResource(is, MCRSwordConstants.MIME_TYPE_APPLICATION_ZIP,
                UriRegistry.PACKAGE_SIMPLE_ZIP);
        } catch (IOException e) {
            throw new MCRException("could not read from temp file!", e);
        }
        return resultRessource;
    }

    private static void addDirectoryToZip(ZipArchiveOutputStream zipOutputStream, Path directory) {
        MCRSession currentSession = MCRSessionMgr.getCurrentSession();
        final DirectoryStream<Path> paths;
        try {
            paths = Files.newDirectoryStream(directory);
        } catch (IOException e) {
            throw new MCRException(e);
        }

        paths.forEach(p -> {
            final boolean isDir = Files.isDirectory(p);
            final ZipArchiveEntry zipArchiveEntry;
            try {
                final String fileName = getFilename(p);
                LOGGER.info("Addding {} to zip file!", fileName);
                if (isDir) {
                    addDirectoryToZip(zipOutputStream, p);
                } else {
                    zipArchiveEntry = new ZipArchiveEntry(fileName);
                    zipArchiveEntry.setSize(Files.size(p));
                    zipOutputStream.putArchiveEntry(zipArchiveEntry);
                    if (currentSession.isTransactionActive()) {
                        currentSession.commitTransaction();
                    }
                    Files.copy(p, zipOutputStream);
                    currentSession.beginTransaction();
                    zipOutputStream.closeArchiveEntry();
                }
            } catch (IOException e) {
                LOGGER.error("Could not add path {}", p);
            }
        });
    }

    public static String getFilename(Path path) {
        return '/' + path.getRoot().relativize(path).toString();
    }

    /**
     * Stores stream to temp file and checks md5
     *
     * @param inputStream the stream which holds the File
     * @param checkMd5    the md5 to compare with (or null if no md5 check is needed)
     * @return the path to the temp file
     * @throws IOException if md5 does mismatch or if stream could not be read
     */
    public static Path createTempFileFromStream(String fileName, InputStream inputStream, String checkMd5)
        throws IOException {
        MCRSession currentSession = MCRSessionMgr.getCurrentSession();
        if (currentSession.isTransactionActive()) {
            currentSession.commitTransaction();
        }

        final Path zipTempFile = Files.createTempFile("swordv2_", fileName);
        MessageDigest md5Digest = null;

        if (checkMd5 != null) {
            try {
                md5Digest = MessageDigest.getInstance("MD5");
                inputStream = new DigestInputStream(inputStream, md5Digest);
            } catch (NoSuchAlgorithmException e) {
                currentSession.beginTransaction();
                throw new MCRConfigurationException("No MD5 available!", e);
            }
        }

        Files.copy(inputStream, zipTempFile, StandardCopyOption.REPLACE_EXISTING);

        if (checkMd5 != null) {
            final String md5String = MCRUtils.toHexString(md5Digest.digest());
            if (!md5String.equals(checkMd5)) {
                currentSession.beginTransaction();
                throw new IOException("MD5 mismatch, expected " + checkMd5 + " got " + md5String);
            }
        }

        currentSession.beginTransaction();
        return zipTempFile;
    }

    public static void extractZipToPath(Path zipFilePath, MCRPath target)
        throws SwordError, IOException, NoSuchAlgorithmException, URISyntaxException {
        LOGGER.info("Extracting zip: {}", zipFilePath);
        try (FileSystem zipfs = FileSystems.newFileSystem(new URI("jar:" + zipFilePath.toUri()), new HashMap<>())) {
            final Path sourcePath = zipfs.getPath("/");
            Files.walkFileTree(sourcePath,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                        final Path relativeSP = sourcePath.relativize(dir);
                        //WORKAROUND for bug
                        Path targetdir = relativeSP.getNameCount() == 0 ? target : target.resolve(relativeSP);
                        try {
                            Files.copy(dir, targetdir);
                        } catch (FileAlreadyExistsException e) {
                            if (!Files.isDirectory(targetdir))
                                throw e;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                        MCRSession currentSession = MCRSessionMgr.getCurrentSession();

                        LOGGER.info("Extracting: {}", file);
                        Path targetFilePath = target.resolve(sourcePath.relativize(file));
                        // WORKAROUND: copy is bad with IFS because fsnodes is locked until copy is completed
                        // so we end the transaction after we got a byte channel, then we write the data
                        // and before completion we start the transaction to let niofs write the md5 to the table
                        try (SeekableByteChannel destinationChannel = Files.newByteChannel(targetFilePath,
                            StandardOpenOption.WRITE, StandardOpenOption.SYNC, StandardOpenOption.CREATE);
                            SeekableByteChannel sourceChannel = Files.newByteChannel(file, StandardOpenOption.READ)) {
                            if (currentSession.isTransactionActive()) {
                                currentSession.commitTransaction();
                            }
                            ByteBuffer buffer = ByteBuffer.allocateDirect(COPY_BUFFER_SIZE);
                            while (sourceChannel.read(buffer) != -1 || buffer.position() > 0) {
                                buffer.flip();
                                destinationChannel.write(buffer);
                                buffer.compact();
                            }
                        } finally {
                            if (!currentSession.isTransactionActive()) {
                                currentSession.beginTransaction();
                            }
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });
        }

    }

    public static List<MCRValidationResult> validateZipFile(final MCRFileValidator validator, Path zipFile)
        throws IOException, URISyntaxException {
        try (FileSystem zipfs = FileSystems.newFileSystem(new URI("jar:" + zipFile.toUri()), new HashMap<>())) {
            final Path sourcePath = zipfs.getPath("/");
            ArrayList<MCRValidationResult> validationResults = new ArrayList<>();
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    MCRValidationResult validationResult = validator.validate(file);
                    if (!validationResult.isValid()) {
                        validationResults.add(validationResult);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
            return validationResults;
        }
    }

    public static String encodeURLPart(String uri) {
        try {
            return URLEncoder.encode(uri, BuildLinkUtil.DEFAULT_URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new MCRException(BuildLinkUtil.DEFAULT_URL_ENCODING + " is not supported!", e);
        }
    }

    public static String decodeURLPart(String uri) {
        try {
            if (uri == null) {
                return null;
            }
            return URLDecoder.decode(uri, BuildLinkUtil.DEFAULT_URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new MCRException(BuildLinkUtil.DEFAULT_URL_ENCODING + " is not supported!", e);
        }
    }

    public static DepositReceipt buildDepositReceipt(IRI iri) throws SwordError {
        DepositReceipt depositReceipt = new DepositReceipt();
        depositReceipt.setEditIRI(iri);
        return depositReceipt;
    }

    public static void addDatesToEntry(Entry entry, MCRObject mcrObject) {
        MCRObjectService serviceElement = mcrObject.getService();
        ArrayList<String> flags = serviceElement.getFlags(MCRObjectService.FLAG_TYPE_CREATEDBY);
        flags.addAll(serviceElement.getFlags(MCRObjectService.FLAG_TYPE_MODIFIEDBY));
        Set<String> clearedFlags = new LinkedHashSet<>(flags);
        clearedFlags.forEach(entry::addAuthor);

        Date modifyDate = serviceElement.getDate(MCRObjectService.DATE_TYPE_MODIFYDATE);
        Date createDate = serviceElement.getDate(MCRObjectService.DATE_TYPE_CREATEDATE);
        entry.setEdited(modifyDate);
        entry.setPublished(createDate);
    }

    public static MCRObject getMcrObjectForDerivateID(String requestDerivateID) {
        final MCRObjectID objectID = MCRObjectID.getInstance(MCRXMLFunctions.getMCRObjectID(requestDerivateID));
        return (MCRObject) MCRMetadataManager.retrieve(objectID);
    }

    public static class ParseLinkUtil {

        public static final Pattern COLLECTION_IRI_PATTERN = Pattern.compile("([a-zA-Z0-9]+)/([0-9]*)");

        public static final Pattern COLLECTION_MCRID_IRI_PATTERN = Pattern
            .compile("([a-zA-Z0-9]+)/([a-zA-Z0-9]+_[a-zA-Z0-9]+_[0-9]+)");

        public static final Pattern COLLECTION_DERIVATEID_IRI_PATTERN = Pattern
            .compile("([a-zA-Z0-9]+)/([a-zA-Z0-9]+_[a-zA-Z0-9]+_[0-9]+)(/.+)?");

        private static String getXFromXIRI(IRI editIRI, Integer x, String iri, Pattern iriPattern) {
            return getXFromXIRI(editIRI, x, iri, iriPattern, true);
        }

        private static String getXFromXIRI(IRI editIRI, Integer x, String iri, Pattern iriPattern, boolean required) {
            String[] urlParts = editIRI.toString().split(iri);

            if (urlParts.length < 2) {
                final String message = "Invalid " + iri + " : " + editIRI;
                throw new IllegalArgumentException(message);
            }

            String uriPathAsString = urlParts[1];
            Matcher matcher = iriPattern.matcher(uriPathAsString);
            if (matcher.matches()) {
                if (matcher.groupCount() >= x) {
                    return matcher.group(x);
                } else {
                    return null;
                }
            } else {
                if (required) {
                    throw new IllegalArgumentException(
                        MessageFormat.format("{0} does not match the pattern {1}", uriPathAsString, iriPattern));
                } else {
                    return null;
                }
            }
        }

        public static class CollectionIRI {

            public static String getCollectionNameFromCollectionIRI(IRI collectionIRI) {
                String uriPathAsString = collectionIRI.getPath().split(MCRSwordConstants.SWORD2_COL_IRI)[1];
                Matcher matcher = COLLECTION_IRI_PATTERN.matcher(uriPathAsString);
                if (matcher.matches()) {
                    return matcher.group(1);
                } else {
                    throw new IllegalArgumentException(MessageFormat.format("{0} does not match the pattern {1}",
                        uriPathAsString, COLLECTION_IRI_PATTERN));
                }
            }

            public static Integer getPaginationFromCollectionIRI(IRI collectionIRI) {
                String uriPathAsString = collectionIRI.getPath().split(MCRSwordConstants.SWORD2_COL_IRI)[1];
                Matcher matcher = COLLECTION_IRI_PATTERN.matcher(uriPathAsString);
                if (matcher.matches() && matcher.groupCount() > 1) {
                    String numberGroup = matcher.group(2);
                    if (numberGroup.length() > 0) {
                        return Integer.parseInt(numberGroup);
                    }
                }
                return 1;
            }
        }

        public static class EditIRI {

            public static String getCollectionFromEditIRI(IRI editIRI) {
                return getXFromXIRI(editIRI, 1, MCRSwordConstants.SWORD2_EDIT_IRI, COLLECTION_MCRID_IRI_PATTERN);
            }

            public static String getObjectFromEditIRI(IRI editIRI) {
                return getXFromXIRI(editIRI, 2, MCRSwordConstants.SWORD2_EDIT_IRI, COLLECTION_MCRID_IRI_PATTERN);
            }
        }

        public static class MediaEditIRI {

            public static String getCollectionFromMediaEditIRI(IRI mediaEditIRI) {
                return getXFromXIRI(mediaEditIRI, 1, MCRSwordConstants.SWORD2_EDIT_MEDIA_IRI,
                    COLLECTION_DERIVATEID_IRI_PATTERN);
            }

            public static String getDerivateFromMediaEditIRI(IRI mediaEditIRI) {
                return getXFromXIRI(mediaEditIRI, 2, MCRSwordConstants.SWORD2_EDIT_MEDIA_IRI,
                    COLLECTION_DERIVATEID_IRI_PATTERN);
            }

            public static String getFilePathFromMediaEditIRI(IRI mediaEditIRI) {
                return decodeURLPart(getXFromXIRI(mediaEditIRI, 3, MCRSwordConstants.SWORD2_EDIT_MEDIA_IRI,
                    COLLECTION_DERIVATEID_IRI_PATTERN, false));
            }
        }
    }

    public static class BuildLinkUtil {

        public static final String DEFAULT_URL_ENCODING = "UTF-8";

        private static Logger LOGGER = LogManager.getLogger(BuildLinkUtil.class);

        public static String getEditHref(String collection, String id) {
            return MessageFormat.format("{0}{1}{2}/{3}", MCRFrontendUtil.getBaseURL(),
                MCRSwordConstants.SWORD2_EDIT_IRI, collection, id);
        }

        public static String getEditMediaHrefOfDerivate(String collection, String id) {
            return MessageFormat.format("{0}{1}{2}/{3}", MCRFrontendUtil.getBaseURL(),
                MCRSwordConstants.SWORD2_EDIT_MEDIA_IRI, collection, id);
        }

        /**
         * Creates a edit link for every derivate of a mcrobject.
         *
         * @param mcrObjId the mcrobject id as String
         * @return returns a Stream which contains links to every derivate.
         */
        public static Stream<Link> getEditMediaIRIStream(final String collection, final String mcrObjId)
            throws SwordError {
            return MCRSword.getCollection(collection).getDerivateIDsofObject(mcrObjId).map(derivateId -> {
                final Factory abderaFactory = Abdera.getNewFactory();
                final Stream<IRI> editMediaFileIRIStream = getEditMediaFileIRIStream(collection, derivateId);
                return Stream
                    .concat(Stream.of(getEditMediaHrefOfDerivate(collection, derivateId)), editMediaFileIRIStream)
                    .map(link -> {
                        final Link newLinkElement = abderaFactory.newLink();
                        newLinkElement.setHref(link.toString());
                        newLinkElement.setRel("edit-media");
                        return newLinkElement;
                    });
            }).flatMap(s -> s);
        }

        /**
         * Creates a Stream which contains edit-media-IRIs to all files in a specific derivate derivate.
         *
         * @param collection the collection in which the derivate is.
         * @param derivateId the id of the derivate
         * @return a Stream which contains edit-media-IRIs to all files.
         */
        private static Stream<IRI> getEditMediaFileIRIStream(final String collection, final String derivateId) {
            MCRPath derivateRootPath = MCRPath.getPath(derivateId, "/");
            try {
                List<IRI> iris = new ArrayList<>();
                Files.walkFileTree(derivateRootPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                        String relativePath = derivateRootPath.relativize(file).toString();
                        final String URI = MessageFormat.format("{0}{1}{2}/{3}/{4}", MCRFrontendUtil.getBaseURL(),
                            MCRSwordConstants.SWORD2_EDIT_MEDIA_IRI, collection, derivateId,
                            encodeURLPart(relativePath));
                        iris.add(new IRI(URI));
                        return FileVisitResult.CONTINUE;
                    }
                });
                return iris.stream();
            } catch (IOException e) {
                LOGGER.error("Error while processing directory stream of {}", derivateId, e);
                throw new MCRException(e);
            }
        }

        public static String buildCollectionPaginationLinkHref(String collection, Integer page) {
            return MCRFrontendUtil.getBaseURL() + MCRSwordConstants.SWORD2_COL_IRI + collection + "/" + page;
        }

        /**
         * Creates Pagination links
         *
         * @param collectionIRI      IRI of the collection
         * @param collection         name of the collection
         * @param feed               the feed where the link will be inserted
         * @param collectionProvider {@link MCRSwordCollectionProvider} of the collection (needed to count how much objects)
         * @throws SwordServerException when the {@link MCRSwordObjectIDSupplier} throws a exception.
         */
        public static void addPaginationLinks(IRI collectionIRI, String collection, Feed feed,
            MCRSwordCollectionProvider collectionProvider) throws SwordServerException {
            final int lastPage = (int) Math.ceil((double) collectionProvider.getIDSupplier().getCount()
                / (double) MCRSwordConstants.MAX_ENTRYS_PER_PAGE);
            Integer currentPage = ParseLinkUtil.CollectionIRI.getPaginationFromCollectionIRI(collectionIRI);

            feed.addLink(buildCollectionPaginationLinkHref(collection, 1), "first");
            if (lastPage != currentPage) {
                feed.addLink(buildCollectionPaginationLinkHref(collection, currentPage + 1), "next");
            }
            feed.addLink(buildCollectionPaginationLinkHref(collection, lastPage), "last");
        }

        static void addEditMediaLinks(String collection, DepositReceipt depositReceipt, MCRObjectID derivateId) {
            getEditMediaFileIRIStream(collection, derivateId.toString()).forEach(depositReceipt::addEditMediaIRI);
        }
    }

    public interface MCRFileValidator {
        MCRValidationResult validate(Path pathToFile);
    }

    public static class MCRValidationResult {
        private boolean valid;

        private Optional<String> message;

        public MCRValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = Optional.ofNullable(message);
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public Optional<String> getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = Optional.ofNullable(message);
        }
    }

}
