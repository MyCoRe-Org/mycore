package org.mycore.pi;

import java.text.SimpleDateFormat;
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
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIParser;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 *
 * MCR.PI.Generator.myGenerator=org.mycore.pi.urn.MCRGenericPIGenerator
 *
 * Set a generic pattern. You can also just use one of them $Count
 *
 * MCR.PI.Generator.myGenerator.GeneralPattern=gbv:$CurrentDate-$ObjectType-$Count
 * MCR.PI.Generator.myGenerator.GeneralPattern=gbv:$ObjectDate-$ObjectType-$Count
 * MCR.PI.Generator.myGenerator.GeneralPattern=gbv:$ObjectDate-$Count
 * MCR.PI.Generator.myGenerator.GeneralPattern=gbv:$ObjectType-$Count
 *
 * Set a optional DateFormat, if not set the ddMMyyyy is just used as value. (SimpleDateFormat)
 *
 * MCR.PI.Generator.myGenerator.DateFormat=ddMMyyyy
 *
 * Set a optional ObjectType mapping, if not set the ObjectType is just used as value
 *
 * MCR.PI.Generator.myGenerator.TypeMapping=document:doc,disshab:diss,Thesis:Thesis,bundle:doc,mods:test
 *
 * Set a optional Count precision, if not set or set to -1 the pure number is used (1,2,.., 999). Count always relativ to type and date.
 *
 * MCR.PI.Generator.myGenerator.CountPrecision=3 # will produce 001, 002, ... , 999
 *
 * Set the Type of the generated pi.
 *
 * MCR.PI.Generator.myGenerator.Type=urn
 *
 *
 * @author Sebastian Hofmann
 */
public class MCRGenericPIGenerator extends MCRPIGenerator<MCRPersistentIdentifier> {

    static final String PLACE_HOLDER_CURRENT_DATE = "$CurrentDate";

    static final String PLACE_HOLDER_OBJECT_DATE = "$ObjectDate";

    static final String PLACE_HOLDER_OBJECT_TYPE = "$ObjectType";

    static final String PLACE_HOLDER_COUNT = "$Count";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String PROPERTY_KEY_GENERAL_PATTERN = "GeneralPattern";

    private static final String PROPERTY_KEY_DATE_FORMAT = "DateFormat";

    private static final String PROPERTY_KEY_OBJECT_TYPE_MAPPING = "ObjectTypeMapping";

    private static final String PROPERTY_KEY_COUNT_PRECISION = "CountPrecision";

    private static final String PROPERTY_KEY_TYPE = "Type";

    private static final Map<String, AtomicInteger> PATTERN_COUNT_MAP = new HashMap<>();

    private String generalPattern;

    private SimpleDateFormat dateFormat;

    private String objectTypeMapping;

    private int countPrecision;

    private String type;

    public MCRGenericPIGenerator(String generatorID) {
        super(generatorID);

        final Map<String, String> properties = getProperties();

        setGeneralPattern(properties.get(PROPERTY_KEY_GENERAL_PATTERN));

        setDateFormat(Optional.ofNullable(properties.get(PROPERTY_KEY_DATE_FORMAT))
            .map(format -> new SimpleDateFormat(format, Locale.ROOT))
            .orElse(new SimpleDateFormat("ddMMyyyy")));

        setObjectTypeMapping(properties.get(PROPERTY_KEY_OBJECT_TYPE_MAPPING));

        setCountPrecision(Optional.ofNullable(properties.get(PROPERTY_KEY_COUNT_PRECISION))
            .map(Integer::parseInt)
            .orElse(-1));

        setType(properties.get(PROPERTY_KEY_TYPE));
        validateProperties();
    }

    // for testing purposes
    MCRGenericPIGenerator(String id, String generalPattern, SimpleDateFormat dateFormat,
        String objectTypeMapping,
        int countPrecision, String type) {
        super(id);
        setGeneralPattern(generalPattern);
        setDateFormat(dateFormat);
        setObjectTypeMapping(objectTypeMapping);
        setCountPrecision(countPrecision);
        setType(type);
        validateProperties();
    }

    private void validateProperties() {
        if (countPrecision == -1 && "urn".equals(getType())) {
            throw new MCRConfigurationException(
                PROPERTY_KEY_COUNT_PRECISION + "=-1 and " + PROPERTY_KEY_TYPE + "=urn is not supported!");
        }
    }

    @Override
    public MCRPersistentIdentifier generate(MCRBase mcrBase, String additional)
        throws MCRPersistentIdentifierException {

        String resultingPI = getGeneralPattern();

        if (resultingPI.contains(PLACE_HOLDER_CURRENT_DATE)) {
            resultingPI = resultingPI.replaceAll(PLACE_HOLDER_CURRENT_DATE, getDateFormat().format(new Date()));
        }

        if (resultingPI.contains(PLACE_HOLDER_OBJECT_DATE)) {
            final Date objectCreateDate = mcrBase.getService().getDate(MCRObjectService.DATE_TYPE_CREATEDATE);
            resultingPI = resultingPI.replaceAll(PLACE_HOLDER_OBJECT_DATE, getDateFormat().format(objectCreateDate));
        }

        if (resultingPI.contains(PLACE_HOLDER_OBJECT_TYPE)) {
            final String mappedObjectType = getMappedType(mcrBase.getId());
            resultingPI = resultingPI.replaceAll(PLACE_HOLDER_OBJECT_TYPE, mappedObjectType);
        }

        final MCRPIParser<MCRPersistentIdentifier> parser = MCRPIManager.getInstance()
            .getParserForType(getType());

        final String result;

        if (resultingPI.contains(PLACE_HOLDER_COUNT)) {
            final int countPrecision = getCountPrecision();
            String regexpStr;

            if (countPrecision == -1) {
                regexpStr = "[0-9]+";
            } else {
                regexpStr = IntStream.range(0, countPrecision).mapToObj((i) -> "[0-9]").collect(Collectors.joining(""));
            }

            String counterPattern = resultingPI.replaceAll(PLACE_HOLDER_COUNT, regexpStr);
            final int count = getCount(counterPattern);
            result = resultingPI.replaceAll(PLACE_HOLDER_COUNT, String.valueOf(count));
        } else {
            result = resultingPI;
        }
        return parser.parse(resultingPI)
            .orElseThrow(() -> new MCRPersistentIdentifierException("Could not parse " + result));

    }

    private String getMappedType(MCRObjectID id) {
        String mapping = getObjectTypeMapping();

        Map<String, String> typeMap = Stream.of(mapping.split(","))
            .collect(Collectors
                .toMap((mappingPart) -> mappingPart.split(":")[0], (mappingPart) -> mappingPart.split(":")[1]));

        String typeID = id.getTypeId();
        return typeMap.getOrDefault(typeID, typeID);
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
}
