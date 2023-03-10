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

package org.mycore.orcid2.v3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.mods.merger.MCRMergeTool;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.exception.MCRORCIDTransformationException;
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.orcid2.v3.transformer.MCRORCIDWorkTransformerHelper;
import org.orcid.jaxb.model.v3.release.common.Contributor;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.xml.sax.SAXException;

/**
 * Provides utility methods for ORCID Work.
 */
public class MCRORCIDWorkUtils {

    /**
     * Transforms and merges List of works to Element.
     * 
     * @param works List of works
     * @return merged Element
     * @throws MCRORCIDException if build fails
     * @see #buildUnmergedMODSFromWorks
     */
    public static Element buildMergedMODSFromWorks(List<Work> works) throws MCRORCIDTransformationException {
        return mergeElements(buildUnmergedMODSFromWorks(works));
    }

    /**
     * Transforms List of works to List of elements.
     * 
     * @param works List of works
     * @return List of elements
     * @throws MCRORCIDException if build fails
     */
    public static List<Element> buildUnmergedMODSFromWorks(List<Work> works) throws MCRORCIDTransformationException {
        final List<Element> modsElements = new ArrayList<>();
        works.forEach(w -> {
            try {
                modsElements.add(MCRORCIDWorkTransformerHelper.transformWork(w).asXML().detachRootElement());
            } catch (IOException | JDOMException | SAXException | MCRORCIDTransformationException e) {
                throw new MCRORCIDException("Build failed", e);
            }
        });
        return modsElements;
    }

    /**
     * Compares works and possibly ignores the credit name of the contributors.
     * 
     * @param localWork the local Work
     * @param remoteWork the remote work
     * @return true if localWork equals remoteWork
     */
    public static boolean checkWorkEquality(Work localWork, Work remoteWork) {
        if (!Objects.equals(localWork.getWorkTitle(), remoteWork.getWorkTitle()) ||
            !Objects.equals(localWork.getShortDescription(), remoteWork.getShortDescription()) ||
            !Objects.equals(localWork.getWorkCitation(), remoteWork.getWorkCitation()) ||
            !Objects.equals(localWork.getWorkType(), remoteWork.getWorkType()) ||
            !Objects.equals(localWork.getPublicationDate(), remoteWork.getPublicationDate()) ||
            !Objects.equals(localWork.getUrl(), remoteWork.getUrl()) ||
            !Objects.equals(localWork.getJournalTitle(), remoteWork.getJournalTitle()) ||
            !Objects.equals(localWork.getLanguageCode(), remoteWork.getLanguageCode()) ||
            !Objects.equals(localWork.getCountry(), remoteWork.getCountry())) {
            return false;
        }
        if (!Objects.equals(localWork.getExternalIdentifiers(), remoteWork.getExternalIdentifiers())) {
            if (localWork.getExternalIdentifiers() == null || remoteWork.getExternalIdentifiers() == null) {
                return false;
            } else {
                Set<MCRIdentifier> localIds = localWork.getExternalIdentifiers().getExternalIdentifier().stream()
                    .map(i -> new MCRIdentifier(i.getType(), i.getValue())).collect(Collectors.toSet());
                Set<MCRIdentifier> remoteIds = remoteWork.getExternalIdentifiers().getExternalIdentifier().stream()
                    .map(i -> new MCRIdentifier(i.getType(), i.getValue())).collect(Collectors.toSet());
                if(!localIds.containsAll(remoteIds) || !remoteIds.containsAll(localIds)
                        || localIds.size() != remoteIds.size()) {
                    return false;
                }
            }
        }
        if (!Objects.equals(localWork.getWorkContributors(), remoteWork.getWorkContributors())) {
            if (localWork.getWorkContributors() == null || remoteWork.getWorkContributors() == null) {
                return false;
            } else {
                final List<Contributor> localContributor = localWork.getWorkContributors().getContributor();
                final List<Contributor> remoteContributor = remoteWork.getWorkContributors().getContributor();
                if (localContributor.size() != remoteContributor.size()) {
                    return false;
                }
                final Iterator<Contributor> itl = localContributor.iterator();
                final Iterator<Contributor> itr = remoteContributor.iterator();
                while (itl.hasNext()) {
                    final Contributor cl = itl.next();
                    final Contributor cr = itr.next();
                    if (!Objects.equals(cl.getContributorOrcid(), cr.getContributorOrcid()) ||
                        cl.getContributorOrcid() == null && !Objects.equals(cl.getCreditName(), cr.getCreditName()) ||
                        !Objects.equals(cl.getContributorEmail(), cr.getContributorEmail()) /*||
                            The comparision of ContributorAttributes is currently broken in the ORCID model and the
                            following test will currently always be true.
                        !Objects.equals(cl.getContributorAttributes(), cr.getContributorAttributes())*/) {
                        return false;
                    }
                    /* Workaround for above mentioned issue in ORCID model */
                    if (!Objects.equals(cl.getContributorAttributes(), cr.getContributorAttributes())) {
                        if (cl.getContributorAttributes() == null || cr.getContributorAttributes() == null) {
                            return false;
                        } else {
                            if (!Objects.equals(cl.getContributorAttributes().getContributorSequence(),
                                    cr.getContributorAttributes().getContributorSequence()) ||
                                !Objects.equals(cl.getContributorAttributes().getContributorRole(),
                                    cr.getContributorAttributes().getContributorRole())) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Merges List of MODS elements to one Element using MCRMergeTool.
     * 
     * @param elements List of Element
     * @return merged Element
     */
    private static Element mergeElements(List<Element> elements) {
        final Element result = elements.get(0);
        elements.stream().skip(1).forEach(e -> MCRMergeTool.merge(result, e));
        return result;
    }
}
