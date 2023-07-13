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

import io.ocfl.api.OcflRepository;
import io.ocfl.core.extension.OcflExtensionConfig;

/**
 * Base Class to provide a {@link OcflRepository}. A {@link MCROCFLRepositoryProvider} will be loaded from the property
 * <code>MCR.OCFL.Repository.%id%</code> and the Method getRepository will be executed.
 */
public abstract class MCROCFLRepositoryProvider {

    public static final String REPOSITORY_PROPERTY_PREFIX = "MCR.OCFL.Repository.";

    public static OcflRepository getRepository(String id) {
        return MCRConfiguration2.getSingleInstanceOf(REPOSITORY_PROPERTY_PREFIX + id)
            .map(MCROCFLRepositoryProvider.class::cast)
            .get()
            .getRepository();
    }

    public abstract OcflRepository getRepository();

    public abstract OcflExtensionConfig getExtensionConfig();
}
