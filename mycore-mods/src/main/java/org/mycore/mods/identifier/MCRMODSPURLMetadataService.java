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

package org.mycore.mods.identifier;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPIMetadataService;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.purl.MCRPURL;

public class MCRMODSPURLMetadataService extends MCRAbstractMODSMetadataService {

    public MCRMODSPURLMetadataService(String inscriberID) {
        super(inscriberID);
    }

    @Override
    protected String getIdentifierType() {
        return "purl";
    }
}
