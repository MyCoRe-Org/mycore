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

import static org.mycore.backend.jpa.MCRPersistenceProvider.JPA_PERSISTENCE_UNIT_PROPERTY_NAME;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRInstanceName;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;
import org.mycore.resource.MCRResourceResolver;
import org.mycore.resource.provider.MCRResourceProvider;

import jakarta.servlet.ServletContext;

public class MCRJPAConfigurationCheck implements AutoExecutable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String MYCORE_JPA_DOCUMENTATION_URL =
        "https://www.mycore.de/documentation/getting_started/gs_jpa/?mark=datenbank";

    public static final String PERFORM_CONFIGURATION_CHECK_PROPERTY = "MCR.JPA.PerformConfigurationCheck";

    @Override
    public String getName() {
        return "JPA Configuration Check";
    }

    @Override
    public int getPriority() {
        return 1001;
    }

    @Override
    public void startUp(ServletContext servletContext) {

        boolean performConfigurationChecks = MCRConfiguration2.getBoolean(PERFORM_CONFIGURATION_CHECK_PROPERTY)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(PERFORM_CONFIGURATION_CHECK_PROPERTY));

        if (performConfigurationChecks) {
            performConfigurationChecks();
        }

    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private void performConfigurationChecks() {

        Optional<String> selectedUnit = MCRConfiguration2.getString("MCR.JPA.PersistenceUnitName");
        selectedUnit.ifPresent(unit -> LOGGER.trace(() -> "Selected persistence unit: " + unit + " ***"));

        Set<String> unitsFromMycoreProperties = MCRConfiguration2
            .getInstantiatablePropertyKeys(JPA_PERSISTENCE_UNIT_PROPERTY_NAME)
            .map(name -> MCRInstanceName.of(name).canonical())
            .map(name -> name.substring(JPA_PERSISTENCE_UNIT_PROPERTY_NAME.length()))
            .collect(Collectors.toSet());

        LOGGER.trace(() -> "*** Persistence units configured in MyCoRe properties: "
            + String.join(", ", unitsFromMycoreProperties) + " ***");

        Set<String> unitsFromPersistenceXml = locatePersistenceUnitNamesFromPersistenceXml();

        LOGGER.trace(() -> "*** Persistence units configured in persistence.xml: "
            + String.join(", ", unitsFromPersistenceXml) + " ***");

        if (unitsFromMycoreProperties.isEmpty() && !unitsFromPersistenceXml.isEmpty()) {
            LOGGER.info(() -> "*** Persistence units are configured in persistence.xml. "
                + "Consider migrating to MyCoRe properties, see " + MYCORE_JPA_DOCUMENTATION_URL + " ***");
        }

        if (!unitsFromMycoreProperties.isEmpty() && !unitsFromPersistenceXml.isEmpty()) {
            LOGGER.warn(() -> "*** Persistence units are configured in MyCoRe properties "
                + "as well as in persistence.xml. This is most likely an unintended configuration. ***");
        }

        Set<String> unitsConfiguredTwice = intersect(unitsFromMycoreProperties, unitsFromPersistenceXml);
        if (!unitsConfiguredTwice.isEmpty()) {
            LOGGER.warn(() -> "*** Persistence units (" + String.join(", ", unitsConfiguredTwice) + ") "
                + "configured in MyCoRe properties and in persistence.xml. "
                + "This is most likely a mistake. ***");
        }

        selectedUnit.ifPresent(unit -> {
            if (unitsConfiguredTwice.contains(unit)) {
                LOGGER.warn(() -> "*** Selected Persistence unit (" + unit + ") "
                    + "configured in MyCoRe properties and in persistence.xml. "
                    + "This is most definitely a mistake. ***");
            }
        });

    }

    private Set<String> locatePersistenceUnitNamesFromPersistenceXml() {
        List<URL> persistenceXmlUrls = MCRResourceResolver.obtainInstance()
            .resolveAllResource("META-INF/persistence.xml")
            .stream()
            .map(MCRResourceProvider.ProvidedUrl::url)
            .toList();
        if (persistenceXmlUrls.isEmpty()) {
            return Collections.emptySet();
        }

        try {
            Map<String, PersistenceUnitDescriptor> descriptors =
                PersistenceXmlParser.create(Collections.emptyMap()).parse(persistenceXmlUrls);

            return descriptors.values().stream()
                .map(PersistenceUnitDescriptor::getName)
                .collect(Collectors.toSet());
        } catch (RuntimeException e) {
            LOGGER.warn("Could not parse persistence.xml files for configuration check.", e);
            return Collections.emptySet();
        }
    }

    private static Set<String> intersect(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        return intersection;
    }

}
