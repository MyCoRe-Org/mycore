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

package org.mycore.pi.doi;

import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRCreateDateDOIGenerator extends MCRPersistentIdentifierGenerator<MCRDigitalObjectIdentifier> {

    private static final String DATE_PATTERN = "yyyyMMdd-HHmmss";

    private final MCRDOIParser mcrdoiParser;

    private String prefix = MCRConfiguration.instance().getString("MCR.DOI.Prefix");

    public MCRCreateDateDOIGenerator(String generatorID) {
        super(generatorID);
        mcrdoiParser = new MCRDOIParser();
    }

    @Override
    public MCRDigitalObjectIdentifier generate(MCRObjectID mcrID, String additional)
        throws MCRPersistentIdentifierException {
        Date createdate = MCRMetadataManager.retrieveMCRObject(mcrID).getService().getDate("createdate");
        if (createdate != null) {
            MCRISO8601Date mcrdate = new MCRISO8601Date();
            mcrdate.setDate(createdate);
            String format = mcrdate.format(DATE_PATTERN, Locale.ENGLISH);
            Optional<MCRDigitalObjectIdentifier> parse = mcrdoiParser.parse(prefix + "/" + format);
            MCRPersistentIdentifier doi = parse.get();
            return (MCRDigitalObjectIdentifier) doi;
        } else {
            throw new MCRPersistenceException("The object " + mcrID + " doesn't have a createdate!");
        }
    }
}
