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

package org.mycore.datamodel.objectinfo;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.backend.jpa.objectinfo.MCRObjectInfoEntity;
import org.mycore.backend.jpa.objectinfo.MCRObjectInfoEntityQueryResolver;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.content.MCRURLContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.xml.sax.SAXParseException;

import jakarta.persistence.EntityManager;

public class MCRObjectInfoEntityQueryResolverTest extends MCRJPATestCase {

    public static final Instant YESTERDAY = Instant.now().minus(24, ChronoUnit.HOURS);

    public static final Instant ONE_WEEK_AGO = Instant.now().minus(24 * 7, ChronoUnit.HOURS);

    public static final Instant TWO_WEEKS_AGO = Instant.now().minus(2 * 24 * 7, ChronoUnit.HOURS);

    public static final Instant THREE_WEEKS_AGO = Instant.now().minus(3 * 24 * 7, ChronoUnit.HOURS);

    public static final String TEST_ID_1 = "junit_foo_00000001";

    public static final String TEST_ID_2 = "junit_test_00000002";

    public static final String TEST_USER_1 = "editor";

    public static final String TEST_USER_2 = "admin";

    public static final String TEST_STATE_1 = "submitted";

    public static final String TEST_STATE_2 = "published";

    public static final String TEST_CATEGORY_1 = "mir_licenses:cc_3.0";

    public static final String TEST_CATEGORY_2 = "mir_licenses:rights_reserved";

    public static final String TEST_CATEGORY_3 = "mir_licenses:ogl";

    static MCRCategoryDAOImpl DAO;

    static MCRCategLinkService CLS;

    private MCRObjectQueryResolver instance;

    private static void initClassifications() throws SAXParseException, MalformedURLException, URISyntaxException {
        DAO = new MCRCategoryDAOImpl();
        CLS = MCRCategLinkServiceFactory.getInstance();

        URL classResourceUrl = MCRObjectInfoEntityQueryResolverTest.class
            .getResource("/mycore-classifications/mir_licenses.xml");
        Document xml = MCRXMLParserFactory.getParser().parseXML(new MCRURLContent(classResourceUrl));
        MCRCategory licenses = MCRXMLTransformer.getCategory(xml);
        DAO.addCategory(null, licenses);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        initClassifications();

        storeObjectInfo(
            TEST_ID_1,
            ONE_WEEK_AGO,
            YESTERDAY,
            TEST_USER_2,
            TEST_USER_1,
            TEST_STATE_1,
            Stream.of(TEST_CATEGORY_1, TEST_CATEGORY_3).collect(Collectors.toList()));

        storeObjectInfo(
            TEST_ID_2,
            THREE_WEEKS_AGO,
            TWO_WEEKS_AGO,
            TEST_USER_1,
            TEST_USER_2,
            TEST_STATE_2,
            Stream.of(TEST_CATEGORY_2, TEST_CATEGORY_3).collect(Collectors.toList()));

        instance = MCRObjectQueryResolver.getInstance();
    }

    public MCRObjectInfoEntity storeObjectInfo(String id,
        Instant createdDate,
        Instant modifiedDate,
        String createdBy,
        String modifiedBy,
        String state,
        List<String> linkedCategoryIds) {
        MCRObjectInfoEntity infoEntity = new MCRObjectInfoEntity();

        infoEntity.setId(MCRObjectID.getInstance(id));
        infoEntity.setCreateDate(createdDate);
        infoEntity.setModifyDate(modifiedDate);
        infoEntity.setCreatedBy(createdBy);
        infoEntity.setModifiedBy(modifiedBy);
        infoEntity.setState(state);

        List<MCRCategoryID> linkedCategories = linkedCategoryIds.stream().map(MCRCategoryID::fromString)
            .collect(Collectors.toList());
        MCRCategLinkReference objectReference = new MCRCategLinkReference(infoEntity.getId());
        CLS.setLinks(objectReference, linkedCategories);

        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.persist(infoEntity);

        return infoEntity;
    }

    @Test
    public void sortByIdTest() {
        MCRObjectQuery query = new MCRObjectQuery();
        query.sort(MCRObjectQuery.SortBy.id, MCRObjectQuery.SortOrder.asc);
        List<MCRObjectInfo> result = instance.getInfos(query);

        Assert.assertEquals("The first result should be " + TEST_ID_1, MCRObjectID.getInstance(TEST_ID_1),
            result.get(0).getId());

        Assert.assertEquals("The second result should be " + TEST_ID_2, MCRObjectID.getInstance(TEST_ID_2),
            result.get(1).getId());

        int count = instance.count(query);
        Assert.assertEquals("All objects should match", 2, count);
    }

