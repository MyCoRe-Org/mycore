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

import java.io.IOException;
import java.util.HashMap;

import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

import mockit.Mock;
import mockit.MockUp;

/**
 * Created by chi on 29.03.17.
 * @author Huu Chi Vu
 */
public class MockMetadataManager extends MockUp<MCRMetadataManager> {
    private HashMap<MCRObjectID, MCRBase> objMap;

    public MockMetadataManager() {
        objMap = new HashMap<>();
    }

    @Mock
    public void update(final MCRDerivate mcrDerivate) throws MCRPersistenceException, IOException,
        MCRAccessException {
    }

    @Mock
    public MCRBase retrieve(final MCRObjectID id) throws MCRPersistenceException {
        return objMap.get(id);
    }

    @Mock
    public MCRDerivate retrieveMCRDerivate(final MCRObjectID id) throws MCRPersistenceException {
        return (MCRDerivate) objMap.get(id);
    }

    public void put(MCRObjectID id, MCRBase obj) {
        objMap.put(id, obj);
    }
}
