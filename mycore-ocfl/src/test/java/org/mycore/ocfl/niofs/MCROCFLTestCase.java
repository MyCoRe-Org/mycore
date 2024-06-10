package org.mycore.ocfl.niofs;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.OffsetDateTime;

import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.repository.MCROCFLRepositoryProvider;

import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.VersionInfo;

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

    protected ObjectVersionId derivateVersionId;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        String repositoryId = MCRConfiguration2.getString("MCR.Content.Manager.Repository").orElseThrow();
        MCROCFLRepositoryProvider repositoryProvider = MCROCFLRepositoryProvider.getProvider(repositoryId);
        repositoryProvider.init(MCROCFLRepositoryProvider.REPOSITORY_PROPERTY_PREFIX + repositoryId);
        this.repository = repositoryProvider.getRepository();

        MCROCFLFileSystemProvider.get().init();

        this.derivateVersionId = loadObject(DERIVATE_1);
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

    protected ObjectVersionId loadObject(String id) throws URISyntaxException {
        URL derivateURL = getClass().getClassLoader().getResource(id);
        if (derivateURL == null) {
            throw new NullPointerException("Unable to locate '" + id + "' folder in resources.");
        }
        return repository.putObject(
            ObjectVersionId.head(id),
            Path.of(derivateURL.toURI()),
            new VersionInfo()
                .setMessage("created")
                .setCreated(OffsetDateTime.now())
                .setUser("junit", ""));
    }

}
