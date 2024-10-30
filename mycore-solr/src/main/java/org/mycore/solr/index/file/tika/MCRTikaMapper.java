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

package org.mycore.solr.index.file.tika;

import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Optional;

import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.config.MCRConfiguration2;

import com.fasterxml.jackson.core.TreeNode;

/**
 * Interface for mapping Tika extracted metadata to Solr fields.
 *
 * @author Sebastian Hofmann
 */
public interface MCRTikaMapper {

    /**
     * Simplifies the given key for use as a Solr field name.
     * It converts the key to lower case and replaces every character that is not a letter or a digit with an underscore
     * @param key The key to simplify
     * @return The simplified key
     */
    static String simplifyKeyName(String key) {
        // replace every character that is not a letter or a digit with an underscore
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "_");
    }

    /**
     * Returns the default Tika mapper. This mapper is used if no mapper is defined for a key.
     * @return The default Tika mapper
     */
    static MCRTikaMapper getDefaultMapper() {
        return MCRConfiguration2.getInstanceOfOrThrow(MCRTikaMapper.class,
            SOLR_CONFIG_PREFIX + "Tika.Mapper.Default.Class");
    }

    /**
     * Returns the Tika mapper for the given key. If no mapper is defined for the key, the default mapper is returned.
     * @param key The key for which the mapper should be returned
     * @return The Tika mapper for the given key
     */
    static Optional<MCRTikaMapper> getMapper(String key) {
        return MCRConfiguration2.getInstanceOf(MCRTikaMapper.class,
            SOLR_CONFIG_PREFIX + "Tika.Mapper." + key + ".Class");
    }

    /**
     * Maps the given JSON element from a Tika Response to the given Solr document.
     * @param key The key of the JSON element
     * @param element The JSON element
     * @param document The Solr document to which the metadata should be added
     * @param filePath The path of the file which was processed by Tika
     * @param attributes The attributes of the file which was processed by Tika
     * @throws MCRTikaMappingException If an error occurs during mapping
     */
    void map(String key, TreeNode element, SolrInputDocument document, Path filePath,
        BasicFileAttributes attributes) throws MCRTikaMappingException;

}
