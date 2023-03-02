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

package org.mycore.ocfl;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.classifications2.MCRCategoryID;

import edu.wisc.library.ocfl.api.OcflRepository;

/**
 *@deprecated use {@link org.mycore.ocfl.classification.MCROCFLXMLClassificationManager MCROCFLXMLClassificationManager}
 */
@Deprecated(forRemoval = true)
public class MCROCFLXMLClassificationManager extends org.mycore.ocfl.classification.MCROCFLXMLClassificationManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DEP_WARN
        = "\u001B[93m" + "Usage of the toplevel ocfl classes is deprecated and will be removed in future releases, " +
            "please use 'org.mycore.ocfl.classification.MCROCFLXMLClassificationManager' instead." + "\u001B[0m";

    public MCROCFLXMLClassificationManager() {
        LOGGER.warn(DEP_WARN);
    }

    @Override
    public void create(MCRCategoryID mcrCg, MCRContent xml) throws IOException {
        LOGGER.warn(DEP_WARN);
        super.create(mcrCg, xml);
    }

    @Override
    public void delete(MCRCategoryID mcrid) throws IOException {
        LOGGER.warn(DEP_WARN);
        super.delete(mcrid);
    }

    @Override
    protected OcflRepository getRepository() throws ClassCastException {
        LOGGER.warn(DEP_WARN);
        return super.getRepository();
    }

    @Override
    public MCRContent retrieveContent(MCRCategoryID mcrid) throws IOException {
        LOGGER.warn(DEP_WARN);
        return super.retrieveContent(mcrid);
    }
    @Override
    public MCRContent retrieveContent(MCRCategoryID mcrid, String revision) throws IOException {
        LOGGER.warn(DEP_WARN);
        return super.retrieveContent(mcrid, revision);
    }

    @Override
    public void update(MCRCategoryID mcrCg, MCRContent xml) throws IOException {
        LOGGER.warn(DEP_WARN);
        super.update(mcrCg, xml);
    }

}
