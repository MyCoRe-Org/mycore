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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.metadata.MCRBase;

import java.util.Date;

public class MCRMockIdentifierService extends MCRPIService<MCRMockIdentifier> {
    protected static final String TYPE = "mock";

    public static final Logger LOGGER = LogManager.getLogger();

    public MCRMockIdentifierService() {
        super(TYPE);
    }

    private boolean registerCalled = false;

    private boolean deleteCalled = false;

    private boolean updatedCalled = false;

    @Override
    protected Date registerIdentifier(MCRBase obj, String additional, MCRMockIdentifier mi) {
        registerCalled = true;
        return new Date();
    }

    @Override
    public void delete(MCRMockIdentifier identifier, MCRBase obj, String additional) {
        deleteCalled = true;
    }

    @Override
    public void update(MCRMockIdentifier identifier, MCRBase obj, String additional) {
        updatedCalled = true;
    }

    public boolean isRegisterCalled() {
        return registerCalled;
    }

    public boolean isDeleteCalled() {
        return deleteCalled;
    }

    public boolean isUpdatedCalled() {
        return updatedCalled;
    }

    protected void reset() {
        this.registerCalled = this.deleteCalled = this.updatedCalled = false;
    }
}
