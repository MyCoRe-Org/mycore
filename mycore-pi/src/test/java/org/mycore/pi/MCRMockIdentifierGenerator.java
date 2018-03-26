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

import org.junit.Assert;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRMockIdentifierGenerator extends MCRPIGenerator<MCRMockIdentifier> {

    public static final String TEST_PROPERTY = "mockProperty";

    public static final String TEST_PROPERTY_VALUE = "mockPropertyValue";

    public MCRMockIdentifierGenerator(String generatorID) {
        super(generatorID);
    }

    @Override
    public MCRMockIdentifier generate(MCRBase mcrBase, String additional) throws MCRPersistentIdentifierException {
        Assert.assertEquals("Test propterties should be set!", getProperties().get(TEST_PROPERTY), TEST_PROPERTY_VALUE);

        return (MCRMockIdentifier) new MCRMockIdentifierParser()
            .parse(MCRMockIdentifier.MOCK_SCHEME + mcrBase.getId() + ":" + additional).get();
    }
}
