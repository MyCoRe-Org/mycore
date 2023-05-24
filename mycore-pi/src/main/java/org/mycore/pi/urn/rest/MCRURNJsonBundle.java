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

package org.mycore.pi.urn.rest;

import java.net.URL;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.mycore.pi.MCRPIRegistrationInfo;

/**
 * Class wraps the urn/url in json as needed by the dnb urn service api. Supersedes the now deprecated class
 * {@link MCREpicurLite}.
 *
 * @see <a href="https://wiki.dnb.de/display/URNSERVDOK/URN-Service+API">URN-Service API</a>
 *
 * @author shermann
 * */
public class MCRURNJsonBundle {

    protected enum Format {
        register, update
    }

    private final MCRPIRegistrationInfo urn;

    private final URL url;

    private MCRURNJsonBundle(MCRPIRegistrationInfo urn, URL url) {
        this.urn = urn;
        this.url = url;
    }

    public static MCRURNJsonBundle instance(MCRPIRegistrationInfo urn, URL url) {
        return new MCRURNJsonBundle(urn, url);
    }

    public MCRPIRegistrationInfo getUrn() {
        return this.urn;
    }

    public URL getUrl() {
        return this.url;
    }

    /**
     * Returns the proper JSON with respect to the given {@link Format}
     *
     * @param format one of {@link Format}
     *
     * @return JSON
     * */
    public String toJSON(Format format) {
        return format.equals(Format.register) ? getRegisterJson() : getUpdateJson();
    }

    /**
     * @see <a href="https://wiki.dnb.de/display/URNSERVDOK/Beispiele%3A+URN-Verwaltung#Beispiele:URN-Verwaltung-AustauschenallereigenenURLsaneinerURN">https://wiki.dnb.de/</a>
     * */
    private String getUpdateJson() {
        JsonArray urls = new JsonArray();
        JsonObject url = new JsonObject();
        url.addProperty("url", getURLString());
        url.addProperty("priority", String.valueOf(1));
        urls.add(url);

        return urls.toString();
    }

    /**
     * @see <a href="https://wiki.dnb.de/display/URNSERVDOK/Beispiele%3A+URN-Verwaltung#Beispiele:URN-Verwaltung-RegistriereneinerneuenURN">https://wiki.dnb.de/</a>
     * */
    private String getRegisterJson() {
        JsonObject json = new JsonObject();

        json.addProperty("urn", getURNString());
        JsonArray urls = new JsonArray();
        json.add("urls", urls);

        JsonObject url = new JsonObject();
        url.addProperty("url", getURLString());
        url.addProperty("priority", String.valueOf(1));
        urls.add(url);

        return json.toString();
    }

    private String getURLString() {
        if (getUrl() == null) {
            return "url not set";
        }

        return getUrl().toString();
    }

    private String getURNString() {
        if (getUrn() == null) {
            return "urn not set";
        }

        return getUrn().getIdentifier();
    }
}
