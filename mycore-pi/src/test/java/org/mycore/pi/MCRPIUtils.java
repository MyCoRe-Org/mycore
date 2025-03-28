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

package org.mycore.pi;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.MCRUUIDURNGenerator;
import org.mycore.pi.urn.rest.MCRDNBURNRestClient;
import org.mycore.pi.urn.rest.MCRURNJsonBundle;

/**
 * Created by chi on 23.02.17.
 *
 * @author Huu Chi Vu
 */
public class MCRPIUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    public static MCRPI generateMCRPI(String fileName, String serviceID) throws MCRPersistentIdentifierException {
        MCRObjectID mycoreID = getNextFreeID();
        return new MCRPI(generateURNFor(mycoreID).asString(), MCRDNBURN.TYPE,
            mycoreID.toString(), fileName, serviceID, null);
    }

    public static MCRObjectID getNextFreeID() {
        return MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId("MyCoRe_test");
    }

    private static MCRDNBURN generateURNFor(MCRObjectID mycoreID) throws MCRPersistentIdentifierException {
        String testGenerator = "testGenerator";
        MCRUUIDURNGenerator mcruuidurnGenerator = new MCRUUIDURNGenerator();
        mcruuidurnGenerator.init(MCRPIService.GENERATOR_CONFIG_PREFIX + testGenerator);
        MCRObject mcrObject1 = new MCRObject();
        mcrObject1.setId(mycoreID);
        return mcruuidurnGenerator.generate(mcrObject1, "");
    }

    public static String randomFilename() {
        return UUID.randomUUID()
            .toString()
            .concat(".tif");
    }

    public static URL getUrl(MCRPIRegistrationInfo info) {
        String url = "http://localhost:8291/deriv_0001/" + info.getAdditional();
        try {
            return new URI(url).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            String message = "Malformed URL: " + url;
            LOGGER.error(message, e);
        }

        return null;
    }

    public static MCRDNBURNRestClient getMCRURNClient() {
        return new MCRDNBURNRestClient(MCRPIUtils::getBundle);
    }

    public static MCRURNJsonBundle getBundle(MCRPIRegistrationInfo urnInfo) {
        return new MCRURNJsonBundle(urnInfo, getUrl(urnInfo));
    }
}
