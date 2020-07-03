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

package org.mycore.pi;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 *
 * MCR.PI.Generator.myGenerator=org.mycore.pi.urn.MCRGenericPIGenerator
 *
 * Set a generic pattern.
 *
 * MCR.PI.Generator.myGenerator.GeneralPattern=urn:nbn:de:gbv:$CurrentDate-$1-$2-$ObjectType-$objectProject-$ObjectNumber-$Count-
 * MCR.PI.Generator.myGenerator.GeneralPattern=urn:nbn:de:gbv:$ObjectDate-$ObjectType-$Count
 * MCR.PI.Generator.myGenerator.GeneralPattern=urn:nbn:de:gbv:$ObjectDate-$Count
 * MCR.PI.Generator.myGenerator.GeneralPattern=urn:nbn:de:gbv:$ObjectType-$Count
 * MCR.PI.Generator.myGenerator.GeneralPattern=urn:nbn:de:gbv:$0-$1-$Count
 *
 * Set a optional DateFormat, if not set the ddMMyyyy is just used as value. (SimpleDateFormat)
 *
 * MCR.PI.Generator.myGenerator.DateFormat=ddMMyyyy
 *
 * Set a optional ObjectType mapping, if not set the ObjectType is just used as value
 *
 * MCR.PI.Generator.myGenerator.TypeMapping=document:doc,disshab:diss,Thesis:Thesis,bundle:doc,mods:test
 *
 * You can also map the projectid
 *
 * Set a optional Count precision, if not set or set to -1 the pure number is used (1,2,.., 999).
 * Count always relativ to type and date.
 *
 * MCR.PI.Generator.myGenerator.CountPrecision=3 # will produce 001, 002, ... , 999
 *
 * Set the Type of the generated pi.
 *
 * MCR.PI.Generator.myGenerator.Type=dnbURN
 *
 *
 * Set the Xpaths
 *
 * MCR.PI.Generator.myGenerator.XPath.1=/mycoreobject/metadata/def.shelf/shelf/
 * MCR.PI.Generator.myGenerator.XPath.2=/mycoreobject/metadata/def.path2/path2/
 *
 * @author Sebastian Hofmann
 */
public class MCRGenericPIGenerator extends MCRPIGenerator<MCRPersistentIdentifier> {

    static final String PLACE_HOLDER_CURRENT_DATE = "$CurrentDate";

    static final String PLACE_HOLDER_OBJECT_DATE = "$ObjectDate";

    static final String PLACE_HOLDER_OBJECT_TYPE = "$ObjectType";

    static final String PLACE_HOLDER_OBJECT_PROJECT = "$ObjectProject";

    static final String PLACE_HOLDER_COUNT = "$Count";

    static final String PLACE_HOLDER_OBJECT_NUMBER = "$ObjectNumber";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String PROPERTY_KEY_GENERAL_PATTERN = "GeneralPattern";

    private static final String PROPERTY_KEY_DATE_FORMAT = "DateFormat";

    private static final String PROPERTY_KEY_OBJECT_TYPE_MAPPING = "ObjectTypeMapping";

    private static final String PROPERTY_KEY_OBJECT_PROJECT_MAPPING = "ObjectProjectMapping";

    private static final String PROPERTY_KEY_COUNT_PRECISION = "CountPrecision";

    private static final String PROPERTY_KEY_XPATH = "XPath";

    private static final String PROPERTY_KEY_TYPE = "Type";

    private static final Map<String, AtomicInteger> PATTERN_COUNT_MAP = new HashMap<>();

    private static final Pattern XPATH_PATTERN = Pattern.compile("\\$([0-9]+)", Pattern.DOTALL);

    private String generalPattern;

    private SimpleDateFormat dateFormat;

    private String objectTypeMapping;

    private String objectProjectMapping;

    private int countPrecision;

    private String type;

    private String[] xpath;

    public MCRGenericPIGenerator(String generatorID) {
        super(generatorID);

        final Map<String, String> properties = getProperties();

        setGeneralPattern(properties.get(PROPERTY_KEY_GENERAL_PATTERN));

        setDateFormat(Optional.ofNullable(properties.get(PROPERTY_KEY_DATE_FORMAT))
            .map(format -> new SimpleDateFormat(format, Locale.ROOT))
            .orElse(new SimpleDateFormat("ddMMyyyy", Locale.ROOT)));

        setObjectTypeMapping(properties.get(PROPERTY_KEY_OBJECT_TYPE_MAPPING));
        setObjectProjectMapping(properties.get(PROPERTY_KEY_OBJECT_PROJECT_MAPPING));

        setCountPrecision(Optional.ofNullable(properties.get(PROPERTY_KEY_COUNT_PRECISION))
            .map(Integer::parseInt)
            .orElse(-1));

        setType(properties.get(PROPERTY_KEY_TYPE));

        List<String> xpaths = new ArrayList<>();
        int count = 1;
        while (properties.containsKey(PROPERTY_KEY_XPATH + "." + count)) {
            xpaths.add(properties.get(PROPERTY_KEY_XPATH + "." + count));
            count++;
        }

        setXpath(xpaths.toArray(new String[0]));
        validateProperties();
    }

