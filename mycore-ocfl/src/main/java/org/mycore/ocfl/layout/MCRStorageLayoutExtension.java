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

package org.mycore.ocfl.layout;

import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;

import edu.wisc.library.ocfl.api.exception.OcflExtensionException;
import edu.wisc.library.ocfl.core.extension.OcflExtensionConfig;
import edu.wisc.library.ocfl.core.extension.storage.layout.OcflStorageLayoutExtension;

public class MCRStorageLayoutExtension implements OcflStorageLayoutExtension {

    public static final String EXTENSION_NAME = "mycore-storage-layout";

    private MCRStorageLayoutConfig config;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExtensionName() {
        return EXTENSION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "OCFL object identifiers are separated by their parts, " +
            "the namespace gets used for defining the type, the ID Parts calculated from the " +
            "MCR SlotLayout get used in nesting the paths under the OCFL Storage root.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void init(OcflExtensionConfig config) {
        this.config = (MCRStorageLayoutConfig) config;
    }

    @Override
    public Class<? extends OcflExtensionConfig> getExtensionConfigClass() {
        return MCRStorageLayoutConfig.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String mapObjectId(String objectId) {
        if (config == null) {
            throw new OcflExtensionException("Extension must be initialized before usage!");
        }
        StringBuilder builder = new StringBuilder();
        String type = objectId.substring(0, objectId.indexOf(':') + 1);
        builder.append(type.substring(0, type.length() - 1)).append('/');
        switch (type) {
            case MCROCFLObjectIDPrefixHelper.MCROBJECT, MCROCFLObjectIDPrefixHelper.MCRDERIVATE -> {
                String mcrid = objectId.replaceAll(".*:", "");
                String[] idParts = mcrid.split("_");
                builder.append(idParts[0]).append('/').append(idParts[1]).append('/');
                String id = idParts[2];
                String[] layers = config.getSlotLayout().split("-");
                int position = 0;
                for (int i = 0; i < layers.length; i++) {
                    if (i == layers.length - 1) {
                        break;
                    }
                    int layerNum = Integer.parseInt(layers[i]);
                    if (layerNum <= 0) {
                        continue;
                    }
                    String layerId = id.substring(position, position + layerNum);
                    builder.append(layerId).append('/');
                    position += layerNum;
                }
                builder.append(mcrid);
                return builder.toString();
            }

            // add more switch cases for own type behaviour
            default -> {
                return type.substring(0, type.length() - 1) + "/" + objectId.replaceAll(".*:", "");
            }
        }
    }
}
