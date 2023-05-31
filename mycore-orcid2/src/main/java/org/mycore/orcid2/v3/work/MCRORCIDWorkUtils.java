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

package org.mycore.orcid2.v3.work;

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
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.exception.MCRORCIDTransformationException;
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.orcid2.v3.transformer.MCRORCIDWorkTransformerHelper;
import org.orcid.jaxb.model.v3.release.common.Contributor;
import org.orcid.jaxb.model.v3.release.common.ContributorAttributes;
import org.orcid.jaxb.model.v3.release.record.ExternalIDs;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.WorkContributors;
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
    public static Element buildMergedMODSFromWorks(List<Work> works) {
        return mergeElements(buildUnmergedMODSFromWorks(works));
    }

    /**
     * Transforms List of works to List of elements.
     * 
     * @param works List of works
     * @return List of elements
     * @throws MCRORCIDException if build fails
     */
    public static List<Element> buildUnmergedMODSFromWorks(List<Work> works) {
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
     * Lists identifiers as Set of MCRIdentifier.
     * 
     * @param work the Work
     * @return Set of MCRIdentifier
     */
    public static Set<MCRIdentifier> listIdentifiers(Work work) {
        return work.getExternalIdentifiers().getExternalIdentifier().stream()
            .filter(i -> MCRORCIDUtils.checkTrustedIdentifier(i.getType()))
            .map(i -> new MCRIdentifier(i.getType(), i.getValue())).collect(Collectors.toSet());
    }

    /**
     * Compares works and possibly ignores the credit name of the contributors.
     * 
     * @param localWork the local Work
     * @param remoteWork the remote work
     * @return true if localWork equals remoteWork
     */
    public static boolean checkWorkEquality(Work localWork, Work remoteWork) {
        return Objects.equals(localWork.getWorkTitle(), remoteWork.getWorkTitle()) &&
            Objects.equals(localWork.getShortDescription(), remoteWork.getShortDescription()) &&
            Objects.equals(localWork.getWorkCitation(), remoteWork.getWorkCitation()) &&
            Objects.equals(localWork.getWorkType(), remoteWork.getWorkType()) &&
            Objects.equals(localWork.getPublicationDate(), remoteWork.getPublicationDate()) &&
            Objects.equals(localWork.getUrl(), remoteWork.getUrl()) &&
            Objects.equals(localWork.getJournalTitle(), remoteWork.getJournalTitle()) &&
            Objects.equals(localWork.getLanguageCode(), remoteWork.getLanguageCode()) &&
            Objects.equals(localWork.getCountry(), remoteWork.getCountry()) &&
            equalExternalIdentifiers(localWork.getExternalIdentifiers(), remoteWork.getExternalIdentifiers()) &&
            equalWorkContributors(localWork.getWorkContributors(), remoteWork.getWorkContributors());
    }

    /**
     * Compares two ExternalIDs.
     *
     * @param a First ExternalIDs
     * @param b Second ExternalIDs
     * @return true if both are regarded equal
     */
    private static boolean equalExternalIdentifiers(ExternalIDs a, ExternalIDs b) {
        if (!Objects.equals(a, b)) {
            if (a == null || b == null) {
                return false;
            } else {
                Set<MCRIdentifier> aIds = a.getExternalIdentifier().stream()
                    .map(i -> new MCRIdentifier(i.getType(), i.getValue())).collect(Collectors.toSet());
                Set<MCRIdentifier> bIds = b.getExternalIdentifier().stream()
                    .map(i -> new MCRIdentifier(i.getType(), i.getValue())).collect(Collectors.toSet());
                if(!Objects.equals(aIds, bIds)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Compares two WorkContributors.
     *
     * @param a First WorkContributors
     * @param b Second WorkContributors
     * @return true if both are regarded equal
     */
    private static boolean equalWorkContributors(WorkContributors a, WorkContributors b) {
        if (!Objects.equals(a, b)) {
            if (a == null || b == null) {
                return false;
            } else {
                final List<Contributor> aContributor = a.getContributor();
                final List<Contributor> bContributor = b.getContributor();
                if (aContributor.size() != bContributor.size()) {
                    return false;
                }
                final Iterator<Contributor> itl = aContributor.iterator();
                final Iterator<Contributor> itr = bContributor.iterator();
                while (itl.hasNext()) {
                    final Contributor ca = itl.next();
                    final Contributor cb = itr.next();
                    /* The comparision of ContributorAttributes is currently broken in the ORCID model
                     * (https://github.com/ORCID/orcid-model/issues/46) and the Object.equals() test will currently
                     * always be true. Once the issue has been fixed equalContributorAttributes() can be replaced
                     * by Object.equals() */
                    if (!Objects.equals(ca.getContributorOrcid(), cb.getContributorOrcid()) ||
                        (ca.getContributorOrcid() == null && cb.getContributorOrcid() == null &&
                        !Objects.equals(ca.getCreditName(), cb.getCreditName())) ||
                        !Objects.equals(ca.getContributorEmail(), cb.getContributorEmail()) ||
                        !equalContributorAttributes(ca.getContributorAttributes(), cb.getContributorAttributes())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /* Temporary Workaround for issue in ORCID model (https://github.com/ORCID/orcid-model/issues/46) */
    private static boolean equalContributorAttributes(ContributorAttributes a, ContributorAttributes b) {
        if (!Objects.equals(a, b)) {
            if (a == null || b == null) {
                return false;
            } else {
                if (!Objects.equals(a.getContributorSequence(), b.getContributorSequence()) ||
                    !Objects.equals(a.getContributorRole(), b.getContributorRole())) {
                    return false;
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
