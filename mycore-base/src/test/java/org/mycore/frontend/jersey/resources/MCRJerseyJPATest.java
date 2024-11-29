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

package org.mycore.frontend.jersey.resources;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.jersey.filter.MCRDBTransactionFilter;
import org.mycore.frontend.jersey.filter.MCRSessionHookFilter;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

public class MCRJerseyJPATest extends MCRStoreTestCase {

    private MCRJerseyTestFeature jersey;

    @Before
    public void setUpJersey() throws Exception {
        jersey = new MCRJerseyTestFeature();
        jersey.setUp(Set.of(
            MCRSessionHookFilter.class,
            MCRDBTransactionFilter.class,
            ObjectResource.class,
            MCRJerseyExceptionMapper.class));
    }

    @After
    public void tearDownJersey() throws Exception {
        jersey.tearDown();
    }

    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        testProperties.put("MCR.Metadata.Type.object", "true");
        return testProperties;
    }

    @Test
    public void rollback() throws MCRAccessException, InterruptedException {
        MCRObjectID id = MCRObjectID.getInstance("mycore_object_00000001");
        try (Response ignored = jersey.target("object/create/" + id).request().post(null)) {
            printTable("MCRObject");
            assertCreateDate("create date should not be null after creation");
        }
        try (Response ignored = jersey.target("object/break/" + id).request().post(null)) {
            printTable("MCRObject");
            assertCreateDate("create date should be rolled back and not be null");
        }
    }

    private void assertCreateDate(String message) {
        queryTable("MCRObject", (resultSet) -> {
            try {
                if (resultSet.next()) {
                    String createDate = resultSet.getString(2);
                    Assert.assertNotNull(message, createDate);
                }
            } catch (SQLException e) {
                Assert.fail("Unable to query MCRObject table");
            }
        });
    }

    @Path("object")
    public static class ObjectResource {

        @Path("create/{id}")
        @POST
        public void create(@PathParam("id") String id) throws MCRAccessException {
            MCRObjectID mcrId = MCRObjectID.getInstance(id);
            MCRObject root = createObject(mcrId.toString());
            MCRMetadataManager.create(root);
        }

        @Path("break/{id}")
        @POST
        public void breakIt(@PathParam("id") String id) {
            String tableName = getTableName("MCRObject");
            executeUpdate("UPDATE " + tableName + " SET CREATEDATE=null WHERE ID='" + id + "'");
            printTable("MCRObject");
            throw new APIException("Breaking!!!");
        }

        private MCRObject createObject(String id) {
            MCRObject object = new MCRObject();
            object.setId(MCRObjectID.getInstance(id));
            object.setSchema("noSchema");
            return object;
        }

    }

    private static final class APIException extends RuntimeException {

        APIException(String message) {
            super(message);
        }

    }

}
