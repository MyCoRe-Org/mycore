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

package org.mycore.pi.urn;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.urn.rest.MCRDNBURNRestClient;

import com.google.gson.JsonElement;

public class MCRURNUtils {

    public static Optional<Date> getDNBRegisterDate(MCRPIRegistrationInfo dnburn) {
        try {
            return Optional.of(getDNBRegisterDate(dnburn.getIdentifier()));
        } catch (ParseException e) {
            LogManager.getLogger().warn("Could not parse: " + dnburn.getIdentifier(), e);
        }

        return Optional.empty();
    }

    public static Date getDNBRegisterDate(MCRDNBURN dnburn) throws ParseException {
        return getDNBRegisterDate(dnburn.asString());
    }

    public static Date getDNBRegisterDate(String identifier) throws
        ParseException {

        String date = MCRDNBURNRestClient.getRegistrationInfo(identifier)
                .map(info -> info.get("created"))
                .map(JsonElement::getAsString)
                .orElse(null);

        if (date == null) {
            return null;
        }
        
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.GERMAN).parse(date);
    }

}
