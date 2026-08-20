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

package org.mycore.pi.purl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * {@link MCRIDPURLGenerator} is a {@link MCRPIGenerator} for {@link MCRPURL} identifiers
 * that generates identifiers using a template and the {@link MCRObjectID} of the {@link MCRBase}.
 * <p>
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRIDPURLGenerator#BASE_URL_TEMPLATE_KEY} can be used to
 * specify the template (must contain the replacement marker <code>$ID</code>).
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.pi.purl.MCRIDPURLGenerator
 * [...].BaseURLTemplate=https://purl.example.com/$ID
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRIDPURLGenerator.Factory.class)
public class MCRIDPURLGenerator implements MCRPIGenerator<MCRPURL> {

    public static final String BASE_URL_TEMPLATE_KEY = "BaseURLTemplate";

    private final String baseUrlTemplate;

    public MCRIDPURLGenerator(String baseUrlTemplate) {
        Objects.requireNonNull(baseUrlTemplate, "Base URL template must not be null");
        this.baseUrlTemplate = checkBaseUrlTemplate(baseUrlTemplate);
    }

    private static String checkBaseUrlTemplate(String baseUrlTemplate) {
        if (!baseUrlTemplate.contains("$ID")) {
            throw new IllegalArgumentException("Base URL template doesn't contain replacement marker $ID: "
                + baseUrlTemplate);
        }
        return baseUrlTemplate;
    }

    @Override
    public MCRPURL generate(MCRBase base, String additional) throws MCRPersistentIdentifierException {

        String id = base.getId().toString();
        String baseUrl = baseUrlTemplate.replaceAll(Pattern.quote("$ID"), id);

        try {
            return new MCRPURL(new URI(baseUrl).toURL());
        } catch (MalformedURLException | URISyntaxException e) {
            throw new MCRPersistentIdentifierException("Error while creating base URL for " + id + ": " + baseUrl, e);
        }

    }

    public static class Factory implements Supplier<MCRIDPURLGenerator> {

        @MCRProperty(name = BASE_URL_TEMPLATE_KEY)
        public String baseUrlTemplate;

        @Override
        public MCRIDPURLGenerator get() {
            return new MCRIDPURLGenerator(baseUrlTemplate);
        }

    }

}