    // for testing purposes
    MCRGenericPIGenerator(String id, String generalPattern, SimpleDateFormat dateFormat,
        String objectTypeMapping, String objectProjectMapping,
        int countPrecision, String type, String... xpaths) {
        super(id);
        setObjectProjectMapping(objectProjectMapping);
        setGeneralPattern(generalPattern);
        setDateFormat(dateFormat);
        setObjectTypeMapping(objectTypeMapping);
        setCountPrecision(countPrecision);
        setType(type);
        validateProperties();
        setXpath(xpaths);
    }

    private void setXpath(String... xpaths) {
        this.xpath = xpaths;
    }

    private void validateProperties() {
        if (countPrecision == -1 && "dnbUrn".equals(getType())) {
            throw new MCRConfigurationException(
                PROPERTY_KEY_COUNT_PRECISION + "=-1 and " + PROPERTY_KEY_TYPE + "=urn is not supported!");
        }
    }

    @Override
    public MCRPersistentIdentifier generate(MCRBase mcrBase, String additional)
        throws MCRPersistentIdentifierException {

        String resultingPI = getGeneralPattern();

        if (resultingPI.contains(PLACE_HOLDER_CURRENT_DATE)) {
            resultingPI = resultingPI.replace(PLACE_HOLDER_CURRENT_DATE, getDateFormat().format(new Date()));
        }

        if (resultingPI.contains(PLACE_HOLDER_OBJECT_DATE)) {
            final Date objectCreateDate = mcrBase.getService().getDate(MCRObjectService.DATE_TYPE_CREATEDATE);
            resultingPI = resultingPI.replace(PLACE_HOLDER_OBJECT_DATE, getDateFormat().format(objectCreateDate));
        }

        if (resultingPI.contains(PLACE_HOLDER_OBJECT_TYPE)) {
            final String mappedObjectType = getMappedType(mcrBase.getId());
            resultingPI = resultingPI.replace(PLACE_HOLDER_OBJECT_TYPE, mappedObjectType);
        }

        if (resultingPI.contains(PLACE_HOLDER_OBJECT_PROJECT)) {
            final String mappedObjectProject = getMappedProject(mcrBase.getId());
            resultingPI = resultingPI.replace(PLACE_HOLDER_OBJECT_PROJECT, mappedObjectProject);
        }

        if (resultingPI.contains(PLACE_HOLDER_OBJECT_NUMBER)) {
            resultingPI = resultingPI.replace(PLACE_HOLDER_OBJECT_NUMBER, mcrBase.getId().getNumberAsString());
        }

        if (XPATH_PATTERN.asPredicate().test(resultingPI)) {
            resultingPI = XPATH_PATTERN.matcher(resultingPI).replaceAll((mr) -> {
                final String xpathNumberString = mr.group(1);
                final int xpathNumber = Integer.parseInt(xpathNumberString, 10) - 1;
                if (this.xpath.length <= xpathNumber || xpathNumber < 0) {
                    throw new MCRException(
                        "The index of " + xpathNumber + " is out of bounds of xpath array (" + xpath.length + ")");
                }

                final String xpathString = this.xpath[xpathNumber];
                XPathFactory factory = XPathFactory.instance();
                XPathExpression<Object> expr = factory.compile(xpathString, Filters.fpassthrough(), null,
                    MCRConstants.getStandardNamespaces());
                final Object content = expr.evaluateFirst(mcrBase.createXML());

                if (content instanceof Text) {
                    return ((Text) content).getTextNormalize();
                } else if (content instanceof Attribute) {
                    return ((Attribute) content).getValue();
                } else if (content instanceof Element) {
                    return ((Element) content).getTextNormalize();
                } else {
                    return content.toString();
                }
            });
            System.out.println(resultingPI);
        }

        final MCRPIParser<MCRPersistentIdentifier> parser = MCRPIManager.getInstance()
            .getParserForType(getType());

        String result;

        result = applyCount(resultingPI);

        if (getType().equals("dnbUrn")) {
            result = result + "C"; // will be replaced by the URN-Parser
        }

        String finalResult = result;
        return parser.parse(finalResult)
            .orElseThrow(() -> new MCRPersistentIdentifierException("Could not parse " + finalResult));

    }

