package org.mycore.pi;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
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
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.backend.MCRPI_;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;
import org.mycore.pi.urn.MCRDNBPIDefProvider;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.rest.MCREpicurLite;
import org.mycore.pi.urn.rest.MCRDNBURNClient;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import static org.mycore.pi.MCRPIUtils.*;

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
        MCRDNBURNClient dnburnClient = getMCRURNClient();

        MCRPIRegistrationInfo info = generateMCRPI(randomFilename());

        //        MCREpicurLite epicurLite = new MCREpicurLite(info);
        //        epicurLite.setUrl(new URL("http://localhost:8291/deriv_0001/" + randomFilename()));
        //
        //        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        //        xmlOutputter.output(epicurLite.toXML(), System.out);

//        dnburnClient
//                .put(info, this::callback);

        //        System.out.println("HEAD: " + dnburnClient.head(info));
//                        System.out.println("PUT: " + dnburnClient.put(info));
        //        System.out.println("POST: " + dnburnClient.post(epicurLite));

        //        new MCRDNBURNParser().parse(info.getIdentifier())
        //                             .ifPresent(urn -> resolveURN(urn));

    }

    private void callback(HttpResponse response, MCREpicurLite mcrEpicurLite) {
        System.out.println("PUT: " + response.getStatusLine().getStatusCode());
        System.out.println("PUT: " + response.getStatusLine().getReasonPhrase());
        Stream.of(response.getAllHeaders())
              .filter(header -> header.getName().equals("Date"))
              .map(Header::getValue)
              .map("HEADER: "::concat)
              .forEach(System.out::println);



        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        try {
            xmlOutputter.output(mcrEpicurLite.toXML(), System.out);
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
