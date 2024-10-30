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

import static org.mycore.common.config.MCRConfiguration2.splitValue;
import static org.mycore.user2.hash.MCRPasswordCheckManagerHelper.checkConfigurationHasNoIncompatibleChange;
import static org.mycore.user2.hash.MCRPasswordCheckManagerHelper.checkSelectedStrategyIsNotOutdated;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.annotation.MCROutdated;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRSentinel;

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
 * <li> Each strategy can be excluded from the configuration using the property {@link MCRSentinel#ENABLED_KEY}.
 * <li> The selected strategy is configured using the property suffix
 * {@link MCRPasswordCheckManager#SELECTED_STRATEGY_KEY}.
 * <li> The property suffix {@link MCRPasswordCheckManager#CONFIGURATION_CHECKS_KEY} can be used to enable or
 * disable configuration checks during instantiation, specifically: (1) whether a strategy annotated with
 * {@link MCROutdated} has been selected and (2) whether the configuration of a strategy has been changed in a way
 * that will prevent existing password hashes from being successfully verified, even if the correct password
 * was supplied.
 * </ul>
 * Example:
 * <pre>
 * MCR.User.PasswordCheck.Class=org.mycore.user2.hash.MCRPasswordCheckManager
 * MCR.User.PasswordCheck.Strategies.foo.Class=foo.bar.FooStrategy
 * MCR.User.PasswordCheck.Strategies.foo.Enabled=true
 * MCR.User.PasswordCheck.Strategies.foo.Key1=Value1
 * MCR.User.PasswordCheck.Strategies.foo.Key2=Value2
 * MCR.User.PasswordCheck.Strategies.bar.Class=foo.bar.FooStrategy
 * MCR.User.PasswordCheck.Strategies.bar.Enabled=false
 * MCR.User.PasswordCheck.Strategies.bar.Key1=Value1
 * MCR.User.PasswordCheck.Strategies.bar.Key2=Value2
 * MCR.User.PasswordCheck.SelectedStrategy=foo
 * MCR.User.PasswordCheck.ConfigurationChecks=OUTDATED_STRATEGY,INCOMPATIBLE_CHANGE
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRPasswordCheckManager.Factory.class)
public final class MCRPasswordCheckManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRPasswordCheckManager INSTANCE = instantiate();

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

        LOGGER.info("Working with strategies: " + String.join(", ", strategies.keySet()));
        LOGGER.info("Creating new password hashes with strategy: " + selectedStrategyType);

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

    public static class Factory implements Supplier<MCRPasswordCheckManager> {

        @MCRInstanceMap(name = STRATEGIES_KEY, valueClass = MCRPasswordCheckStrategy.class, sentinel = @MCRSentinel)
        public Map<String, MCRPasswordCheckStrategy> strategies;

        @MCRProperty(name = SELECTED_STRATEGY_KEY)
        public String selectedStrategy;

        @MCRProperty(name = CONFIGURATION_CHECKS_KEY)
        public String configurationChecks;

        @Override
        public MCRPasswordCheckManager get() {

            SecureRandom random = getStrongSecureRandom();

            Set<ConfigurationCheck> configurationChecks = splitValue(this.configurationChecks)
                .map(ConfigurationCheck::valueOf).collect(Collectors.toSet());

            return new MCRPasswordCheckManager(random, strategies, selectedStrategy, configurationChecks);

        }

        private static SecureRandom getStrongSecureRandom() {
            try {
                return SecureRandom.getInstanceStrong();
            } catch (NoSuchAlgorithmException e) {
                throw new MCRException("Failed to obtain strong secure random number generator", e);
            }
        }

    }

    public enum ConfigurationCheck {

        OUTDATED_STRATEGY,

        INCOMPATIBLE_CHANGE;

    }

}
