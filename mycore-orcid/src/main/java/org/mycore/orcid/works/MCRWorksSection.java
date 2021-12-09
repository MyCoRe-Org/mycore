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

package org.mycore.orcid.works;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid.MCRORCIDException;
import org.mycore.orcid.MCRORCIDProfile;
import org.xml.sax.SAXException;

/**
 * Represents the "works" section of an ORCID profile with grouped works
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRWorksSection {

    private MCRORCIDProfile orcid;

    /** The groups of works this ORCID profile contains */
    private List<MCRGroupOfWorks> groups = new ArrayList<>();

    /** All works (not grouped) */
    private List<MCRWork> works = new ArrayList<>();

    /** Lookup table to get work by it's put code */
    private Map<String, MCRWork> putCode2Work = new HashMap<>();

    /**
     * Creates a representation of the ORCID's works section and
     * fetches the grouping of works and the work summaries
     */
    public MCRWorksSection(MCRORCIDProfile orcid) throws JDOMException, IOException, SAXException {
        this.orcid = orcid;
        refetchGroupsAndSummaries();
    }

    public MCRORCIDProfile getORCID() {
        return orcid;
    }

    public List<MCRWork> getWorks() {
        return new ArrayList<>(works);
    }

    void addWork(MCRWork work) {
        works.add(work);
        putCode2Work.put(work.getPutCode(), work);
    }

    void removeWork(MCRWork work) {
        works.remove(work);
        putCode2Work.remove(work.getPutCode());
    }

    /** Returns the work with the given put code, if any */
    public MCRWork getWork(String putCode) {
        return putCode2Work.get(putCode);
    }

    /**
     * Returns the list of grouped works after fetching work summaries.
     * Multiple works from different sources which are assumed to represent the same publication
     * are grouped together by ORCID.
     */
    public List<MCRGroupOfWorks> getGroups() {
        return groups;
    }

    /**
     * Returns a mods:modsCollection containing all MODS representations of the works.
     * The MODS from multiple works within the same groups is merged together,
     * so for each group of works there will be a single mods within the collection.
     */
    public Element buildMODSCollection() {
        Element modsCollection = new Element("modsCollection", MCRConstants.MODS_NAMESPACE);
        groups.forEach(g -> modsCollection.addContent(g.buildMergedMODS()));
        return modsCollection;
    }

    public Element buildUnmergedMODSCollection() {
        Element modsCollection = new Element("modsCollection", MCRConstants.MODS_NAMESPACE);
        groups.forEach(g -> modsCollection.addContent(g.buildUnmergedMODS()));
        return modsCollection;
    }

    /**
     * Fetches the grouping of works and all work summaries from the ORCID profile.
     * Can be called to refresh information on grouping to find out how grouping of works
     * may have changed after adding or deleting works.
     */
    public void refetchGroupsAndSummaries() throws JDOMException, IOException, SAXException {
        groups = orcid.getFetcher().fetchGroups(this);

        // Now, rebuild putCode2Work and works list from groups list:
        putCode2Work.clear();
        works.clear();

        groups.stream().flatMap(g -> g.getWorks().stream()).forEach(work -> {
            putCode2Work.put(work.getPutCode(), work);
            works.add(work);
        });
    }

    /** Fetches the work details for all work summaries from the ORCID profile. */
    public void fetchDetails() throws IOException, JDOMException, SAXException {
        orcid.getFetcher().fetchDetails(this);
    }

    /**
     * Adds a new "work" to the remote ORCID profile.
     * The publication data is taken from the MODS stored in the MyCoRe object with the given ID.
     */
    public MCRWork addWorkFrom(MCRObjectID objectID) throws IOException, JDOMException, SAXException {
        if (!MCRMetadataManager.exists(objectID)) {
            throw new MCRORCIDException("can not create work, object " + objectID + " does not exist locally");
        }

        MCRWork work = orcid.getPublisher().createWorkFrom(objectID);
        this.addWork(work);
        return work;
    }

    /**
     * Returns the work originating from the given local object, if any.
     * This is done by comparing the ID and all mods:identifier elements given in the MyCoRe MODS object
     * with the identifiers given in the ORCID work.
     */
    public Optional<MCRWork> findWork(MCRObjectID oid) {
        MCRObject obj = MCRMetadataManager.retrieveMCRObject(oid);
        return findWork(obj);
    }

    public Optional<MCRWork> findWork(MCRObject obj) {
        return findWorks(obj).findFirst();
    }

    public Optional<MCRWork> findOwnWork(MCRObjectID oid) {
        MCRObject obj = MCRMetadataManager.retrieveMCRObject(oid);
        return findOwnWork(obj);
    }

    public Optional<MCRWork> findOwnWork(MCRObject obj) {
        return findWorks(obj).filter(work -> work.getSource().isThisApplication()).findFirst();
    }

    public Stream<MCRWork> findWorks(MCRObject obj) {
        MCRMODSWrapper wrapper = new MCRMODSWrapper(obj);
        List<Element> objectIdentifiers = wrapper.getElements("mods:identifier");
        Set<String> objectKeys = buildIdentifierKeys(objectIdentifiers);
        return works.stream().filter(work -> matches(work, obj.getId(), objectKeys));
    }

    private boolean matches(MCRWork work, MCRObjectID oid, Set<String> objectIdentifiers) {
        Set<String> workIdentifiers = buildIdentifierKeys(work.getIdentifiers());
        workIdentifiers.retainAll(objectIdentifiers);
        return !workIdentifiers.isEmpty();
    }

    private Set<String> buildIdentifierKeys(List<Element> modsIdentifiers) {
        Set<String> objectKeys = new HashSet<>();
        for (Element modsIdentifier : modsIdentifiers) {
            objectKeys.add(buildIdentifierKey(modsIdentifier));
        }
        return objectKeys;
    }

    private String buildIdentifierKey(Element modsIdentifier) {
        return modsIdentifier.getAttributeValue("type") + ":" + modsIdentifier.getTextTrim();
    }

    /** Returns true, if there is a work in the ORCID profile that's origin is the given MyCoRe object */
    public boolean containsWork(MCRObjectID oid) {
        return findWork(oid).isPresent();
    }

    public boolean containsOwnWork(MCRObjectID oid) {
        return findOwnWork(oid).isPresent();
    }

    /** Returns all works in the ORCID profile that have been added by ths MyCoRe application */
    public Stream<MCRWork> getWorksFromThisApplication() {
        return works.stream().filter(work -> work.getSource().isThisApplication());
    }
}
