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

package org.mycore.dedup;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRPropertyList;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Resolves the possible duplicates of an object as XML, for use in stylesheets.
 * <p>
 * Syntax: {@code dedup:duplicates:{objectId}} or {@code dedup:duplicates-for-session:{sessionKey}}
 * <p>
 * It returns the objects that share at least one deduplication key with the given object and that are
 * not marked as no-duplicates of it:
 * <pre>
 * &lt;duplicates for="mir_mods_00000001"&gt;
 *   &lt;duplicate id="mir_mods_00000002"&gt;
 *     &lt;criterion type="identifier" value="doi:10.1000/xyz" /&gt;
 *     &lt;criterion type="title-author" value="meier: a title" /&gt;
 *   &lt;/duplicate&gt;
 * &lt;/duplicates&gt;
 * </pre>
 * <p>
 * For the {@code duplicates-for-session} action the session key must start with one of the configured
 * prefixes ({@code MCR.URIResolver.ModuleResolver.dedup.SessionKeyPrefixes}, comma-separated); keys
 * matching none of them are rejected so that stylesheets can only read the objects an editor explicitly
 * stored for this purpose.
 */
@MCRConfigurationProxy(proxyClass = MCRDeDupURIResolver.Factory.class)
public class MCRDeDupURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ACTION_DUPLICATES = "duplicates";

    private static final String ACTION_DUPLICATES_FOR_SESSION = "duplicates-for-session";

    private final List<String> sessionKeyPrefixes;

    /**
     * @param sessionKeyPrefixes the prefixes a session key may have to be accepted by the
     *     {@code duplicates-for-session} action; keys matching none of them are rejected
     */
    public MCRDeDupURIResolver(List<String> sessionKeyPrefixes) {
        this.sessionKeyPrefixes = List.copyOf(
            Objects.requireNonNull(sessionKeyPrefixes, "sessionKeyPrefixes must not be null"));
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String[] parts = href.split(":", 3);
        if (parts.length != 3) {
            throw new TransformerException("Malformed dedup URI, expected 'dedup:duplicates:{objectId}': " + href);
        }

        String action = parts[1];
        if (ACTION_DUPLICATES.equals(action)) {
            return resolveDuplicates(parts[2], href);
        } else if (ACTION_DUPLICATES_FOR_SESSION.equals(action)) {
            return resolveDuplicatesForSession(parts[2]);
        }

        throw new TransformerException("Unknown dedup action '" + action + "' in " + href);
    }

    private Source resolveDuplicates(String objectIdString, String href) throws TransformerException {
        if (!MCRObjectID.isValid(objectIdString)) {
            throw new TransformerException("Invalid object id '" + objectIdString + "' in " + href);
        }

        MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);
        LOGGER.debug("Resolving possible duplicates of {}", objectId);

        List<MCRPossibleDuplicate> duplicates = MCRDeDupKeyManager.obtainInstance().findDuplicates(objectId);
        return new JDOMSource(buildResult(objectIdString, groupDuplicates(objectIdString, duplicates)));
    }

    private Source resolveDuplicatesForSession(String sessionKey) throws TransformerException {
        if (sessionKeyPrefixes.stream().noneMatch(sessionKey::startsWith)) {
            throw new TransformerException("Session key '" + sessionKey + "' is not allowed, it must start with one "
                + "of " + sessionKeyPrefixes);
        }
        LOGGER.debug("Resolving possible duplicates of session object {}", sessionKey);
        Element element = (Element) MCRSessionMgr.getCurrentSession().get(sessionKey);
        MCRObject object = new MCRObject(new Document(element.clone()));
        Set<MCRDeDupCriterion> criteria = MCRDeDupCriteriaProvider.obtainInstance().getCriteria(object);
        Map<MCRObjectID, Set<MCRDeDupCriterion>> duplicates = MCRDeDupKeyManager.obtainInstance()
            .findDuplicates(criteria);
        Map<String, Set<MCRDeDupCriterion>> byObjectId = duplicates.entrySet().stream()
            .collect(Collectors.toMap(entry -> entry.getKey().toString(), Map.Entry::getValue,
                (left, right) -> left, LinkedHashMap::new));
        return new JDOMSource(buildResult(sessionKey, byObjectId));
    }

    private static Map<String, Set<MCRDeDupCriterion>> groupDuplicates(String objectIdString,
        List<MCRPossibleDuplicate> duplicates) {
        return duplicates.stream()
            .collect(Collectors.groupingBy(duplicate -> otherId(objectIdString, duplicate), LinkedHashMap::new,
                Collectors.mapping(MCRPossibleDuplicate::criterion, Collectors.toCollection(LinkedHashSet::new))));
    }

    private Element buildResult(String forValue, Map<String, ? extends Collection<MCRDeDupCriterion>> duplicates) {
        Element result = new Element("duplicates");
        result.setAttribute("for", forValue);

        duplicates.forEach((otherId, matches) -> {
            Element duplicate = new Element("duplicate");
            duplicate.setAttribute("id", otherId);
            for (MCRDeDupCriterion criterion : matches) {
                duplicate.addContent(new Element("criterion")
                    .setAttribute("type", criterion.type())
                    .setAttribute("value", criterion.value()));
            }
            result.addContent(duplicate);
        });

        return result;
    }

    private static String otherId(String objectIdString, MCRPossibleDuplicate duplicate) {
        return objectIdString.equals(duplicate.objectId1()) ? duplicate.objectId2() : duplicate.objectId1();
    }

    /**
     * Builds a {@link MCRDeDupURIResolver} from the configuration, injecting the session key prefix.
     */
    public static class Factory implements Supplier<MCRDeDupURIResolver> {

        /** Prefixes a session key may have to be accepted by the {@code duplicates-for-session} action. */
        @MCRPropertyList(name = "SessionKeyPrefixes")
        public List<String> sessionKeyPrefixes;

        @Override
        public MCRDeDupURIResolver get() {
            return new MCRDeDupURIResolver(sessionKeyPrefixes);
        }
    }
}
