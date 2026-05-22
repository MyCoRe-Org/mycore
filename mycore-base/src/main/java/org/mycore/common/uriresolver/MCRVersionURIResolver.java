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

package org.mycore.common.uriresolver;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRCoreVersion;

/**
 * {@link URIResolver} that returns version information about the running MyCoRe instance as XML.
 */
public class MCRVersionURIResolver implements URIResolver {

    /**
     * Resolves the requested version information and returns it as an XML source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{versionType}
     * </pre>
     * <p>Supported version types:
     * <ul>
     *   <li>{@code gitDescribe} – full Git describe string</li>
     *   <li>{@code abbrev} – abbreviated commit hash</li>
     *   <li>{@code branch} – current branch name</li>
     *   <li>{@code version} – Maven version string</li>
     *   <li>{@code revision} – full commit hash</li>
     *   <li>{@code completeVersion} – combined version string (default)</li>
     * </ul>
     * <p>Example request:
     * <pre>
     *   version:completeVersion
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <version>MyCoRe 2022.06.3-SNAPSHOT 2022.06.x:v2022.06.2-1-g881e24d</version>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping the {@code <version>} element
     * @throws IllegalArgumentException if the version type is not one of the supported values
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String versionType = href.substring(href.indexOf(':') + 1);
        final Element versionElement = new Element("version");
        versionElement.setText(
            switch (versionType) {
                case "gitDescribe" -> MCRCoreVersion.getGitDescribe();
                case "abbrev" -> MCRCoreVersion.getAbbrev();
                case "branch" -> MCRCoreVersion.getBranch();
                case "version" -> MCRCoreVersion.getVersion();
                case "completeVersion" -> MCRCoreVersion.getCompleteVersion();
                case "revision" -> MCRCoreVersion.getRevision();
                default -> throw new IllegalArgumentException(
                    "Invalid parameter for MCRVersionResolver: " + versionType);

            });
        return new JDOMSource(versionElement);
    }

}
