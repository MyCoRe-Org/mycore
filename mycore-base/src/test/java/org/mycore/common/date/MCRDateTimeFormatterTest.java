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

package org.mycore.common.date;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MCRDateTimeFormatterTest {

    public static final String DATE_FORMAT_1 = "yyyyMMddHHmmss";

    public static final String DATE_FORMAT_2 = "yyyy-MM-dd'T'HH:mm";

    public static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private static Stream<Arguments> provideDates() {
        List<Arguments> argumentsList = new ArrayList<>();
        argumentsList.add(Arguments.of(toDate(2000, 1, 1, 0, 0), DATE_FORMAT_1, "20000101000000"));
        argumentsList.add(Arguments.of(toDate(2000, 1, 1, 0, 0), DATE_FORMAT_2, "2000-01-01T00:00"));
        argumentsList.add(Arguments.of(toDate(2000, 12, 1, 10, 12), DATE_FORMAT_1, "20001201101200"));
        argumentsList.add(Arguments.of(toDate(2000, 12, 1, 10, 12), DATE_FORMAT_2, "2000-12-01T10:12"));
        argumentsList.add(Arguments.of(toDate(2012, 11, 10, 9, 8), DATE_FORMAT_1, "20121110090800"));
        argumentsList.add(Arguments.of(toDate(2012, 11, 10, 9, 8), DATE_FORMAT_2, "2012-11-10T09:08"));
        argumentsList.add(Arguments.of(toDate(2025, 11, 5, 13, 0), DATE_FORMAT_1, "20251105130000"));
        argumentsList.add(Arguments.of(toDate(2025, 11, 5, 13, 0), DATE_FORMAT_2, "2025-11-05T13:00"));
        return argumentsList.stream();
    }

    private static Date toDate(int year, int month, int day, int hour, int minute) {
        return Date.from(toInstant(year, month, day, hour, minute));
    }

    @ParameterizedTest
    @MethodSource("provideDates")
    public void formatDate(Date date, String format, String expected) {

        String formattedDate = new MCRDateTimeFormatter(format).format(date);

        assertEquals(expected, formattedDate);

    }

    private static Stream<Arguments> provideInstants() {
        List<Arguments> argumentsList = new ArrayList<>();
        argumentsList.add(Arguments.of(toInstant(2000, 1, 1, 0, 0), DATE_FORMAT_1, "20000101000000"));
        argumentsList.add(Arguments.of(toInstant(2000, 1, 1, 0, 0), DATE_FORMAT_2, "2000-01-01T00:00"));
        argumentsList.add(Arguments.of(toInstant(2000, 12, 1, 10, 12), DATE_FORMAT_1, "20001201101200"));
        argumentsList.add(Arguments.of(toInstant(2000, 12, 1, 10, 12), DATE_FORMAT_2, "2000-12-01T10:12"));
        argumentsList.add(Arguments.of(toInstant(2012, 11, 10, 9, 8), DATE_FORMAT_1, "20121110090800"));
        argumentsList.add(Arguments.of(toInstant(2012, 11, 10, 9, 8), DATE_FORMAT_2, "2012-11-10T09:08"));
        argumentsList.add(Arguments.of(toInstant(2025, 11, 5, 13, 0), DATE_FORMAT_1, "20251105130000"));
        argumentsList.add(Arguments.of(toInstant(2025, 11, 5, 13, 0), DATE_FORMAT_2, "2025-11-05T13:00"));
        return argumentsList.stream();
    }

    private static Instant toInstant(int year, int month, int day, int hour, int minute) {
        LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minute);
        return localDateTime.atZone(ZONE_ID).toInstant();
    }

    @ParameterizedTest
    @MethodSource("provideInstants")
    public void formatInstant(Instant instant, String format, String expected) {

        String formattedDate = new MCRDateTimeFormatter(format).format(instant);

        assertEquals(expected, formattedDate);

    }

}
