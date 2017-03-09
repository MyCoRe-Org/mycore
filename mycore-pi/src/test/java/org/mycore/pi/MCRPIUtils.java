package org.mycore.pi;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.MCRUUIDURNGenerator;
import org.mycore.pi.urn.rest.MCREpicurLite;
import org.mycore.pi.urn.rest.MCRDNBURNClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

/**
 * Created by chi on 23.02.17.
 * @author Huu Chi Vu
 */
public class MCRPIUtils {
    public static MCRPI generateMCRPI(String fileName) throws MCRPersistentIdentifierException {
        MCRObjectID mycoreID = getNextFreeID();
        return new MCRPI(generateURNFor(mycoreID).asString(), MCRDNBURN.TYPE,
                         mycoreID.toString(), fileName, "TestService", null);
    }

    public static MCRObjectID getNextFreeID() {return MCRObjectID.getNextFreeId("MyCoRe_test");}

    private static MCRDNBURN generateURNFor(MCRObjectID mycoreID) throws
            MCRPersistentIdentifierException {
        MCRUUIDURNGenerator mcruuidurnGenerator = new MCRUUIDURNGenerator("testGenerator");
        return mcruuidurnGenerator.generate(mycoreID, "");
    }

    public static String randomFilename() {
        return UUID.randomUUID()
                   .toString()
                   .concat(".tif");
    }

    public static URL getUrl(MCRPIRegistrationInfo info) {
        try {
            return new URL("http://localhost:8291/deriv_0001/" + info.getAdditional());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static MCRDNBURNClient getMCRURNClient() {
        return new MCRDNBURNClient(MCRPIUtils::getEpicure);
    }

    private static MCREpicurLite getEpicure(MCRPIRegistrationInfo urnInfo) {
        return MCREpicurLite.instance(urnInfo, MCRPIUtils.getUrl(urnInfo))
                            .setCredentials(new UsernamePasswordCredentials("test", "test"));
    }
}
