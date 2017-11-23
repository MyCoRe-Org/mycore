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

package org.mycore.datamodel.metadata.share;

import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * @author Thomas Scheffler (yagee)
 * @see MCRMetadataShareAgentFactory
 */
public interface MCRMetadataShareAgent {

    /**
     * Determines if shareable metadata changed from <code>oldVersion</code> to <code>newVersion</code>
     * @param oldVersion previous version of MCRObject
     * @param newVersion new version of MCRObject
     */
    boolean shareableMetadataChanged(MCRObject oldVersion, MCRObject newVersion);

    /**
     * updates all recipients of shareable metadata from <code>holder</code>.
     * @param holder usually the parent object that can distrivute metadata
     * @throws MCRAccessException 
     * @throws MCRPersistenceException 
     */
    void distributeMetadata(MCRObject holder) throws MCRPersistenceException, MCRAccessException;

    /**
     * Include shareable metadata from <code>holder</code> before persisting <code>recipient</code>.
     * @param recipient on update/create before handling events.
     */
    void receiveMetadata(MCRObject recipient);
}
