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

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

/**
 * Provider interface for managing access to OCFL repositories.
 * <p>
 * This interface defines methods to obtain and configure {@link MCROCFLRepository} instances,
 * enabling interaction with OCFL-compliant storage backends.
 * <p>
 * The {@code MCROCFLRepositoryProvider} interface also offers static utility methods to retrieve
 * repository providers based on unique repository identifiers, facilitating centralized access
 * to different repositories configured within the application.
 *
 * <h2>Configuration</h2>
 * Each repository provider is associated with a configuration prefix, {@code REPOSITORY_PROPERTY_PREFIX},
 * which defines the configuration path used to initialize specific repository instances within MyCoRe.
 */
public interface MCROCFLRepositoryProvider {

    /**
     * Configuration property prefix for OCFL repository settings.
     */
    String REPOSITORY_PROPERTY_PREFIX = "MCR.OCFL.Repository.";

    /**
     * Retrieves an instance of {@link MCROCFLRepository} associated with this provider.
     *
     * @return the OCFL repository managed by this provider.
     */
    MCROCFLRepository getRepository();

    /**
     * Returns the {@link MCROCFLRepository} associated with the specified repository ID.
     * <p>
     * This method retrieves the provider configured for the specified repository ID, then
     * returns the OCFL repository instance managed by that provider.
     *
     * @param id the unique identifier for the desired repository.
     * @return the {@link MCROCFLRepository} instance associated with the given ID.
     */
    static MCROCFLRepository getRepository(String id) {
        return obtainInstance(id).getRepository();
    }

    /**
     * Retrieves the {@link MCROCFLRepositoryProvider} configured for the specified repository ID.
     * <p>
     * This method uses MyCoRe's configuration system to dynamically locate and return the provider
     * instance matching the given repository ID.
     *
     * @param id the unique identifier for the desired repository provider.
     * @return the {@link MCROCFLRepositoryProvider} instance for the specified ID.
     * @throws MCRConfigurationException if no provider is configured for the specified ID.
     */
    static MCROCFLRepositoryProvider obtainInstance(String id) {
        return MCRConfiguration2.getSingleInstanceOfOrThrow(
            MCROCFLRepositoryProvider.class, REPOSITORY_PROPERTY_PREFIX + id);
    }

}
