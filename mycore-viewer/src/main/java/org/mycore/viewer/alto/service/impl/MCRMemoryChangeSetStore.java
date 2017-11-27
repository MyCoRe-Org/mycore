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

package org.mycore.viewer.alto.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.inject.MCRInjectorConfig;
import org.mycore.viewer.alto.model.MCRAltoChangeSet;
import org.mycore.viewer.alto.model.MCRStoredChangeSet;
import org.mycore.viewer.alto.service.MCRAltoChangeSetStore;
import org.mycore.viewer.alto.service.MCRDerivateTitleResolver;

import com.google.inject.Singleton;

@Singleton
public class MCRMemoryChangeSetStore implements MCRAltoChangeSetStore {

    private MCRDerivateTitleResolver titleResolver = MCRInjectorConfig.injector()
        .getInstance(MCRDerivateTitleResolver.class);

    private Map<String, List<MCRStoredChangeSet>> derivateChangeSet = new HashMap<>();

    private Map<String, List<MCRStoredChangeSet>> sessionIDChangeSet = new HashMap<>();

    private Map<String, MCRStoredChangeSet> idChangeSet = new HashMap<>();

    public MCRMemoryChangeSetStore() {
    }

    @Override
    public MCRStoredChangeSet get(String pid) {
        return this.idChangeSet.get(pid);
    }

    @Override
    public MCRStoredChangeSet storeChangeSet(MCRAltoChangeSet changeSet) {
        MCRStoredChangeSet mcrStoredChangeSet = new MCRStoredChangeSet();

        String derivateID = changeSet.getDerivateID();
        String objectTitle = titleResolver.resolveTitle(derivateID);

        mcrStoredChangeSet.setUser(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID());
        mcrStoredChangeSet.setObjectTitle(objectTitle);
        mcrStoredChangeSet.setCreated(new Date());
        mcrStoredChangeSet.setChangeSet(changeSet);
        mcrStoredChangeSet.setSessionID(MCRSessionMgr.getCurrentSessionID());
        mcrStoredChangeSet.setDerivateID(derivateID);

        return storeChangeSet(mcrStoredChangeSet);
    }

    public MCRStoredChangeSet storeChangeSet(MCRStoredChangeSet storedChangeSet) {
        addTo(derivateChangeSet, storedChangeSet.getDerivateID(), storedChangeSet);
        addTo(sessionIDChangeSet, storedChangeSet.getSessionID(), storedChangeSet);
        idChangeSet.put(storedChangeSet.getPid(), storedChangeSet);
        return storedChangeSet;
    }

    @Override
    public MCRStoredChangeSet updateChangeSet(String pid, MCRAltoChangeSet changeSet) {
        MCRStoredChangeSet storedChangeSet = this.get(pid);
        this.delete(pid);
        return this.storeChangeSet(storedChangeSet);
    }

    @Override
    public List<MCRStoredChangeSet> list() {
        return new ArrayList<>(idChangeSet.values());
    }

    @Override
    public List<MCRStoredChangeSet> listBySessionID(String sessionID) {
        List<MCRStoredChangeSet> changeSets = sessionIDChangeSet.get(sessionID);
        if (changeSets != null) {
            return changeSets.stream().filter(change -> change.getApplied() == null).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<MCRStoredChangeSet> listByDerivate(String derivateID) {
        List<MCRStoredChangeSet> changeSets = derivateChangeSet.get(derivateID);
        if (changeSets != null) {
            return changeSets.stream().filter(change -> change.getApplied() == null).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<MCRStoredChangeSet> list(long start, long count) {
        return idChangeSet
            .values()
            .stream()
            .filter(change -> change.getApplied() == null)
            .skip(start)
            .limit(count)
            .collect(Collectors.toList());
    }

    @Override
    public List<MCRStoredChangeSet> listBySessionID(long start, long count, String sessionID) {
        List<MCRStoredChangeSet> changeSets = sessionIDChangeSet
            .get(sessionID);
        if (changeSets != null) {
            return changeSets
                .stream()
                .filter(change -> change.getApplied() == null)
                .skip(start)
                .limit(count)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<MCRStoredChangeSet> listByDerivate(long start, long count, String derivateID) {
        List<MCRStoredChangeSet> changeSets = derivateChangeSet
            .get(derivateID);
        if (changeSets != null) {
            return changeSets
                .stream()
                .filter(change -> change.getApplied() == null)
                .skip(start)
                .limit(count)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public long count() {
        return idChangeSet.size();
    }

    @Override
    public void delete(String pid) {
        MCRStoredChangeSet mcrStoredChangeSet = idChangeSet.get(pid);
        derivateChangeSet.get(mcrStoredChangeSet.getDerivateID()).remove(mcrStoredChangeSet);
        sessionIDChangeSet.get(mcrStoredChangeSet.getSessionID()).remove(mcrStoredChangeSet);
        this.idChangeSet.remove(pid);
    }

    private void addTo(Map<String, List<MCRStoredChangeSet>> setMap, String key, MCRStoredChangeSet set) {
        List<MCRStoredChangeSet> list;

        if (setMap.containsKey(key)) {
            list = setMap.get(key);
        } else {
            list = new ArrayList<>();
            setMap.put(key, list);
        }

        list.add(set);
    }
}
