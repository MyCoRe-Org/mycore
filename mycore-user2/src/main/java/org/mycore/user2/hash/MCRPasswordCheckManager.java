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
import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.log.MCRListMessage;
import org.mycore.resource.MCRResourceHelper;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A {@link MCRPasswordCheckManager} can be used to create hashes of passwords and to verify an existing hash
 * against a given password, without knowledge of the underlying algorithm that performs these actions.
 * <p>
 * Multiple instances of {@link MCRPasswordCheckStrategy} can be configured using the property prefix
 * {@link MCRPasswordCheckManager#STRATEGIES_KEY} and a freely selectable name. Each user password check strategy
 * implements a strategy to create hashes of passwords and to verify an existing hash against a given password.
 * <p>
 * Example:
 * <pre>
 * MCR.User.PasswordCheck.Strategies.foo.Class=my.package.MCRFooPasswordCheckStrategy
 * </pre>
 * This will create an instance of <code>MCRFooPasswordCheckStrategy</code> and make it available with the schema
 * <code>foo</code>.
 * <p>
 * A single strategy is configured as the preferred strategy using the property
 * {@link MCRPasswordCheckManager#STRATEGY_KEY}.
 * <p>
 * The preferred strategy is used when creating a hash of a password. All configured strategies are available when
 * verifying an existing hash against a given password. The verification result is marked as deprecated, when the
 * strategy used to verify the hash isn't the preferred strategy or if the used strategy marked it as deprecated.
 */
public final class MCRPasswordCheckManager {

    public static final String STRATEGIES_KEY = "MCR.User.PasswordCheck.Strategies";

    public static final String STRATEGY_KEY = "MCR.User.PasswordCheck.Strategy";

    public static final String RANDOM_ALGORITHM_KEY = "MCR.User.PasswordCheck.RandomAlgorithm";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final MCRPasswordCheckManager INSTANCE = new MCRPasswordCheckManager(getConfiguredRandom(),
        getConfiguredStrategies(), getConfiguredStrategy(), true);

    private final SecureRandom random;

    private final Map<String, MCRPasswordCheckStrategy> strategies;

    private final MCRPasswordCheckStrategy preferredStrategy;

    private final String preferredStrategyType;

    public MCRPasswordCheckManager() {
        this(getConfiguredRandom(), getConfiguredStrategies(), getConfiguredStrategy(), false);
    }

    public MCRPasswordCheckManager(SecureRandom random, Map<String, MCRPasswordCheckStrategy> strategies,
                                   String preferredStrategyType, boolean checkIncompatibleConfigurationChange) {
        this.random = Objects.requireNonNull(random);
        this.strategies = new HashMap<>(Objects.requireNonNull(strategies));
        this.strategies.values().forEach(Objects::requireNonNull);
        this.preferredStrategyType = Objects.requireNonNull(preferredStrategyType);
        this.preferredStrategy = this.strategies.get(preferredStrategyType);
        if (this.preferredStrategy == null) {
            throw new IllegalArgumentException("Preferred strategy " + preferredStrategyType + " unavailable, got: "
                + String.join(", ", this.strategies.keySet()));
        }
        if (checkIncompatibleConfigurationChange) {
            checkIncompatibleConfigurationChange(strategies);
        }
    }

    private void checkIncompatibleConfigurationChange(Map<String, MCRPasswordCheckStrategy> strategies) {

        for (Map.Entry<String, MCRPasswordCheckStrategy> entry : strategies.entrySet()) {
            String name = entry.getKey();
            String newValue = entry.getValue().invariableConfigurationString();
            String oldValue = loadInvariableConfigurationString(name);

            System.out.println("--- " + name);
            System.out.println("-- " + oldValue);
            System.out.println("- " + newValue);

            if (oldValue != null && !oldValue.equals(newValue)) {
                throw new MCRConfigurationException("Detected incompatible configuration change for password check " +
                    "strategy " + name + " that will prevent existing passwords from being successfully verified, " +
                    "even if the correct password was supplied, got " + newValue + ", expected " + oldValue);
            }
            if (oldValue == null) {
                storeInvariableConfigurationString(name, newValue);
            }
        }


    }

    private static String loadInvariableConfigurationString(String name) {

        String path = "passwordCheckStrategies/" + name;

        File file = MCRConfigurationDir.getConfigFile(path);
        if (file != null && file.exists()) {

            System.out.println("READING " + file.getAbsolutePath());

            try (BufferedReader reader = new BufferedReader(new FileReader(file, UTF_8), 128)) {
                return reader.readLine();
            } catch (IOException e) {
                throw new MCRConfigurationException("Unable to read from configuration file " +
                    file.getAbsolutePath());
            }
        }

        InputStream stream = MCRResourceHelper.getResourceAsStream(path);
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, UTF_8), 128)) {
                return reader.readLine();
            } catch (IOException e) {
                throw new MCRException("Unable to read from configuration resource " + path);
            }
        }

        return null;

    }

    private static void storeInvariableConfigurationString(String name, String value) {

        String path = "passwordCheckStrategies/" + name;

        File file = MCRConfigurationDir.getConfigFile(path);
        if (file != null) {

            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                boolean parentDirCreated = parentDir.mkdirs();
                if (!parentDirCreated) {
                    throw new MCRException("Unable to create configuration directory " + parentDir.getAbsolutePath());
                }
            }

            System.out.println("WRITING " + file.getAbsolutePath());

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, UTF_8), 128)) {
                writer.write(value);
                writer.newLine();
            } catch (IOException e) {
                throw new MCRException("Unable to write to configuration file " + file.getAbsolutePath());
            }
        }

    }

    public static SecureRandom getConfiguredRandom() {
        try {
            return SecureRandom.getInstance(MCRConfiguration2.getStringOrThrow(RANDOM_ALGORITHM_KEY));
        } catch (NoSuchAlgorithmException e) {
            throw new MCRConfigurationException("Could not initialize secure random number generator", e);
        }
    }

    public static Map<String, MCRPasswordCheckStrategy> getConfiguredStrategies() {

        String prefix = STRATEGIES_KEY + ".";
        Map<String, Callable<MCRPasswordCheckStrategy>> strategyFactoriesByProperty =
            MCRConfiguration2.getInstances(prefix);

        MCRListMessage description = new MCRListMessage();
        Map<String, MCRPasswordCheckStrategy> strategiesByType = new HashMap<>();
        for (String property : strategyFactoriesByProperty.keySet()) {
            try {
                String type = property.substring(prefix.length());
                MCRPasswordCheckStrategy strategy = strategyFactoriesByProperty.get(property).call();
                description.add(type, strategy.getClass().getName());
                strategiesByType.put(type, strategy);
            } catch (Exception e) {
                throw new MCRConfigurationException("Failed to instantiate strategy configured in: " + property, e);
            }
        }
        LOGGER.info(description.logMessage("Checking password with strategies:"));

        return strategiesByType;

    }

    public static String getConfiguredStrategy() {
        return MCRConfiguration2.getStringOrThrow(STRATEGY_KEY);
    }

    public static MCRPasswordCheckManager instance() {
        return INSTANCE;
    }

    public MCRPasswordCheckData create(String password) {
        return preferredStrategy.create(random, preferredStrategyType, password);
    }

    public MCRPasswordCheckResult verify(MCRPasswordCheckData data, String password) {
        MCRPasswordCheckResult result = getStrategy(data.type()).verify(data, password);
        if (preferredStrategyType.equals(data.type())) {
            return result;
        } else {
            return new MCRPasswordCheckResult(result.valid(), true);
        }
    }

    private MCRPasswordCheckStrategy getStrategy(String type) {
        MCRPasswordCheckStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new MCRException("Unknown type " + type);
        }
        return strategy;
    }

}
