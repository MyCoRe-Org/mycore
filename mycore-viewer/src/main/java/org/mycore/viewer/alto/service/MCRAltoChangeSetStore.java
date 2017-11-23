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

package org.mycore.viewer.alto.service;

import java.util.List;

import org.mycore.viewer.alto.model.MCRAltoChangeSet;
import org.mycore.viewer.alto.model.MCRStoredChangeSet;

public interface MCRAltoChangeSetStore {

    MCRStoredChangeSet get(String pid);

    MCRStoredChangeSet storeChangeSet(MCRAltoChangeSet changeSet);

    MCRStoredChangeSet updateChangeSet(String pid, MCRAltoChangeSet changeSet);

    List<MCRStoredChangeSet> list();

    List<MCRStoredChangeSet> listBySessionID(String sessionID);

    List<MCRStoredChangeSet> listByDerivate(String derivateID);

    List<MCRStoredChangeSet> list(long start, long count);

    List<MCRStoredChangeSet> listBySessionID(long start, long count, String sessionID);

    List<MCRStoredChangeSet> listByDerivate(long start, long count, String derivateID);

    long count();

    void delete(String pid);
}
