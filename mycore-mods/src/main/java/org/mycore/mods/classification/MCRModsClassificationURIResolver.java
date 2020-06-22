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

package org.mycore.mods.classification;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * Resolves a classification in parent style.
 * Uses the URI <code>classification:metadata:0:parents:{{@link MCRCategoryID#toString()}}</code> to resolve the result.
 * If no matching classification is found an &lt;empty /&gt; element is returned.
 * This URIResolver can used by this URI syntax:<br>
 *     <ul>
 *         <li>
 *             mycoremods:/{sourcePath}/{subPath}
 *             <ul>
 *                 <li>sourcePath is either:
 *                 <ul>
 *                  <li>uri
 *                  <ul>
 *                      <li>subPath is {{@literal @}authorityURI}/{{@literal @}valueURI} in URI encoded form</li>
 *                  </ul>
 *                  </li>
 *                  <li>authority
 *                  <ul>
 *                      <li>subPath is {{@literal @}authority}/{text()} in URI encoded form</li>
 *                  </ul>
 *                  </li>
 *                  <li>accessCondition
 *                  <ul>
 *                      <li>subPath is {{@literal @}xlink:href} in URI encoded form</li>
 *                  </ul>
 *                  </li>
 *                  <li>typeOfResource
 *                  <ul>
 *                      <li>subPath is {text()} in URI encoded form</li>
 *                  </ul>
 *                  </li>
 *                 </ul>
 *                 </li>
 *             </ul>
 *         </li>
 *         <li>
 *             examples:
 *             <ul>
 *                 <li>
 *modsclass:/uri/http%3A%2F%2Fwww.example.org%2Fclassifications/http%3A%2F%2Fwww.example.org%2Fpub-type%23Sound
 *                 </li>
 *                 <li>modsclass:/authority/marcrelator/aut</li>
 *                 <li>
 *modsclass:/accessCondition/http%3A%2F%2Fwww.mycore.org%2Fclassifications%2Fmir_licenses%23cc_by-sa_4.0
 *                 </li>
 *                 <li>modsclass:/typeOfResource/sound%20recording</li>
 *             </ul>
 *         </li>
 *     </ul>
 *
 */
public class MCRModsClassificationURIResolver implements URIResolver {

    private static Optional<MCRAuthorityInfo> getAuthorityInfo(String href) {
        final URI uri = URI.create(href);
        //keep any encoded '/' inside a path segment
        final String[] decodedPathSegments = Stream.of(uri.getRawPath().substring(1).split("/"))
            .map(s -> "/" + s) //mask as a path for parsing
            .map(URI::create) //parse
            .map(URI::getPath) //decode
            .map(p -> p.substring(1)) //strip '/' again
            .toArray(String[]::new);
        MCRAuthorityInfo authInfo;
        switch (decodedPathSegments[0]) {
            case "uri":
                authInfo = new MCRAuthorityWithURI(decodedPathSegments[1], decodedPathSegments[2]);
                break;
            case "authority":
                authInfo = new MCRAuthorityAndCode(decodedPathSegments[1], decodedPathSegments[2]);
                break;
            case "accessCondition":
                authInfo = new MCRAccessCondition(decodedPathSegments[1]);
                break;
            case "typeOfResource":
                authInfo = new MCRTypeOfResource(decodedPathSegments[1]);
                break;
            default:
                authInfo = null;
        }
        LogManager.getLogger().debug("authinfo {}", authInfo);
        return Optional.ofNullable(authInfo);
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        final Optional<String> categoryURI = getAuthorityInfo(href)
            .map(MCRAuthorityInfo::getCategoryID)
            .map(category -> String.format(Locale.ROOT, "classification:metadata:0:parents:%s:%s", category.getRootID(),
                category.getID()));
        if (categoryURI.isPresent()) {
            LogManager.getLogger().debug("{} -> {}", href, categoryURI.get());
            return MCRURIResolver.instance().resolve(categoryURI.get(), base);
        }
        LogManager.getLogger().debug("no category found for {}", href);
        return new JDOMSource(new Element("empty"));
    }
}
