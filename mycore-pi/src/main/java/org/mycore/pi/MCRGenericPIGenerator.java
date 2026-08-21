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

package org.mycore.pi;

import static org.mycore.pi.util.MCRPIGeneratorUtils.formatCount;
import static org.mycore.pi.util.MCRPIGeneratorUtils.getCountPattern;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRPropertyList;
import org.mycore.common.config.annotation.MCRPropertyMap;
import org.mycore.common.date.MCRDateFormatter;
import org.mycore.common.date.MCRSimpleDateFormatter;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.util.MCRPIGeneratorUtils;

/**
 * {@link MCRGenericPIGenerator} is a {@link MCRPIGenerator} for arbitrary identifiers
 * that generates identifiers using a general pattern and other information dependent on the
 * replacement markers contained in that pattern.
 * <ul>
 *   <li>
 *     The replacement marker {@link MCRGenericPIGenerator#PLACE_HOLDER_CURRENT_DATE}
 *     will be replaced with the current date, formatted by the given date formatter.
 *   </li>
 *   <li>
 *     The replacement marker {@link MCRGenericPIGenerator#PLACE_HOLDER_OBJECT_DATE}
 *     will be replaced with the objects creation date, formatted by the given date formatter.
 *   </li>
 *   <li>
 *     The replacement marker {@link MCRGenericPIGenerator#PLACE_HOLDER_OBJECT_PROJECT}
 *     will be replaced with the object IDs {@link MCRObjectID#getProjectId()}, mapped by the given project mapping.
 *   </li>
 *   <li>
 *     The replacement marker {@link MCRGenericPIGenerator#PLACE_HOLDER_OBJECT_TYPE}
 *     will be replaced with the object IDs {@link MCRObjectID#getTypeId()}, mapped by the given type mapping.
 *   </li>
 *   <li>
 *     The replacement marker {@link MCRGenericPIGenerator#PLACE_HOLDER_OBJECT_NUMBER}
 *     will be replaced with the object IDs {@link MCRObjectID#getNumberAsString()}.
 *   </li>
 *   <li>
 *     The replacement marker of form {@link MCRGenericPIGenerator#XPATH_PATTERN}
 *     will be replaced with the result of evaluating the objects XML representation with
 *     a given xPath, using the <em>n</em>-th given xPath, when the pattern represents the number <em>n</em>.
 *   </li>
 *   <li>
 *     The replacement marker {@link MCRGenericPIGenerator#PLACE_HOLDER_COUNT}
 *     will be replaced with a unique count number with the given count precision.
 *   </li>
 * </ul>
 * <p>
 * Example patterns:
 * <pre><code>
 * urn:nbn:de:gbv:$CurrentDate-$1-$2-$ObjectType-$ObjectProject-$ObjectNumber-$Count-
 * urn:nbn:de:gbv:$ObjectDate-$ObjectType-$Count
 * urn:nbn:de:gbv:$ObjectDate-$Count
 * urn:nbn:de:gbv:$ObjectType-$Count
 * urn:nbn:de:gbv:$0-$1-$Count
 * </code></pre>
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRGenericPIGenerator#GENERAL_PATTERN_KEY} can be used to
 * specify the pattern.
 * <li> The property suffix {@link MCRGenericPIGenerator#DATE_FORMAT_KEY} can be used to
 * specify the date format to be used (unless {@link MCRGenericPIGenerator#DATE_FORMATTER_KEY} is also used;
 * uses {@link MCRSimpleDateFormatter} with {@link Locale#ROOT}).
 * <li> The property suffix {@link MCRGenericPIGenerator#DATE_FORMATTER_KEY} can be used to
 * specify the date formatter to be used (optional, overrides the formatter configured with
 * {@link MCRGenericPIGenerator#DATE_FORMAT_KEY}).
 * <li> The property suffix {@link MCRGenericPIGenerator#OBJECT_PROJECT_MAPPING_KEY} can be used to
 * specify the project ID mappings to be used.
 * <li> The property suffix {@link MCRGenericPIGenerator#OBJECT_TYPE_MAPPING_KEY} can be used to
 * specify the type ID mapping to be used.
 * <li> The property suffix {@link MCRGenericPIGenerator#COUNT_PRECISION_KEY} can be used to
 * specify number of digits to be used for the count (optional, defaults to <code>-1</code>,
 * which uses the natural number of digits).
 * <li> The property suffix {@link MCRGenericPIGenerator#TYPE_KEY} can be used to
 * specify identifier type.
 * <li> The property suffix {@link MCRGenericPIGenerator#X_PATH_KEY} can be used to
 * specify the list of xPaths.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.pi.MCRGenericPIGenerator
 * [...].GeneralPattern=urn:nbn:de:gbv:$CurrentDate-$1-$2-$ObjectType-$ObjectProject-$ObjectNumber-$Count-
 * [...].DateFormatter.Class=org.mycore.common.date.MCRSimpleDateFormatter
 * [...].DateFormatter.Format=yyyy-MM-dd
 * [...].ObjectProjectMapping.mycore=MyCoRe
 * [...].ObjectTypeMapping.mods=MODS
 * [...].CountPrecision=6
 * [...].Type=dnbUrn
 * [...].XPath.1=/mycoreobject/metadata//mods:typeOfResource/text()
 * [...].XPath.2=substring-after(/mycoreobject/metadata//mods:genre/@valueURI,'#')
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRGenericPIGenerator.Factory.class)
public class MCRGenericPIGenerator implements MCRPIGenerator<MCRPersistentIdentifier> {

    public static final String DEFAULT_PROPERTY_PREFIX = "MCR.Default.PI.Generator.Generic.";

    public static final String GENERAL_PATTERN_KEY = "GeneralPattern";

    public static final String DATE_FORMAT_KEY = "DateFormat";

    public static final String DATE_FORMATTER_KEY = "DateFormatter";

    public static final String OBJECT_PROJECT_MAPPING_KEY = "ObjectProjectMapping";

    public static final String OBJECT_TYPE_MAPPING_KEY = "ObjectTypeMapping";

    public static final String COUNT_PRECISION_KEY = "CountPrecision";

    public static final String TYPE_KEY = "Type";

    public static final String X_PATH_KEY = "XPath";

    public static final String PLACE_HOLDER_CURRENT_DATE = "$CurrentDate";

    public static final String PLACE_HOLDER_OBJECT_DATE = "$ObjectDate";

    public static final String PLACE_HOLDER_OBJECT_PROJECT = "$ObjectProject";

    public static final String PLACE_HOLDER_OBJECT_TYPE = "$ObjectType";

    public static final String PLACE_HOLDER_COUNT = "$Count";

    public static final String PLACE_HOLDER_OBJECT_NUMBER = "$ObjectNumber";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Pattern XPATH_PATTERN = Pattern.compile("\\$([0-9]+)", Pattern.DOTALL);

    private final MCRPIGeneratorUtils.Counter counter;

    private final String generalPattern;

    private final MCRDateFormatter dateFormatter;

    private final Map<String, String> projectIdMappings;

    private final Map<String, String> typeIdMappings;

    private final int countPrecision;

    private final String type;

    private final List<String> xPaths;

    public MCRGenericPIGenerator(MCRPIGeneratorUtils.Counter counter, String generalPattern,
        MCRDateFormatter dateFormatter, Map<String, String> projectIdMappings, Map<String, String> typeIdMappings,
        int countPrecision, String type, List<String> xPaths) {
        this.counter = Objects.requireNonNull(counter, "Counter must not be null");
        this.generalPattern = Objects.requireNonNull(generalPattern, "General pattern must not be null");
        this.dateFormatter = Objects.requireNonNull(dateFormatter, "Date formatter must not be null");
        this.projectIdMappings = Objects.requireNonNull(projectIdMappings, "Project ID mappings must not be null");
        this.typeIdMappings = Objects.requireNonNull(typeIdMappings, "Type ID mappings must not be null");
        this.countPrecision = countPrecision;
        this.type = Objects.requireNonNull(type, "Type must not be null");
        this.xPaths = Objects.requireNonNull(xPaths, "XPaths must not be null");
        validateProperties();
    }

    private void validateProperties() {
        if (countPrecision == -1 && MCRDNBURN.TYPE.equals(type)) {
            throw new MCRConfigurationException(
                "Combination of count precision -1 and type 'urn' is not supported!");
        }
    }

    @Override
    public final MCRPersistentIdentifier generate(MCRBase base, String additional)
        throws MCRPersistentIdentifierException {

        String resultingPI = generalPattern;

        if (resultingPI.contains(PLACE_HOLDER_CURRENT_DATE)) {
            resultingPI = resultingPI.replace(PLACE_HOLDER_CURRENT_DATE, dateFormatter.format(new Date()));
        }

        if (resultingPI.contains(PLACE_HOLDER_OBJECT_DATE)) {
            final Date objectCreateDate = base.getService().getDate(MCRObjectService.DATE_TYPE_CREATEDATE);
            if (objectCreateDate == null) {
                throw new MCRPersistentIdentifierException("Object " + base.getId() + " doesn't have a create date!");
            }
            resultingPI = resultingPI.replace(PLACE_HOLDER_OBJECT_DATE, dateFormatter.format(objectCreateDate));
        }

        if (resultingPI.contains(PLACE_HOLDER_OBJECT_PROJECT)) {
            final String projectId = base.getId().getProjectId();
            final String mappedProjectId = projectIdMappings.getOrDefault(projectId, projectId);
            resultingPI = resultingPI.replace(PLACE_HOLDER_OBJECT_PROJECT, mappedProjectId);
        }

        if (resultingPI.contains(PLACE_HOLDER_OBJECT_TYPE)) {
            final String typeId = base.getId().getTypeId();
            final String mappedTypeId = typeIdMappings.getOrDefault(typeId, typeId);
            resultingPI = resultingPI.replace(PLACE_HOLDER_OBJECT_TYPE, mappedTypeId);
        }

        if (resultingPI.contains(PLACE_HOLDER_OBJECT_NUMBER)) {
            resultingPI = resultingPI.replace(PLACE_HOLDER_OBJECT_NUMBER, base.getId().getNumberAsString());
        }

        if (XPATH_PATTERN.asPredicate().test(resultingPI)) {
            resultingPI = XPATH_PATTERN.matcher(resultingPI).replaceAll((mr) -> {
                final String xpathNumberString = mr.group(1);
                final int xpathNumber = Integer.parseInt(xpathNumberString, 10) - 1;
                if (this.xPaths.size() <= xpathNumber || xpathNumber < 0) {
                    throw new MCRException(
                        "The index of " + xpathNumber + " is out of bounds of xpath array (" + xPaths.size() + ")");
                }

                final String xpathString = this.xPaths.get(xpathNumber);
                XPathFactory factory = XPathFactory.instance();
                XPathExpression<Object> expr = factory.compile(xpathString, Filters.fpassthrough(), null,
                    MCRConstants.getStandardNamespaces());
                final Object content = expr.evaluateFirst(base.createXML());

                return switch (content) {
                    case Text text -> text.getTextNormalize();
                    case Attribute attribute -> attribute.getValue();
                    case Element element -> element.getTextNormalize();
                    case null -> "";
                    default -> content.toString();
                };
            });
            LOGGER.info(resultingPI);
        }

        final MCRPIParser<MCRPersistentIdentifier> parser = MCRPIManager.getInstance()
            .getParserForType(type);

        String result;

        result = applyCount(resultingPI);

        if (MCRDNBURN.TYPE.equals(type)) {
            result = result + "C"; // will be replaced by the URN-Parser
        }

        String finalResult = result;
        return parser.parse(finalResult)
            .orElseThrow(() -> new MCRPersistentIdentifierException("Could not parse " + finalResult));

    }

    private String applyCount(String resultingPI) throws MCRPersistentIdentifierException {
        String result;
        if (resultingPI.contains(PLACE_HOLDER_COUNT)) {

            // replace all occurrences of the count placeholder with the count pattern, and quote
            // all other parts, so they are interpreted as literals in the generated pattern
            StringBuilder counterPattern = new StringBuilder();
            String countPattern = getCountPattern(countPrecision);
            int start = 0;
            int index = resultingPI.indexOf(PLACE_HOLDER_COUNT);
            while (index != -1) {
                if (index != start) {
                    counterPattern.append(Pattern.quote(resultingPI.substring(start, index)));
                }
                counterPattern.append(countPattern);
                start = index + PLACE_HOLDER_COUNT.length();
                index = resultingPI.indexOf(PLACE_HOLDER_COUNT, start);
            }
            if (start != resultingPI.length()) {
                counterPattern.append(Pattern.quote(resultingPI.substring(start)));
            }

            if (MCRDNBURN.TYPE.equals(type)) {
                counterPattern.append("[0-9]");
            }

            LOGGER.info("Counter pattern is {}", counterPattern);
            final int count = counter.getCount(type, counterPattern.toString());

            LOGGER.info("Count is {}", count);
            result = resultingPI.replace(PLACE_HOLDER_COUNT, formatCount(count, countPrecision));
        } else {
            result = resultingPI;
        }
        return result;
    }

    public static class Factory implements Supplier<MCRGenericPIGenerator> {

        @MCRProperty(name = GENERAL_PATTERN_KEY)
        public String generalPattern;

        @MCRProperty(name = DATE_FORMAT_KEY, defaultName = DEFAULT_PROPERTY_PREFIX + DATE_FORMAT_KEY)
        public String dateFormat;

        @MCRInstance(name = DATE_FORMATTER_KEY, valueClass = MCRDateFormatter.class, required = false)
        public MCRDateFormatter dateFormatter;

        @MCRPropertyMap(name = OBJECT_PROJECT_MAPPING_KEY, required = false)
        public Map<String, String> projectIdMappings;

        @MCRPropertyMap(name = OBJECT_TYPE_MAPPING_KEY, required = false)
        public Map<String, String> typeIdMappings;

        @MCRProperty(name = COUNT_PRECISION_KEY, defaultName = DEFAULT_PROPERTY_PREFIX + COUNT_PRECISION_KEY)
        public String countPrecision;

        @MCRProperty(name = TYPE_KEY)
        public String type;

        @MCRPropertyList(name = X_PATH_KEY, required = false)
        public List<String> xPaths;

        @Override
        public MCRGenericPIGenerator get() {
            return new MCRGenericPIGenerator(MCRPIGeneratorUtils.SHARED_COUNTER, generalPattern, getDateFormatter(),
                projectIdMappings, typeIdMappings, Integer.parseInt(countPrecision), type, xPaths);
        }

        private MCRDateFormatter getDateFormatter() {
            return dateFormatter != null ? dateFormatter : new MCRSimpleDateFormatter(dateFormat, Locale.ROOT);
        }

    }

}
