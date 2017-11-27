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

package org.mycore.pi;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRMockMetadataManager extends MCRPersistentIdentifierMetadataManager<MCRMockIdentifier> {

    public static final String TEST_PROPERTY = "mockProperty";

    public static final String TEST_PROPERTY_VALUE = "mockPropertyValue";

    private Map<String, MCRMockIdentifier> map = new HashMap<>();

    public MCRMockMetadataManager(String inscriberID) {
        super(inscriberID);
    }

    @Override
    public void insertIdentifier(MCRMockIdentifier identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        Assert.assertEquals("Test propterties should be set!", getProperties().get(TEST_PROPERTY), TEST_PROPERTY_VALUE);
        map.put(obj + additional, identifier);
    }

    @Override
    public void removeIdentifier(MCRMockIdentifier identifier, MCRBase obj, String additional) {
        Assert.assertEquals("Test properties should be set!", getProperties().get(TEST_PROPERTY), TEST_PROPERTY_VALUE);
        map.remove(obj + additional);
    }

    @Override
    public Optional<MCRPersistentIdentifier> getIdentifier(MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        return Optional.ofNullable(map.get(obj + additional));
    }

}
