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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MCRFLDateScamblerTest {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final ZoneId ZONE_ID = TimeZone.getTimeZone("GMT+01:00").toZoneId();

    private static Stream<Arguments> provideDates() {
        List<Arguments> argumentsList = new ArrayList<>();
        argumentsList.add(Arguments.of(toDate(2000, 1, 1, 0, 0), "7588999999"));
        argumentsList.add(Arguments.of(toDate(2000, 12, 1, 10, 12), "2285963279"));
        argumentsList.add(Arguments.of(toDate(2012, 11, 10, 9, 8), "1838967119"));
        argumentsList.add(Arguments.of(toDate(2025, 11, 5, 13, 0), "9192853199"));
        return argumentsList.stream();
    }

    private static Date toDate(int year, int month, int day, int hour, int minute) {
        return Date.from(toInstant(year, month, day, hour, minute));
    }

    @ParameterizedTest
    @MethodSource("provideDates")
    public void formatDate(Date date, String expectedScramble) {

        String scambled = new MCRFLDateScrambler().format(date);
        String unscrambled = unscramble(scambled);

        LOGGER.info("Date: {} => {} => {}", date, scambled, unscrambled);

        assertEquals(expectedScramble, scambled);

    }

    private static Stream<Arguments> provideInstants() {
        List<Arguments> argumentsList = new ArrayList<>();
        argumentsList.add(Arguments.of(toInstant(2000, 1, 1, 0, 0), "7588999999"));
        argumentsList.add(Arguments.of(toInstant(2000, 12, 1, 10, 12), "2285963279"));
        argumentsList.add(Arguments.of(toInstant(2012, 11, 10, 9, 8), "1838967119"));
        argumentsList.add(Arguments.of(toInstant(2025, 11, 5, 13, 0), "9192853199"));
        return argumentsList.stream();
    }

    private static Instant toInstant(int year, int month, int day, int hour, int minute) {
        LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minute);
        return localDateTime.atZone(ZONE_ID).toInstant();
    }

    @ParameterizedTest
    @MethodSource("provideInstants")
    public void formatInstant(Instant instant, String expectedScramble) {

        String scambled = new MCRFLDateScrambler().format(instant);
        String unscrambled = unscramble(scambled);

        LOGGER.info("Instant: {} => {} => {}", instant, scambled, unscrambled);

        assertEquals(expectedScramble, scambled);

    }

    @SuppressWarnings("unused")
    public static String unscramble(String scramble) {

        if (!scramble.matches("[0-9]{10}")) {
            return "Invalid string length";
        }

        String prefix = scramble.substring(0, 5);
        String sssString = scramble.substring(5);

        char[] dddddChars = new char[5];
        dddddChars[4] = prefix.charAt(0);
        dddddChars[2] = prefix.charAt(1);
        dddddChars[1] = prefix.charAt(2);
        dddddChars[3] = prefix.charAt(3);
        dddddChars[0] = prefix.charAt(4);
        String dddddString = new String(dddddChars);

        String decodedDate = "YYYY-MM-DD";
        String decodedTime = "HH:mm:ss";

        try {

            int ddddd = Integer.parseInt(dddddString);

            int yyy = (ddddd - 134) / 366;
            int ddd = ddddd - (yyy * 366);

            int year = 2268 - yyy;
            int dayOfYear = 500 - ddd;

            LocalDate date = LocalDate.ofYearDay(year, dayOfYear);
            decodedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

        } catch (Exception e) {
            decodedDate = "INVALID_DATE";
        }

        try {

            int sss = Integer.parseInt(sssString);
            int totalSeconds = 99999 - sss;

            int hh = totalSeconds / 3600;
            int mm = (totalSeconds % 3600) / 60;
            int ss = totalSeconds % 60;

            if (hh >= 0 && hh < 24 && mm >= 0 && mm < 60 && ss >= 0 && ss < 60) {
                decodedTime = String.format("%02d:%02d:%02d", hh, mm, ss);
            } else {
                decodedTime = "INVALID_TIME";
            }

        } catch (NumberFormatException e) {
            decodedTime = "INVALID_TIME";
        }

        return decodedDate + " " + decodedTime;
    }

}