    @Test
    public void createdBeforeTest() {
        MCRObjectQuery createdBeforeTwoWeeksAgo = new MCRObjectQuery().createdBefore(TWO_WEEKS_AGO);
        List<MCRObjectInfo> result = instance.getInfos(createdBeforeTwoWeeksAgo);
        Assert.assertEquals("The only result should be " + TEST_ID_2, MCRObjectID.getInstance(TEST_ID_2),
            result.get(0).getId());

        int count = instance.count(createdBeforeTwoWeeksAgo);
        Assert.assertEquals("Only one object should match", 1, count);
    }

    @Test
    public void createdAfterTest() {
        MCRObjectQuery createdAfterTwoWeeksAgo = new MCRObjectQuery().createdAfter(TWO_WEEKS_AGO);
        List<MCRObjectInfo> result = instance.getInfos(createdAfterTwoWeeksAgo);

        Assert.assertEquals("The only result should be " + TEST_ID_1, MCRObjectID.getInstance(TEST_ID_1),
            result.get(0).getId());

        int count = instance.count(createdAfterTwoWeeksAgo);
        Assert.assertEquals("Only one object should match", 1, count);
    }

    @Test
    public void modifiedBeforeTest() {
        MCRObjectQuery modifiedBeforeOneWeekAgo = new MCRObjectQuery().modifiedBefore(ONE_WEEK_AGO);
        List<MCRObjectInfo> result = instance.getInfos(modifiedBeforeOneWeekAgo);
        Assert.assertEquals("The only result should be " + TEST_ID_2, MCRObjectID.getInstance(TEST_ID_2),
            result.get(0).getId());

        int count = instance.count(modifiedBeforeOneWeekAgo);
        Assert.assertEquals("Only one object should match", 1, count);
    }

    @Test
    public void modifiedAfterTest() {
        MCRObjectQuery modifiedAfterOneWeekAgo = new MCRObjectQuery().modifiedAfter(ONE_WEEK_AGO);
        List<MCRObjectInfo> result = instance.getInfos(modifiedAfterOneWeekAgo);
        Assert.assertEquals("The only result should be " + TEST_ID_1, MCRObjectID.getInstance(TEST_ID_1),
            result.get(0).getId());

        int count = instance.count(modifiedAfterOneWeekAgo);
        Assert.assertEquals("Only one object should match", 1, count);
    }

    @Test
    public void createdByTest() {
        MCRObjectQuery createdByTestUser1 = new MCRObjectQuery().createdBy(TEST_USER_1);
        List<MCRObjectInfo> result = instance.getInfos(createdByTestUser1);
        Assert.assertEquals("The only result should be " + TEST_ID_2, MCRObjectID.getInstance(TEST_ID_2),
            result.get(0).getId());

        int count = instance.count(createdByTestUser1);
        Assert.assertEquals("Only one object should match", 1, count);

        MCRObjectQuery createdByTestUser2 = new MCRObjectQuery().createdBy(TEST_USER_2);
        result = instance.getInfos(createdByTestUser2);
        Assert.assertEquals("The only result should be " + TEST_ID_1, MCRObjectID.getInstance(TEST_ID_1),
            result.get(0).getId());

        count = instance.count(createdByTestUser2);
        Assert.assertEquals("Only one object should match", 1, count);

    }

    @Test
    public void objectNumberTest() {
        MCRObjectQuery numberGreaterTestID1 = new MCRObjectQuery().numberGreater(MCRObjectID.getInstance(TEST_ID_1)
            .getNumberAsInteger());
        List<MCRObjectInfo> result = instance.getInfos(numberGreaterTestID1);
        Assert.assertEquals("The only result should be " + TEST_ID_2, MCRObjectID.getInstance(TEST_ID_2),
            result.get(0).getId());

        int count = instance.count(numberGreaterTestID1);
        Assert.assertEquals("Only one object should match", 1, count);

        MCRObjectQuery numberLessTestID2 = new MCRObjectQuery().numberLess(MCRObjectID.getInstance(TEST_ID_2)
            .getNumberAsInteger());
        result = instance.getInfos(numberLessTestID2);
        Assert.assertEquals("The only result should be " + TEST_ID_1, MCRObjectID.getInstance(TEST_ID_1),
            result.get(0).getId());

        count = instance.count(numberLessTestID2);
        Assert.assertEquals("Only one object should match", 1, count);
    }

