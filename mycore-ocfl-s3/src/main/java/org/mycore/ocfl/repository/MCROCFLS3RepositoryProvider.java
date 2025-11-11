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

package org.mycore.ocfl.repository;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;

import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.events.MCRShutdownHandler;

import io.ocfl.aws.OcflS3Client;
import io.ocfl.core.extension.storage.layout.config.HashedNTupleIdEncapsulationLayoutConfig;
import io.ocfl.core.path.constraint.ContentPathConstraints;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * Provides an implementation of {@link MCROCFLRepositoryProvider} for managing OCFL repositories on AWS S3-compatible
 * storage.
 * <p>
 * This class sets up and initializes an OCFL repository backed by S3 storage. It handles the configuration of
 * AWS clients, transfer managers, and storage layers, leveraging MyCoRe's dependency injection framework to populate
 * necessary properties.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Integrates with S3-compatible storage for OCFL data persistence.</li>
 *   <li>Supports configurable client settings such as timeouts and concurrency limits.</li>
 *   <li>Registers proper shutdown handlers to ensure cleanup of S3 resources.</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * This provider relies on a set of MyCoRe configuration properties:
 * <ul>
 *   <li>{@code WorkDir}: Specifies the working directory for temporary storage.</li>
 *   <li>{@code Endpoint}: The S3 endpoint URL.</li>
 *   <li>{@code Bucket}: The S3 bucket name.</li>
 *   <li>{@code RepoPrefix}: The prefix for repository objects in the S3 bucket.</li>
 *   <li>{@code AccessKeyId}: AWS access key for authentication.</li>
 *   <li>{@code SecretAccessKey}: AWS secret key for authentication.</li>
 *   <li>Client-specific timeouts and concurrency settings.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * The repository is initialized during post-construction, and its working directory must be configured before use.
 */
public class MCROCFLS3RepositoryProvider implements MCROCFLRepositoryProvider {

    /**
     * AWS S3 client for low-level operations using the AWS Common Runtime (CRT).
     */
    private S3AsyncClient crtClient;

    /**
     * AWS S3 client for general-purpose operations.
     */
    private S3AsyncClient asyncClient;

    /**
     * Transfer manager for efficient file transfers to and from S3.
     */
    private S3TransferManager transferManager;

    /**
     * The OCFL repository managed by this provider.
     */
    private MCROCFLRepository repository;

    /**
     * Working directory for temporary or intermediate storage.
     */
    protected Path workDir;

    /**
     * Settings object populated from mycore.properties.
     */
    @MCRInstance(name = "S3", valueClass = S3Settings.class)
    public S3Settings settings;

    /**
     * Initializes the S3-based OCFL repository.
     * <p>
     * This method is called post-construction and performs the following:
     * <ul>
     *   <li>Creates the working directory if it does not exist.</li>
     *   <li>Initializes S3 clients and transfer manager with the provided settings.</li>
     *   <li>Builds the OCFL repository with S3 as the storage backend.</li>
     *   <li>Registers a shutdown handler to clean up resources on application close.</li>
     * </ul>
     *
     * @param prop the property key used to identify this repository configuration.
     * @throws IOException if an error occurs during initialization.
     */
    @MCRPostConstruction
    public void init(String prop) throws IOException {
        String id = prop.substring(REPOSITORY_PROPERTY_PREFIX.length());

        Files.createDirectories(workDir);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            settings.credentials.accessKeyId,
            settings.credentials.secretAccessKey);

