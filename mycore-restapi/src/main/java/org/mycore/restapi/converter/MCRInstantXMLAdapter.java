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

package org.mycore.restapi.converter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class MCRInstantXMLAdapter extends XmlAdapter<String, Instant> {

    @Override
    public Instant unmarshal(String v) throws Exception {
        return Instant
            .from(DateTimeFormatter.ISO_INSTANT.parseBest(v,
                ZonedDateTime::from,
                LocalDateTime::from,
                LocalDate::from,
                YearMonth::from,
                Year::from));
    }

    @Override
    public String marshal(Instant v) throws Exception {
        return v.toString();
    }
}
