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

import static java.time.format.FormatStyle.LONG;
import static java.time.format.FormatStyle.SHORT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MCRDateStylerTest {

    public static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private static Stream<Arguments> provideDates() {
        List<Arguments> argumentsList = new ArrayList<>();
        argumentsList.add(Arguments.of(toDate(2000, 1, 1, 0, 0), SHORT, "01.01.00"));
        argumentsList.add(Arguments.of(toDate(2000, 1, 1, 0, 0), LONG, "1. Januar 2000"));
        argumentsList.add(Arguments.of(toDate(2000, 12, 1, 10, 12), SHORT, "01.12.00"));
        argumentsList.add(Arguments.of(toDate(2000, 12, 1, 10, 12), LONG, "1. Dezember 2000"));
        argumentsList.add(Arguments.of(toDate(2012, 11, 10, 9, 8), SHORT, "10.11.12"));
        argumentsList.add(Arguments.of(toDate(2012, 11, 10, 9, 8), LONG, "10. November 2012"));
        argumentsList.add(Arguments.of(toDate(2025, 11, 5, 13, 0), SHORT, "05.11.25"));
        argumentsList.add(Arguments.of(toDate(2025, 11, 5, 13, 0), LONG, "5. November 2025"));
        return argumentsList.stream();
    }

    private static Date toDate(int year, int month, int day, int hour, int minute) {
        return Date.from(toInstant(year, month, day, hour, minute));
    }

    @ParameterizedTest
    @MethodSource("provideDates")
    public void formatDate(Date date, FormatStyle dateFormat, String expected) {

        String formattedDate = new MCRDateStyler(dateFormat, Locale.GERMAN).format(date);

        assertEquals(expected, formattedDate);

    }

    private static Stream<Arguments> provideInstants() {
        List<Arguments> argumentsList = new ArrayList<>();
        argumentsList.add(Arguments.of(toInstant(2000, 1, 1, 0, 0), SHORT, "01.01.00"));
        argumentsList.add(Arguments.of(toInstant(2000, 1, 1, 0, 0), LONG, "1. Januar 2000"));
        argumentsList.add(Arguments.of(toInstant(2000, 12, 1, 10, 12), SHORT, "01.12.00"));
        argumentsList.add(Arguments.of(toInstant(2000, 12, 1, 10, 12), LONG, "1. Dezember 2000"));
        argumentsList.add(Arguments.of(toInstant(2012, 11, 10, 9, 8), SHORT, "10.11.12"));
        argumentsList.add(Arguments.of(toInstant(2012, 11, 10, 9, 8), LONG, "10. November 2012"));
        argumentsList.add(Arguments.of(toInstant(2025, 11, 5, 13, 0), SHORT, "05.11.25"));
        argumentsList.add(Arguments.of(toInstant(2025, 11, 5, 13, 0), LONG, "5. November 2025"));
        return argumentsList.stream();
    }

    private static Instant toInstant(int year, int month, int day, int hour, int minute) {
        LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minute);
        return localDateTime.atZone(ZONE_ID).toInstant();
    }

    @ParameterizedTest
    @MethodSource("provideInstants")
    public void formatInstant(Instant instant, FormatStyle dateFormat, String expected) {

        String formattedDate = new MCRDateStyler(dateFormat, Locale.GERMAN).format(instant);

        assertEquals(expected, formattedDate);

    }

}
