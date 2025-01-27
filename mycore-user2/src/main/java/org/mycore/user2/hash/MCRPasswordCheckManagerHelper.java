/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.user2.hash;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

import org.mycore.common.MCRException;
import org.mycore.common.annotation.MCROutdated;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.resource.MCRResourceHelper;

/**
 * Helper class that performs optional configuration checks for instances of {@link MCRPasswordCheckManager}.
 * <p>
 * The check selected with {@link MCRPasswordCheckManager.ConfigurationCheck#OUTDATED_STRATEGY} ensures that
 * the selected strategy is not annotated with {@link MCROutdated}.
 * <p>
 * The check selected with {@link MCRPasswordCheckManager.ConfigurationCheck#INCOMPATIBLE_CHANGE} ensures that
 * the configuration of a strategy has not been changed in a way that will prevent existing password hashes from
 * being successfully verified, even if the correct password was supplied.
 * For this, every strategies implementation of {@link MCRPasswordCheckStrategy#unmodifiableConfigurationHint()} must
 * provide a string that encodes the relevant configuration values. When a new strategy is first encountered,
 * this value, together with the fully qualified class name of the strategy implementation is stored in a file
 * inside a subdirectory named {@link MCRPasswordCheckManagerHelper#DATA_DIRECTORY_NAME} in the applications
 * configuration directory. The content of this file is compared to the corresponding values, when a strategy is
 * encountered again. For build-in strategies, corresponding files are provided as a ressource.
 */
final class MCRPasswordCheckManagerHelper {

    private static final String DATA_DIRECTORY_NAME = "passwordCheckStrategies";

    private MCRPasswordCheckManagerHelper() {
    }

    static void checkSelectedStrategyIsNotOutdated(String type, MCRPasswordCheckStrategy strategy) {

        Class<? extends MCRPasswordCheckStrategy> selectedStrategyClass = strategy.getClass();

        if (selectedStrategyClass.isAnnotationPresent(MCROutdated.class)) {
            throw new MCRConfigurationException("Detected outdated password check strategy implementation " +
                selectedStrategyClass.getName() + " for selected password check strategy " + type + ", expected " +
                "an implementation that is not outdated");
        }

    }

    static void checkConfigurationHasNoIncompatibleChange(Map<String, MCRPasswordCheckStrategy> strategies) {

        for (Map.Entry<String, MCRPasswordCheckStrategy> entry : strategies.entrySet()) {

            String type = entry.getKey();
            Optional<UnmodifiableConfiguration> oldConfiguration = loadUnmodifiableConfiguration(type);
            UnmodifiableConfiguration newConfiguration = toUnmodifiableConfiguration(entry.getValue());

            if (oldConfiguration.isPresent()) {
                checkConfigurationHasNoIncompatibleChange(type, oldConfiguration.get(), newConfiguration);
            } else {
                storeUnmodifiableConfiguration(type, newConfiguration);
            }

        }

    }

    private static Optional<UnmodifiableConfiguration> loadUnmodifiableConfiguration(String type) {

        String path = DATA_DIRECTORY_NAME + "/" + type;

        File file = MCRConfigurationDir.getConfigFile(path);
        if (file != null && file.exists()) {
            if (!file.isFile() || !file.canRead()) {
                throw new MCRException("Expected " + file.getAbsolutePath() + " to be a readable file");
            }
            try (BufferedReader reader = new BufferedReader(Files.newBufferedReader(file.toPath(), UTF_8), 128)) {
                return Optional.of(readUnmodifiableConfiguration(reader));
            } catch (IOException e) {
                throw new MCRException("Unable to read file " + file.getAbsolutePath(), e);
            }
        }

        InputStream stream = MCRResourceHelper.getResourceAsStream(path);
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, UTF_8), 128)) {
                return Optional.of(readUnmodifiableConfiguration(reader));
            } catch (IOException e) {
                throw new MCRException("Unable to read resource " + path, e);
            }
        }

        return Optional.empty();

    }

    private static UnmodifiableConfiguration readUnmodifiableConfiguration(BufferedReader reader) throws IOException {
        String className = reader.readLine();
        String hint = reader.readLine();
        return new UnmodifiableConfiguration(className, hint);
    }

    private static UnmodifiableConfiguration toUnmodifiableConfiguration(MCRPasswordCheckStrategy strategy) {
        return new UnmodifiableConfiguration(strategy.getClass().getName(), strategy.unmodifiableConfigurationHint());
    }

    private static void checkConfigurationHasNoIncompatibleChange(String type,
        UnmodifiableConfiguration oldConfiguration, UnmodifiableConfiguration newConfiguration) {

        if (!oldConfiguration.className().equals(newConfiguration.className())) {
            throw new MCRConfigurationException("Detected incompatible implementation change for password " +
                "check strategy " + type + " that will prevent existing password hashes from being " +
                "successfully verified, even if the correct password was supplied, got " +
                newConfiguration.className() + ", expected " + oldConfiguration.className());
        }

        if (!oldConfiguration.hint().equals(newConfiguration.hint())) {
            throw new MCRConfigurationException("Detected incompatible value change for password " +
                "check strategy " + type + " that will prevent existing password hashes from being " +
                "successfully verified, even if the correct password was supplied, got " +
                newConfiguration.hint() + ", expected " + oldConfiguration.hint());
        }

    }

    private static void storeUnmodifiableConfiguration(String name, UnmodifiableConfiguration configuration) {

        String path = DATA_DIRECTORY_NAME + "/" + name;

        File file = MCRConfigurationDir.getConfigFile(path);
        if (file != null) {

            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                boolean parentDirCreated = parentDir.mkdirs();
                if (!parentDirCreated) {
                    throw new MCRException("Unable to create value directory " + parentDir.getAbsolutePath());
                }
            }

            try (BufferedWriter writer = new BufferedWriter(Files.newBufferedWriter(file.toPath(), UTF_8), 128)) {
                writer.write(configuration.className());
                writer.newLine();
                writer.write(configuration.hint());
                writer.newLine();
            } catch (IOException e) {
                throw new MCRException("Unable to write to value file " + file.getAbsolutePath(), e);
            }

        }

    }

    private record UnmodifiableConfiguration(String className, String hint) {
    }

}