        crtClient = S3AsyncClient.crtBuilder()
            .region(Region.EU_WEST_1)
            .endpointOverride(URI.create(settings.endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .forcePathStyle(true)
            .build();

        transferManager = S3TransferManager.builder()
            .s3Client(crtClient)
            .build();

        asyncClient = S3AsyncClient.builder()
            .region(Region.EU_WEST_1)
            .endpointOverride(URI.create(settings.endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .forcePathStyle(true)
            .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                .connectionAcquisitionTimeout(Duration.ofSeconds(settings.client.getConnectionAcquisitionTimeout()))
                .writeTimeout(Duration.ofSeconds(settings.client.getWriteTimeout()))
                .readTimeout(Duration.ofSeconds(settings.client.getReadTimeout()))
                .maxConcurrency(settings.client.getMaxConcurrency()))
            .build();

        repository = new MCROCFLRepositoryBuilder()
            .id(id)
            .remote(true)
            .defaultLayoutConfig(new HashedNTupleIdEncapsulationLayoutConfig())
            .contentPathConstraints(ContentPathConstraints.cloud())
            // TODO
            // - check if object lock is required
            // - check if we should store inventory details in DB
            // - ocfl-java uses JDBC
            //.objectLock(lock -> lock.dataSource(dataSource))
            //.objectDetailsDb(db -> db.dataSource(dataSource))
            .storage(storage -> storage
                .cloud(OcflS3Client.builder()
                    .s3Client(asyncClient)
                    .transferManager(transferManager)
                    .bucket(settings.bucket.toLowerCase(Locale.ROOT))
                    .repoPrefix(settings.repoPrefix.toLowerCase(Locale.ROOT))
                    .build()))
            .workDir(workDir)
            .buildMCR();

        MCRShutdownHandler.getInstance().addCloseable(this::onClose);
    }

    /**
     * Sets the working directory path for the OCFL repository.
     *
     * @param workDir the path to the repository's working directory.
     * @return this instance for chaining.
     */
    @MCRProperty(name = "WorkDir")
    public MCROCFLS3RepositoryProvider setWorkDir(String workDir) {
        this.workDir = Paths.get(workDir);
        return this;
    }

    /**
     * Cleans up resources such as S3 clients and transfer managers during shutdown.
     */
    protected void onClose() {
        this.transferManager.close();
        this.asyncClient.close();
        this.crtClient.close();
    }

    /**
     * Returns the OCFL repository managed by this provider.
     *
     * @return the {@link MCROCFLRepository} instance.
     */
    @Override
    public MCROCFLRepository getRepository() {
        return this.repository;
    }

    /**
     * Holds S3 configuration settings such as credentials, bucket, and repository prefix.
     */
    public static final class S3Settings {

        @MCRProperty(name = "Endpoint")
        public String endpoint;

        @MCRProperty(name = "Bucket")
        public String bucket;

        @MCRProperty(name = "RepoPrefix")
        public String repoPrefix;

        @MCRInstance(name = "Credentials", valueClass = S3CredentialSettings.class)
        public S3CredentialSettings credentials;

        @MCRInstance(name = "Client", valueClass = S3ClientSettings.class)
        public S3ClientSettings client;

    }

    /**
     * Holds AWS credentials for accessing S3 storage.
     */
    public static final class S3CredentialSettings {

        @MCRProperty(name = "AccessKeyId")
        public String accessKeyId;

        @MCRProperty(name = "SecretAccessKey")
        public String secretAccessKey;

    }

    /**
     * Holds S3 client-specific settings such as timeouts and concurrency limits.
     * TODO: should work directly with Integer and without getters -> mycore does not support this yet!
     */
    public static final class S3ClientSettings {

        @MCRProperty(name = "ConnectionAcquisitionTimeout")
        public String connectionAcquisitionTimeout;

        @MCRProperty(name = "WriteTimeout")
        public String writeTimeout;

        @MCRProperty(name = "ReadTimeout")
        public String readTimeout;

        @MCRProperty(name = "MaxConcurrency")
        public String maxConcurrency;

        public Integer getConnectionAcquisitionTimeout() {
            return Integer.parseInt(connectionAcquisitionTimeout);
        }

        public Integer getWriteTimeout() {
            return Integer.parseInt(writeTimeout);
        }

        public Integer getReadTimeout() {
            return Integer.parseInt(readTimeout);
        }

        public Integer getMaxConcurrency() {
            return Integer.parseInt(maxConcurrency);
        }
    }

}
