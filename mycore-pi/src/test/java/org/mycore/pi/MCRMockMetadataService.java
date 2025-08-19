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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.mycore.datamodel.metadata.MCRBase;

public class MCRMockMetadataService extends MCRPIMetadataService<MCRMockIdentifier> {

    public static final String TEST_PROPERTY = "mockProperty";

    public static final String TEST_PROPERTY_VALUE = "mockPropertyValue";

    private Map<String, MCRMockIdentifier> map = new HashMap<>();

    @Override
    public void insertIdentifier(MCRMockIdentifier identifier, MCRBase obj, String additional) {
        assertEquals(TEST_PROPERTY_VALUE, getProperties().get(TEST_PROPERTY), "Test properties should be set!");
        map.put(obj + additional, identifier);
    }

    @Override
    public void removeIdentifier(MCRMockIdentifier identifier, MCRBase obj, String additional) {
        assertEquals(TEST_PROPERTY_VALUE, getProperties().get(TEST_PROPERTY), "Test properties should be set!");
        map.remove(obj + additional);
    }

    @Override
    public Optional<MCRPersistentIdentifier> getIdentifier(MCRBase obj, String additional) {
        return Optional.ofNullable(map.get(obj + additional));
    }

}
