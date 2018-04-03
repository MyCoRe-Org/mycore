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

package org.mycore.pi.purl;

import java.net.MalformedURLException;
import java.net.URL;

import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * The property BaseURLTemplate is your url and the {@link MCRIDPURLGenerator} will replace $ID with the actual MyCoRe-ID.
 */
public class MCRIDPURLGenerator extends MCRPIGenerator<MCRPURL> {

    public MCRIDPURLGenerator(String generatorID) {
        super(generatorID);
    }

    @Override
    public MCRPURL generate(MCRBase mcrObj, String additional)
        throws MCRPersistentIdentifierException {

        String baseUrlTemplate = getProperties().getOrDefault("BaseURLTemplate", "");

        String replace = baseUrlTemplate;
        String before;

        do {
            before = replace;
            replace = baseUrlTemplate.replace("$ID", mcrObj.getId().toString());
        } while (!replace.equals(before));

        try {
            URL url = new URL(replace);
            return new MCRPURL(url);
        } catch (MalformedURLException e) {
            throw new MCRPersistentIdentifierException("Error while creating URL object from string: " + replace, e);
        }
    }
}
