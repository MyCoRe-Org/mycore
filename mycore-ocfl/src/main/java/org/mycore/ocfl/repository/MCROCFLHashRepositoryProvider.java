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

package org.mycore.ocfl.repository;

import io.ocfl.api.OcflRepository;
import io.ocfl.core.extension.OcflExtensionConfig;
import io.ocfl.core.extension.storage.layout.config.HashedNTupleIdEncapsulationLayoutConfig;
import io.ocfl.core.storage.OcflStorageBuilder;
import jakarta.inject.Singleton;

/**
 * Simple way to provide a {@link OcflRepository}
 */
@Singleton
public class MCROCFLHashRepositoryProvider extends MCROCFLRepositoryProvider {

    public OcflExtensionConfig getExtensionConfig() {
        return new HashedNTupleIdEncapsulationLayoutConfig();
    }

    @Override
    public OcflStorageBuilder getStorage(OcflStorageBuilder storageBuilder) {
        return storageBuilder.fileSystem(repositoryRoot);
    }

}