    @Test
    public void objectTypeTest() {
        MCRObjectQuery typeTest = new MCRObjectQuery().type(MCRObjectID.getInstance(TEST_ID_1).getTypeId());
        List<MCRObjectInfo> result = instance.getInfos(typeTest);
        Assert.assertEquals("The only result should be " + TEST_ID_1, MCRObjectID.getInstance(TEST_ID_1),
            result.get(0).getId());

        int count = instance.count(typeTest);
        Assert.assertEquals("Only one object should match", 1, count);

        MCRObjectQuery typeFoo = new MCRObjectQuery().type(MCRObjectID.getInstance(TEST_ID_2).getTypeId());

        result = instance.getInfos(typeFoo);
        Assert.assertEquals("The only result should be " + TEST_ID_2, MCRObjectID.getInstance(TEST_ID_2),
            result.get(0).getId());

        count = instance.count(typeFoo);
        Assert.assertEquals("Only one object should match", 1, count);
    }

    @Test
    public void stateTest() {
        MCRObjectQuery state1 = new MCRObjectQuery().status(TEST_STATE_1);
        List<MCRObjectInfo> result = instance.getInfos(state1);
        Assert.assertEquals("The only result should be " + TEST_ID_1, MCRObjectID.getInstance(TEST_ID_1),
            result.get(0).getId());

        int count = instance.count(state1);
        Assert.assertEquals("Only one object should match", 1, count);

        MCRObjectQuery state2 = new MCRObjectQuery().status(TEST_STATE_2);

        result = instance.getInfos(state2);
        Assert.assertEquals("The only result should be " + TEST_ID_2, MCRObjectID.getInstance(TEST_ID_2),
            result.get(0).getId());

        count = instance.count(state2);
        Assert.assertEquals("Only one object should match", 1, count);
    }

    @Test
    public void categoryTest() {
        MCRObjectQuery category1 = new MCRObjectQuery();
        category1.getIncludeCategories().add(TEST_CATEGORY_1);
        List<MCRObjectInfo> result = instance.getInfos(category1);

        Assert.assertEquals("The only result should be " + TEST_ID_1, MCRObjectID.getInstance(TEST_ID_1),
            result.get(0).getId());

        int count = instance.count(category1);
        Assert.assertEquals("Only one object should match", 1, count);

        MCRObjectQuery category2 = new MCRObjectQuery();
        category2.getIncludeCategories().add(TEST_CATEGORY_2);
        result = instance.getInfos(category2);

        Assert.assertEquals("The only result should be " + TEST_ID_2, MCRObjectID.getInstance(TEST_ID_2),
            result.get(0).getId());

        count = instance.count(category2);
        Assert.assertEquals("Only one object should match", 1, count);

        MCRObjectQuery category3 = new MCRObjectQuery().sort(MCRObjectQuery.SortBy.id, MCRObjectQuery.SortOrder.asc);
        category3.getIncludeCategories().add(TEST_CATEGORY_3);
        result = instance.getInfos(category3);

        Assert.assertEquals("The first result should be " + TEST_ID_1, MCRObjectID.getInstance(TEST_ID_1),
            result.get(0).getId());
        Assert.assertEquals("The second result should be " + TEST_ID_2, MCRObjectID.getInstance(TEST_ID_2),
            result.get(1).getId());

        count = instance.count(category3);
        Assert.assertEquals("both object should match", 2, count);

        MCRObjectQuery category1and3 = new MCRObjectQuery().sort(MCRObjectQuery.SortBy.id,
            MCRObjectQuery.SortOrder.asc);
        category1and3.getIncludeCategories().add(TEST_CATEGORY_1);
        category1and3.getIncludeCategories().add(TEST_CATEGORY_3);
        result = instance.getInfos(category1and3);

        Assert.assertEquals("The only result should be " + TEST_ID_1, MCRObjectID.getInstance(TEST_ID_1),
            result.get(0).getId());

        count = instance.count(category1and3);
        Assert.assertEquals("Only one object should match", 1, count);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();

        testProperties.put("MCR.Object.QueryResolver.Class", MCRObjectInfoEntityQueryResolver.class.getName());
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        testProperties.put("MCR.Metadata.Type.foo", Boolean.TRUE.toString());

        return testProperties;
    }
}
