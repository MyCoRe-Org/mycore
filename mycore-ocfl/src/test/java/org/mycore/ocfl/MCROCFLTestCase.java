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

package org.mycore.ocfl;

import java.io.IOException;

import org.mycore.common.MCRTestCase;
import org.mycore.ocfl.repository.MCROCFLRepository;

public class MCROCFLTestCase extends MCRTestCase {

    protected MCROCFLRepository repository;

    public void setUpRepository(boolean remote) throws IOException {
        this.repository = MCROCFLTestCaseHelper.setUp(remote);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        MCROCFLTestCaseHelper.tearDown(this.repository);
    }

}
