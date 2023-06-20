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

import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.ocfl.layout.MCRStorageLayoutConfig;
import org.mycore.ocfl.layout.MCRStorageLayoutExtension;

import io.ocfl.core.extension.OcflExtensionConfig;
import io.ocfl.core.extension.OcflExtensionRegistry;
import jakarta.inject.Singleton;

/**
 * Repository Provider for the MyCoRe-Storage-Layout
 * @author Tobias Lenhardt [Hammer1279]
 */
@Singleton
public class MCROCFLMCRRepositoryProvider extends MCROCFLHashRepositoryProvider {

    @Override
    @MCRPostConstruction
    public void init(String prop) throws IOException {
        OcflExtensionRegistry.register(MCRStorageLayoutExtension.EXTENSION_NAME, MCRStorageLayoutExtension.class);
        super.init(prop);
    }

    @Override
    public OcflExtensionConfig getExtensionConfig() {
        return new MCRStorageLayoutConfig();
    }
}
