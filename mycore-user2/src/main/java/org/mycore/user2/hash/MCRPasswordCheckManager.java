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

package org.mycore.user2.hash;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mycore.common.MCRException;
import org.mycore.common.annotation.MCROutdated;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.resource.MCRResourceHelper;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mycore.common.config.MCRConfiguration2.splitValue;

/**
 * A {@link MCRPasswordCheckManager} can be used to create password hashes and to verify an existing hash
 * against a given password, without knowledge of the underlying algorithm that performs these actions. To do so,
 * it uses {@link MCRPasswordCheckStrategy} instances that each implement a password hashing and verification strategy.
 * A single strategy is selected to create new password hashes. Alle strategies are available to verify existing
 * password hashes.
 * <p>
 * The verification result is marked as deprecated, when the strategy used to verify the hash isn't the
 * selected strategy or if the used strategy already marked it as deprecated.
 * <p>
 * A singular, globally available and centrally configured instance can be obtained with
 * {@link MCRPasswordCheckManager#instance()}. This instance is configured using the property prefix
 * {@link MCRPasswordCheckManager#MANAGER_PROPERTY} and should be used in order to create or verify password hashes,
 * although custom instances can be created when necessary.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> Strategies are configured as a map using the property suffix {@link MCRPasswordCheckManager#STRATEGIES_KEY}.
 * <li> The selected strategy is configured using the property suffix
 * {@link MCRPasswordCheckManager#SELECTED_STRATEGY_KEY}.
 * <li> The property suffix {@link MCRPasswordCheckManager#CONFIGURATION_CHECKS_KEY} can be used to enable or
 * disable configuration checks during instantiation, specifically: (1) whether the configuration of a selector has
 * been changed in a way that will prevent existing password hashes from being successfully verified, even if the
 * correct password was supplied and (2) whether a strategy annotated with {@link MCROutdated} has been selected.
 * </ul>
 * Example:
 * <pre>
 * MCR.User.PasswordCheck.Class=org.mycore.user2.hash.MCRPasswordCheckManager
 * MCR.User.PasswordCheck.Strategies.foo.Class=foo.bar.FooStrategy
 * MCR.User.PasswordCheck.Strategies.foo.Key1=Value1
 * MCR.User.PasswordCheck.Strategies.foo.Key2=Value2
 * MCR.User.PasswordCheck.Strategies.bar.Class=foo.bar.FooStrategy
 * MCR.User.PasswordCheck.Strategies.bar.Key1=Value1
 * MCR.User.PasswordCheck.Strategies.bar.Key2=Value2
 * MCR.User.PasswordCheck.SelectedStrategy=foo
 * MCR.User.PasswordCheck.ConfigurationChecks=OUTDATED_STRATEGY,INCOMPATIBLE_CHANGE
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRPasswordCheckManager.Factory.class)
public final class MCRPasswordCheckManager {

    private static final MCRPasswordCheckManager INSTANCE = instantiate();

    private static final String DATA_DIRECTORY_NAME = "passwordCheckStrategies";

    public static final String MANAGER_PROPERTY = "MCR.User.PasswordCheck";

    public static final String STRATEGIES_KEY = "Strategies";

    public static final String SELECTED_STRATEGY_KEY = "SelectedStrategy";

    public static final String CONFIGURATION_CHECKS_KEY = "ConfigurationChecks";

    private final SecureRandom random;

    private final Map<String, MCRPasswordCheckStrategy> strategies;

    private final MCRPasswordCheckStrategy selectedStrategy;

    private final String selectedStrategyType;

    public MCRPasswordCheckManager(SecureRandom random, Map<String, MCRPasswordCheckStrategy> strategies,
                                   String selectedStrategyType, Set<ConfigurationCheck> configurationChecks) {
        this.random = Objects.requireNonNull(random, "Random must not be null");
        this.strategies = new HashMap<>(Objects.requireNonNull(strategies, "Strategies must not be null"));
        this.strategies.values().forEach(strategy -> Objects.requireNonNull(strategy, "Strategy must not be null"));
        this.selectedStrategyType = Objects.requireNonNull(selectedStrategyType, "Selected strategy must not be null");
        this.selectedStrategy = this.strategies.get(selectedStrategyType);
        if (this.selectedStrategy == null) {
            throw new IllegalArgumentException("Selected strategy " + selectedStrategyType + " unavailable, got: "
                + String.join(", ", this.strategies.keySet()));
        }
        if (configurationChecks.contains(ConfigurationCheck.OUTDATED_STRATEGY)) {
            checkSelectedStrategyIsNotOutdated(selectedStrategyType, selectedStrategy);
        }
        if (configurationChecks.contains(ConfigurationCheck.INCOMPATIBLE_CHANGE)) {
            checkConfigurationHasNoIncompatibleChange(strategies);
        }
    }

    private void checkSelectedStrategyIsNotOutdated(String type, MCRPasswordCheckStrategy strategy) {

        Class<? extends MCRPasswordCheckStrategy> selectedStrategyClass = strategy.getClass();

        if (selectedStrategyClass.isAnnotationPresent(MCROutdated.class)) {
            throw new MCRConfigurationException("Detected outdated password check strategy implementation " +
                selectedStrategyClass.getName() + " for selected password check strategy " + type + ", expected " +
                "an implementation that is not outdated");
        }

    }

    private void checkConfigurationHasNoIncompatibleChange(Map<String, MCRPasswordCheckStrategy> strategies) {

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

        // previously seen configuration values (those, that must not change)
        // might have been stored for comparison as a file when a custom provider was first encountered, ...
        File file = MCRConfigurationDir.getConfigFile(path);
        if (file != null && file.exists()) {
            if (!file.isFile() || !file.canRead()) {
                throw new MCRException("Expected " + file.getAbsolutePath() + " to be a readable file");
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(file, UTF_8), 128)) {
                return Optional.of(readUnmodifiableConfiguration(reader));
            } catch (IOException e) {
                throw new MCRException("Unable to read file " + file.getAbsolutePath());
            }
        }

        // ... or are provided as a resource in case of a built-in provider
        InputStream stream = MCRResourceHelper.getResourceAsStream(path);
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, UTF_8), 128)) {
                return Optional.of(readUnmodifiableConfiguration(reader));
            } catch (IOException e) {
                throw new MCRException("Unable to read resource " + path);
            }
        }

        return Optional.empty();

    }

    private static UnmodifiableConfiguration readUnmodifiableConfiguration(BufferedReader reader) throws IOException {
        String className = reader.readLine();
        String hint = reader.readLine();
        return new UnmodifiableConfiguration(className, hint);
    }

    private UnmodifiableConfiguration toUnmodifiableConfiguration(MCRPasswordCheckStrategy strategy) {
        return new UnmodifiableConfiguration(strategy.getClass().getName(), strategy.unmodifiableConfigurationHint());
    }

    private void checkConfigurationHasNoIncompatibleChange(String type, UnmodifiableConfiguration oldConfiguration,
                                                           UnmodifiableConfiguration newConfiguration) {

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

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, UTF_8), 128)) {
                writer.write(configuration.className());
                writer.newLine();
                writer.write(configuration.hint());
                writer.newLine();
            } catch (IOException e) {
                throw new MCRException("Unable to write to value file " + file.getAbsolutePath());
            }

        }

    }

    public static MCRPasswordCheckManager instance() {
        return INSTANCE;
    }

    public static MCRPasswordCheckManager instantiate() {
        String classProperty = MANAGER_PROPERTY + ".Class";
        return MCRConfiguration2.getInstanceOfOrThrow(MCRPasswordCheckManager.class, classProperty);
    }

    public MCRPasswordCheckData create(String password) {
        return selectedStrategy.create(random, selectedStrategyType, password);
    }

    public MCRPasswordCheckResult verify(MCRPasswordCheckData data, String password) {
        MCRPasswordCheckResult result = getStrategy(data.type()).verify(data, password);
        if (selectedStrategyType.equals(data.type())) {
            return result;
        } else {
            return new MCRPasswordCheckResult(result.valid(), true);
        }
    }

    private MCRPasswordCheckStrategy getStrategy(String type) {
        MCRPasswordCheckStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new MCRException("Unknown password check strategy type " + type);
        }
        return strategy;
    }

    private record UnmodifiableConfiguration(String className, String hint) {
    }

    public enum ConfigurationCheck {

        OUTDATED_STRATEGY,

        INCOMPATIBLE_CHANGE;

    }

    public static class Factory implements Supplier<MCRPasswordCheckManager> {

        @MCRInstanceMap(name = STRATEGIES_KEY, valueClass = MCRPasswordCheckStrategy.class)
        public Map<String, MCRPasswordCheckStrategy> strategies;

        @MCRProperty(name = SELECTED_STRATEGY_KEY)
        public String selectedStrategy;

        @MCRProperty(name = CONFIGURATION_CHECKS_KEY)
        public String configurationChecks;

        @Override
        public MCRPasswordCheckManager get() {

            Set<ConfigurationCheck> configurationChecks = splitValue(this.configurationChecks)
                .map(ConfigurationCheck::valueOf).collect(Collectors.toSet());

            return new MCRPasswordCheckManager(getStrongSecureRandom(), strategies, selectedStrategy,
                configurationChecks);

        }

        private static SecureRandom getStrongSecureRandom() {
            try {
                return SecureRandom.getInstanceStrong();
            } catch (NoSuchAlgorithmException e) {
                throw new MCRException("Failed to obtain strong secure random number generator", e);
            }
        }

    }

}
