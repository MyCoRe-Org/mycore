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

package org.mycore.common.content.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

class Range {

    public long start;

    public long end;

    public long length;

    public static final String RANGE_UNIT = "bytes";

    public boolean validate() {
        if (end >= length) {
            //set 'end' to content size
            end = length - 1;
        }
        return start >= 0 && end >= 0 && start <= end && length > 0;
    }

    public static List<Range> parseRanges(final String rangeHeader, long fileLength) throws IllegalArgumentException {
        if (rangeHeader == null) {
            return null;
        }

        // We operate on byte level only
        if (!rangeHeader.startsWith(RANGE_UNIT)) {
            throw new IllegalArgumentException();
        }

        String rangeValue = rangeHeader.substring(RANGE_UNIT.length() + 1);

        final ArrayList<Range> result = new ArrayList<>();
        final StringTokenizer commaTokenizer = new StringTokenizer(rangeValue, ",");

        // Parsing the range list
        long lastByte = 0;
        while (commaTokenizer.hasMoreTokens()) {
            final String rangeDefinition = commaTokenizer.nextToken().trim();

            final Range currentRange = new Range();
            currentRange.length = fileLength;

            final int dashPos = rangeDefinition.indexOf('-');

            if (dashPos == -1) {
                throw new IllegalArgumentException();
            }

            if (dashPos == 0) {

                try {
                    //offset is negative
                    final long offset = Long.parseLong(rangeDefinition);
                    currentRange.start = fileLength + offset;
                    currentRange.end = fileLength - 1;
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException(e);
                }

            } else {

                try {
                    currentRange.start = Long.parseLong(rangeDefinition.substring(0, dashPos));
                    if (dashPos < rangeDefinition.length() - 1) {
                        currentRange.end = Long.parseLong(rangeDefinition.substring(dashPos + 1,
                            rangeDefinition.length()));
                    } else {
                        currentRange.end = fileLength - 1;
                    }
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException(e);
                }

            }

            if (!currentRange.validate() || lastByte > currentRange.start) {
                throw new IllegalArgumentException();
            }
            lastByte = currentRange.end;
            result.add(currentRange);
        }
        return result;
    }
}
