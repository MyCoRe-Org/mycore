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

package org.mycore.backend.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.mycore.common.config.MCRConfiguration2;

/**
 * Adds simpler and name conforming property configuration on top of {@link MCRPersistenceUnitDescriptor}
 */
public class MCRSimpleConfigPersistenceUnitDescriptor extends MCRPersistenceUnitDescriptor {

    private static final Map<String, String> MAP = new HashMap<>();

    public MCRSimpleConfigPersistenceUnitDescriptor() {
        super();

        MAP.put("jakarta.persistence.jdbc.driver", "MCR.JPA.Driver");
        MAP.put("jakarta.persistence.jdbc.url", "MCR.JPA.URL");
        MAP.put("jakarta.persistence.jdbc.user", "MCR.JPA.User");
        MAP.put("jakarta.persistence.jdbc.password", "MCR.JPA.Password");

        MAP.put("hibernate.cache.use_second_level_cache", "MCR.JPA.Cache.UseSecondLevelCache");
        MAP.put("hibernate.cache.use_query_cache", "MCR.JPA.Cache.UseQueryCache");
        MAP.put("hibernate.cache.region.factory_class", "MCR.JPA.Cache.RegionFactoryClass");

        MAP.put("hibernate.globally_quoted_identifiers_skip_column_definitions",
            "MCR.JPA.GloballyQuotedIdentifiers.SkipColumnDefinitions");
        MAP.put("hibernate.globally_quoted_identifiers", "MCR.JPA.GloballyQuotedIdentifiers");
        MAP.put("hibernate.show_sql", "MCR.JPA.ShowSql");
        MAP.put("hibernate.hbm2ddl.auto", "MCR.JPA.Hbm2ddlAuto");
        MAP.put("hibernate.default_schema", "MCR.JPA.DefaultSchema");

        MAP.put("hibernate.connection.provider_class", "MCR.JPA.Connection.ProviderClass");
        MAP.put("hibernate.hikari.maximumPoolSize", "MCR.JPA.Connection.MaximumPoolSize");
        MAP.put("hibernate.hikari.minimumIdle", "MCR.JPA.Connection.MinimumIdle");
        MAP.put("hibernate.hikari.idleTimeout", "MCR.JPA.Connection.IdleTimeout");
        MAP.put("hibernate.hikari.maxLifetime", "MCR.JPA.Connection.MaxLifetime");
        MAP.put("hibernate.hikari.connectionTimeout", "MCR.JPA.Connection.ConnectionTimeout");
        MAP.put("hibernate.hikari.leakDetectionThreshold", "MCR.JPA.Connection.LeakDetectionThreshold");
        MAP.put("hibernate.hikari.registerMbeans", "MCR.JPA.Connection.RegisterMbeans");
    }

    @Override
    public Properties getProperties() {
        Properties properties = super.getProperties();
        MAP.forEach((k, v) -> putSimpleProperty(properties, v, k));
        return properties;
    }

    private void putSimpleProperty(Properties properties, String mcrSimplePropertyName, String jpaPropertyName) {
        if (!properties.contains(jpaPropertyName)) {
            Optional<String> optionalProp = MCRConfiguration2.getString(mcrSimplePropertyName);
            optionalProp.ifPresent(s -> properties.put(jpaPropertyName, s));
        }
    }
}
