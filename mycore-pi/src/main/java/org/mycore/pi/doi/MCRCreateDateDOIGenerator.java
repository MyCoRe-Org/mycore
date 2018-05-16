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
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPersistentIdentifier;

public class MCRCreateDateDOIGenerator extends MCRPIGenerator<MCRDigitalObjectIdentifier> {

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
    public MCRDigitalObjectIdentifier generate(MCRBase mcrObj, String additional)
        throws MCRPersistentIdentifierException {
        Date createdate = mcrObj.getService().getDate("createdate");

        if (createdate != null) {
            MCRISO8601Date mcrdate = new MCRISO8601Date();
            mcrdate.setDate(createdate);
            String createDate = mcrdate.format(DATE_PATTERN, Locale.ENGLISH);
            final int count = getCountForCreateDate(createDate);
            Optional<MCRDigitalObjectIdentifier> parse = mcrdoiParser.parse(prefix + "/" + createDate + "-" + count);
            MCRPersistentIdentifier doi = parse.orElseThrow(() -> new MCRException("Error while parsing default doi!"));
            return (MCRDigitalObjectIdentifier) doi;
        } else {
            throw new MCRPersistenceException("The object " + mcrObj.getId() + " doesn't have a createdate!");
        }
    }

    private int getCountForCreateDate(String createDate) {
        return getCount(prefix + "/" + DATE_REGEXP.replace(CREATE_DATE_PLACEHOLDER, createDate));
    }

    private synchronized int getCount(final String pattern) {
        AtomicInteger count = PATTERN_COUNT_MAP.computeIfAbsent(pattern, (pattern_) -> {
            Pattern regExpPattern = Pattern.compile(pattern_);
            Predicate<String> matching = regExpPattern.asPredicate();

            List<MCRPIRegistrationInfo> list = MCRPIManager.getInstance()
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
