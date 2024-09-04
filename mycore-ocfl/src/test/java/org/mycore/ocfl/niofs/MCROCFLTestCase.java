package org.mycore.ocfl.niofs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.repository.MCROCFLRepositoryProvider;

@RunWith(Parameterized.class)
public abstract class MCROCFLTestCase extends MCRTestCase {

    /**
     * This ocfl object is created on test startup.
     */
    protected static final String DERIVATE_1 = "junit_derivate_00000001";

    /**
     * This ocfl object is not created on test startup.
     */
    protected static final String DERIVATE_2 = "junit_derivate_00000002";

    protected MCROCFLRepository repository;

    private final boolean remote;

    @Parameterized.Parameters(name = "remote: {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] { { false }, { true } });
    }

    public MCROCFLTestCase(boolean remote) {
        this.remote = remote;
    }

    @Override
    protected Map<String, String> getTestProperties() {
        HashMap<String, String> properties = new HashMap<>();
        properties.put("MCR.OCFL.Repository.Test.FS.Remote", remote ? "true" : "false");
        return properties;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        String repositoryId = MCRConfiguration2.getString("MCR.Content.Manager.Repository").orElseThrow();
        MCROCFLRepositoryProvider repositoryProvider = MCROCFLRepositoryProvider.getProvider(repositoryId);
        repositoryProvider.init(MCROCFLRepositoryProvider.REPOSITORY_PROPERTY_PREFIX + repositoryId);
        this.repository = repositoryProvider.getRepository();

        MCROCFLFileSystemProvider.get().init();

        loadObject(DERIVATE_1);
    }

    @Override
    public void tearDown() throws Exception {
        MCROCFLFileSystemProvider.get().clearCache();
        MCRTransactionHelper.rollbackTransaction();
        MCROCFLFileSystemTransaction.resetTransactionCounter();
        super.tearDown();
        for (String objectId : this.repository.listObjectIds().toList()) {
            this.repository.purgeObject(objectId);
        }
    }

    protected void loadObject(String id) throws URISyntaxException, IOException {
        URL derivateURL = getClass().getClassLoader().getResource(id);
        if (derivateURL == null) {
            throw new IllegalArgumentException("Unable to locate '" + id + "' folder in resources.");
        }
        final Path sourcePath = Path.of(derivateURL.toURI());
        final MCRVersionedPath targetPath = MCRVersionedPath.head(id, "/");
        MCRTransactionHelper.beginTransaction();
        Files.walkFileTree(sourcePath, new CopyFileVisitor(targetPath));
        MCRTransactionHelper.commitTransaction();
    }

    private static class CopyFileVisitor extends SimpleFileVisitor<Path> {

        private final Path targetPath;

        private Path sourcePath = null;

        CopyFileVisitor(Path targetPath) {
            this.targetPath = targetPath;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            if (sourcePath == null) {
                sourcePath = dir;
            } else {
                Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.copy(file, targetPath.resolve(sourcePath.relativize(file)));
            return FileVisitResult.CONTINUE;
        }

    }

}