    private String applyCount(String resultingPI) {
        String result;
        if (resultingPI.contains(PLACE_HOLDER_COUNT)) {
            final int countPrecision = getCountPrecision();
            String regexpStr;

            if (countPrecision == -1) {
                regexpStr = "([0-9]+)";
            } else {
                regexpStr = "("
                    + IntStream.range(0, countPrecision).mapToObj((i) -> "[0-9]").collect(Collectors.joining(""))
                    + ")";
            }

            String counterPattern = resultingPI.replace(PLACE_HOLDER_COUNT, regexpStr);
            if (getType().equals("dnbUrn")) {
                counterPattern = counterPattern + "[0-9]";
            }

            LOGGER.info("Counter pattern is {}", counterPattern);

            final int count = getCount(counterPattern);
            LOGGER.info("Count is {}", count);
            final String pattern = IntStream.range(0, Math.abs(countPrecision)).mapToObj((i) -> "0")
                .collect(Collectors.joining(""));
            DecimalFormat decimalFormat = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.ROOT));
            final String countAsString = countPrecision != -1 ? decimalFormat.format(count) : String.valueOf(count);
            result = resultingPI.replace(PLACE_HOLDER_COUNT, countAsString);
        } else {
            result = resultingPI;
        }
        return result;
    }

    private String getMappedType(MCRObjectID id) {
        String mapping = getObjectTypeMapping();
        String typeID = id.getTypeId();

        return Optional.ofNullable(mapping)
            .map(mappingStr -> mappingStr.split(","))
            .map(Arrays::asList)
            .filter(o -> o.get(0).equals(typeID))
            .map(o -> o.get(1))
            .orElse(typeID);
    }

    private String getMappedProject(MCRObjectID id) {
        String mapping = getObjectProjectMapping();
        String projectID = id.getProjectId();

        return Optional.ofNullable(mapping)
            .map(mappingStr -> mappingStr.split(","))
            .map(Arrays::asList)
            .filter(o -> o.get(0).equals(projectID))
            .map(o -> o.get(1))
            .orElse(projectID);
    }

    protected AtomicInteger readCountFromDatabase(String countPattern) {
        Pattern regExpPattern = Pattern.compile(countPattern);
        Predicate<String> matching = regExpPattern.asPredicate();

        List<MCRPIRegistrationInfo> list = MCRPIManager.getInstance()
            .getList(getType(), -1, -1);

        // extract the number of the PI
        Optional<Integer> highestNumber = list.stream()
            .map(MCRPIRegistrationInfo::getIdentifier)
            .filter(matching)
            .map(pi -> {
                // extract the number of the PI
                Matcher matcher = regExpPattern.matcher(pi);
                if (matcher.find() && matcher.groupCount() == 1) {
                    String group = matcher.group(1);
                    return Integer.parseInt(group, 10);
                } else {
                    return null;
                }
            }).filter(Objects::nonNull)
            .min(Comparator.reverseOrder())
            .map(n -> n + 1);
        return new AtomicInteger(highestNumber.orElse(0));
    }

    private String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGeneralPattern() {
        return generalPattern;
    }

    public void setGeneralPattern(String generalPattern) {
        this.generalPattern = generalPattern;
    }

    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getObjectTypeMapping() {
        return objectTypeMapping;
    }

    public void setObjectTypeMapping(String typeMapping) {
        this.objectTypeMapping = typeMapping;
    }

    public int getCountPrecision() {
        return countPrecision;
    }

    public void setCountPrecision(int countPrecision) {
        this.countPrecision = countPrecision;
    }

    /**
     * Gets the count for a specific pattern and increase the internal counter. If there is no internal counter it will
     * look into the Database and detect the highest count with the pattern.
     *
     * @param pattern a reg exp pattern which will be used to detect the highest count. The first group is the count.
     *                e.G. [0-9]+-mods-2017-([0-9][0-9][0-9][0-9])-[0-9] will match 31-mods-2017-0003-3 and the returned
     *                count will be 4 (3+1).
     * @return the next count
     */
    public final synchronized int getCount(String pattern) {
        AtomicInteger count = PATTERN_COUNT_MAP
            .computeIfAbsent(pattern, this::readCountFromDatabase);

        return count.getAndIncrement();
    }

    public String getObjectProjectMapping() {
        return objectProjectMapping;
    }

    public void setObjectProjectMapping(String objectProjectMapping) {
        this.objectProjectMapping = objectProjectMapping;
    }
}
