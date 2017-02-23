package org.mycore.pi;

import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.backend.MCRPI_;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.urn.MCRDNBPIDefProvider;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.MCRUUIDURNGenerator;
import org.mycore.pi.urn.rest.MCREpicurLite;
import org.mycore.pi.urn.rest.MCRURNGranularRESTRegistrationTask;
import org.mycore.pi.urn.rest.MCRURNServer;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

/**
 * Created by chi on 01.02.17.
 * @author Huu Chi Vu
 */
public class MCRServerTest extends MCRStoreTestCase {
    Logger LOGGER = LogManager.getLogger();

    private static final String BASE_ID = "MyCoRe_test";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testURNGerating() throws Exception {
        MCRPIRegistrationInfo info = generateMCRPI(randomFilename());
        System.out.println(info.getIdentifier());
    }

    @Test
    public void connectionTest() throws Exception {
        MCRURNServer mcrurnServer = getMCRURNServer();

        MCRPIRegistrationInfo info = generateMCRPI(randomFilename());

        //        MCREpicurLite epicurLite = new MCREpicurLite(info);
        //        epicurLite.setUrl(new URL("http://localhost:8291/deriv_0001/" + randomFilename()));
        //
        //        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        //        xmlOutputter.output(epicurLite.getEpicurLite(), System.out);

        mcrurnServer
                .put(info, this::callback);

        //        System.out.println("HEAD: " + mcrurnServer.head(info));
//                System.out.println("PUT: " + mcrurnServer.put(info));
        //        System.out.println("POST: " + mcrurnServer.post(epicurLite));

        //        new MCRDNBURNParser().parse(info.getIdentifier())
        //                             .ifPresent(urn -> resolveURN(urn));

    }

    private void callback(HttpResponse response, MCREpicurLite mcrEpicurLite) {
        System.out.println("PUT: " + response.getStatusLine().getStatusCode());
        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        try {
            xmlOutputter.output(mcrEpicurLite.getEpicurLite(), System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resolveURN(MCRDNBURN urn) {
        try {
            Document document = MCRDNBPIDefProvider.get(urn);
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            xmlOutputter.output(document, System.out);
        } catch (MCRIdentifierUnresolvableException | IOException e) {
            e.printStackTrace();
        }
    }


    private MCRURNServer getMCRURNServer() {
        return new MCRURNServer(new UsernamePasswordCredentials("test", "test"), this::getUrl);
    }

    private String randomFilename() {
        return UUID.randomUUID()
                   .toString()
                   .concat(".tif");
    }

    private MCRPI generateMCRPI(String fileName) throws MCRPersistentIdentifierException {
        MCRObjectID mycoreID = MCRObjectID.getNextFreeId("MyCoRe_test");
        return new MCRPI(generateURNFor(mycoreID).asString(), MCRDNBURN.TYPE,
                         mycoreID.toString(), fileName);
    }

    private MCRDNBURN generateURNFor(MCRObjectID mycoreID) throws
            MCRPersistentIdentifierException {
        MCRUUIDURNGenerator mcruuidurnGenerator = new MCRUUIDURNGenerator("testGenerator");
        return mcruuidurnGenerator.generate(mycoreID, "");
    }

    @Test
    public void testTimerTask() throws Exception {
        MCRHIBConnection.instance().getSession().save(generateMCRPI(randomFilename()));
        MCRHIBConnection.instance().getSession().save(generateMCRPI(randomFilename()));
        MCRHIBConnection.instance().getSession().save(generateMCRPI(randomFilename()));
        MCRHIBConnection.instance().getSession().save(generateMCRPI(randomFilename()));

        MCRURNGranularRESTRegistrationTask registrationTask = new MCRURNGranularRESTRegistrationTask(getMCRURNServer());

        registrationTask.run();
    }

    private URL getUrl(MCRPIRegistrationInfo info) {
        try {
            return new URL("http://localhost:8291/deriv_0001/" + info.getAdditional());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Test
    public void saveIdentifierToDB() throws Exception {
        MCRHIBConnection.instance().getSession().save(generateMCRPI(randomFilename()));
        MCRHIBConnection.instance().getSession().save(generateMCRPI(randomFilename()));

        EntityManager em = MCREntityManagerProvider
                .getCurrentEntityManager();

        CriteriaQuery<MCRPIRegistrationInfo> criteriaQuery = em
                .getCriteriaBuilder()
                .createQuery(MCRPIRegistrationInfo.class);

        Root<MCRPI> fooEntry = criteriaQuery.from(MCRPI.class);

        criteriaQuery.select(fooEntry)
                     .where(fooEntry.get(MCRPI_.registered).isNull());

        em.createQuery(criteriaQuery)
          .getResultList()
          .stream()
          //          .map(MCRPIRegistrationInfo::getIdentifier)
          .map(pi -> pi.getIdentifier() + " | " + pi.getRegistered())
          .map("PI: "::concat)
          .forEach(LOGGER::info);
    }

    @Test
    public void getUnregisteredPITest() throws Exception {
        MCRHIBConnection.instance().getSession().save(generateMCRPI(randomFilename()));
        MCRHIBConnection.instance().getSession().save(generateMCRPI(randomFilename()));

        MCRPersistentIdentifierManager.getInstance()
                                      .getUnregisteredIdenifiers(MCRDNBURN.TYPE)
                                      .stream()
                                      .map(pi -> pi.getIdentifier() + " | " + pi.getRegistered())
                                      .map("PI: "::concat)
                                      .forEach(LOGGER::info);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        testProperties.put("MCR.PI.Generator.testGenerator.Namespace", "frontend-");
        return testProperties;
    }
}
