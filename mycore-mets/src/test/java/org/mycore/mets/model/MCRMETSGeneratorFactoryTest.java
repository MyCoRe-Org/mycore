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

package org.mycore.mets.model;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mycore.common.config.MCRConfiguration;

public class MCRMETSGeneratorFactoryTest {

    @Test
    public void getGenerator() throws Exception {
        // prepare config
        MCRConfiguration.instance().set("MCR.Component.MetsMods.Generator", TestGenerator.class.getName());
        // check getGenerator
        MCRMETSGenerator generator = MCRMETSGeneratorFactory.create(null);
        assertTrue(generator instanceof TestGenerator);
    }

    public static class TestGenerator implements MCRMETSGenerator {
        @Override
        public Mets generate() {
            return new Mets();
        }
    }

}
