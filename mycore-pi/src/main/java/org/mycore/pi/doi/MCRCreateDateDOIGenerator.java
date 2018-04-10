package org.mycore.pi.doi;

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

import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierGenerator;
import org.mycore.pi.MCRPersistentIdentifierManager;

public class MCRCreateDateDOIGenerator extends MCRPersistentIdentifierGenerator<MCRDigitalObjectIdentifier> {

    private static final String DATE_PATTERN = "yyyyMMdd-HHmmss";

    private static final String CREATE_DATE_PLACEHOLDER = "$createDate$";

    private static final String DATE_REGEXP = CREATE_DATE_PLACEHOLDER + "-([0-9]+)";

    private static final Map<String, AtomicInteger> PATTERN_COUNT_MAP = new HashMap<>();

    private final MCRDOIParser mcrdoiParser;

    private String prefix = MCRConfiguration.instance().getString("MCR.DOI.Prefix");

    public MCRCreateDateDOIGenerator(String generatorID) {
        super(generatorID);
        mcrdoiParser = new MCRDOIParser();
    }

    @Override
    public MCRDigitalObjectIdentifier generate(MCRObjectID mcrID, String additional) {
        Date createdate = MCRMetadataManager.retrieveMCRObject(mcrID).getService().getDate("createdate");
        if (createdate != null) {
            MCRISO8601Date mcrdate = new MCRISO8601Date();
            mcrdate.setDate(createdate);
            String createDate = mcrdate.format(DATE_PATTERN, Locale.ENGLISH);
            final int count = getCountForCreateDate(createDate);
            Optional<MCRDigitalObjectIdentifier> parse = mcrdoiParser.parse(prefix + "/" + createDate + "-" + count);
            MCRPersistentIdentifier doi = parse.orElseThrow(() -> new MCRException("Error while parsing default doi!"));
            return (MCRDigitalObjectIdentifier) doi;
        } else {
            throw new MCRPersistenceException("The object " + mcrID.toString() + " doesn't have a createdate!");
        }
    }

    private int getCountForCreateDate(String createDate) {
        return getCount(prefix + "/" + DATE_REGEXP.replace(CREATE_DATE_PLACEHOLDER, createDate));
    }

    private synchronized int getCount(final String pattern) {
        AtomicInteger count = PATTERN_COUNT_MAP.computeIfAbsent(pattern, (pattern_) -> {
            Pattern regExpPattern = Pattern.compile(pattern_);
            Predicate<String> matching = regExpPattern.asPredicate();

            List<MCRPIRegistrationInfo> list = MCRPersistentIdentifierManager.getInstance()
                .getList(MCRDigitalObjectIdentifier.TYPE, -1, -1);

            Comparator<Integer> integerComparator = Integer::compareTo;
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
                .max(integerComparator)
                .map(n -> n + 1);
            return new AtomicInteger(highestNumber.orElse(0));
        });

        return count.getAndIncrement();
    }

}
