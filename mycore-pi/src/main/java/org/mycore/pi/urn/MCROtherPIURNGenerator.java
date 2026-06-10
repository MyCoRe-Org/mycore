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

package org.mycore.pi.urn;

import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.util.MCROtherPIValueExtractor;

public class MCROtherPIURNGenerator extends MCRDNBURNGenerator {

    private MCROtherPIValueExtractor extractor;

    @MCRProperty(name = "Type")
    public String type;

    @MCRProperty(name = "Service")
    public String service;

    @MCRProperty(name = "Pattern")
    public String pattern;

    @MCRPostConstruction
    public void createExtractor() {
        extractor = new MCROtherPIValueExtractor(type, service, pattern);
    }

    @Override
    protected String buildNISS(MCRObjectID mcrID, String additional) {
        return extractor.extractValue(mcrID);
    }

}
