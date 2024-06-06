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

package org.mycore.solr.index.file.tika;

import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.solr.index.file.MCRSolrFileIndexAccumulator;

import com.fasterxml.jackson.core.TreeNode;

/**
 * Accumulates text extracted from files using a remote Tika server. The server URL is configured in the
 * <code>mycore.properties</code> file using the key <code>MCR.Solr.Tika.ServerURL</code>. The value of this key should
 * be a comma-separated list of URLs to Tika servers. The accumulator will use the servers in a round-robin fashion.
 * <br/>
 * The accumulator uses mappers to map the extracted json to Solr fields. The mappers are defined in the
 * properties file using the key <code>MCR.Solr.Tika.Mapper.&lt;key&gt;.Class</code>. The <code>&lt;key&gt;</code> is the
 * key of the extracted json element transformed to lowercase and every not letter and digit replaced with
 * <code>_</code> and the value is the fully qualified class name of the mapper. If no mapper is defined for a key, the
 * default mapper with the key <code>default</code> is used.
 *
 * @author Sebastian Hofmann
 */
public class MCRSolrRemoteTikaAccumulator implements MCRSolrFileIndexAccumulator {

    public static final String TIKA_MAPPER_IGNORED_FILES_PROPERTY = SOLR_CONFIG_PREFIX + "Tika.IgnoredFiles";

    public static final String TIKA_SERVER_PROPERTY = SOLR_CONFIG_PREFIX + "Tika.ServerURL";

    public static final String TIKA_MAPPER_MAX_FILE_SIZE = SOLR_CONFIG_PREFIX + "Tika.MaxFileSize";

    private static final Logger LOGGER = LogManager.getLogger();

    private final Set<MCRTikaHttpClient> tikaClient;

    private final boolean enabled;

    private final Set<Pattern> ignoredFilesPattern;

    private final long maxFileSize;

    private Iterator<MCRTikaHttpClient> tikaClientIterator = null;

    public MCRSolrRemoteTikaAccumulator() {
        Optional<String> serverList = MCRConfiguration2.getString(TIKA_SERVER_PROPERTY);

        ignoredFilesPattern = Collections.synchronizedSet(MCRConfiguration2
            .getOrThrow(TIKA_MAPPER_IGNORED_FILES_PROPERTY,
                MCRConfiguration2::splitValue)
            .map(Pattern::compile)
            .collect(Collectors.toSet()));

        maxFileSize = MCRConfiguration2.getLong(TIKA_MAPPER_MAX_FILE_SIZE).orElseThrow(
            () -> MCRConfiguration2.createConfigurationException(TIKA_MAPPER_MAX_FILE_SIZE));

        Set<MCRTikaHttpClient> tikaClients = null;
        if (serverList.isPresent()) {
            List<String> servers = MCRConfiguration2.splitValue(serverList.get()).toList();
            tikaClients = servers.stream().map(MCRTikaHttpClient::new).collect(Collectors.toSet());
        }
        this.tikaClient = tikaClients;
        enabled = tikaClients != null && !tikaClients.isEmpty();
    }

    @Override
    public void accumulate(SolrInputDocument document, Path filePath, BasicFileAttributes attributes) {
        if (!enabled) {
            return;
        }

        if (filePath instanceof MCRPath mcrPath) {
            Optional<Pattern> matching = ignoredFilesPattern.stream()
                .filter(p -> p.matcher(mcrPath.getOwnerRelativePath()).matches())
                .findAny();
            if (matching.isPresent()) {
                LOGGER.info("File {} is ignored by pattern {}", mcrPath.getOwnerRelativePath(), matching.get());
                return;
            }
        }

        if (attributes.size() > maxFileSize) {
            LOGGER.info("File {} is ignored because it is too large {}", filePath,
                MCRUtils.getSizeFormatted(attributes.size()));
            return;
        }

        MCRTikaHttpClient client = getNextClient();
        long start = System.currentTimeMillis();

        boolean hasError = false;
        String errorMessage = null;

        try (InputStream is = Files.newInputStream(filePath)) {
            LOGGER.debug("Extracting text from {} using Tika", filePath);
            client.extractText(is, (jsonReader -> processJsonResponse(document, filePath, attributes, jsonReader)));
        } catch (Exception e) {
            hasError = true;
            errorMessage = e.getMessage();
        } finally {
            LOGGER.debug("Extracted text from {} using Tika in {}ms", filePath,
                System.currentTimeMillis() - start);
        }

        document.addField("tika_has_error", hasError);
        document.addField("tika_error_message", errorMessage);
    }

    public void processJsonResponse(SolrInputDocument document, Path filePath,
        BasicFileAttributes attributes, TreeNode json) throws MCRTikaMappingException {
        Iterator<String> it = json.fieldNames();
        while (it.hasNext()) {
            String key = it.next();
            String simpleKeyName = MCRTikaMapper.simplifyKeyName(key);
            Optional<MCRTikaMapper> mapper = MCRTikaMapper.getMapper(simpleKeyName);
            if (mapper.isPresent()) {
                mapper.get().map(key, json.get(key), document, filePath, attributes);
            } else {
                MCRTikaMapper defaultMapper = MCRTikaMapper.getDefaultMapper();
                defaultMapper.map(key, json.get(key), document, filePath, attributes);
            }
        }
    }

    public synchronized MCRTikaHttpClient getNextClient() {
        if (tikaClientIterator == null || !tikaClientIterator.hasNext()) {
            tikaClientIterator = tikaClient.iterator();
        }
        return tikaClientIterator.next();
    }

    @Override
    public boolean isEnabled() {
        return MCRSolrFileIndexAccumulator.super.isEnabled() && enabled;
    }
}
